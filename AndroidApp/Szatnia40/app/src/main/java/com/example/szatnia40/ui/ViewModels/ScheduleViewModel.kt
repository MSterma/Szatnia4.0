package com.example.szatnia40.ui.ViewModels

import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.szatnia40.data.remote.PostgrestApi
import com.example.szatnia40.data.remote.ScheduleItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.HttpException
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class ScheduleState(
    val items: List<ScheduleItem> = emptyList(),
    val filteredItems: List<ScheduleItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedDayFilter: Int? = null,
    val sortColumn: String = "day",
    val isSortAscending: Boolean = true
)

@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val api: PostgrestApi,
    private val prefs: SharedPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScheduleState())
    val uiState: StateFlow<ScheduleState> = _uiState.asStateFlow()

    val daysMap = mapOf(
        1 to "Poniedziałek", 2 to "Wtorek", 3 to "Środa",
        4 to "Czwartek", 5 to "Piątek", 6 to "Sobota", 7 to "Niedziela"
    )

    init {
        fetchSchedule()
    }

    // --- WALIDACJA LOKALNA ---
    // Zwraca null jeśli OK, albo treść błędu
    private fun validateInput(item: ScheduleItem): String? {
        // 1. Sprawdź format godziny (HH:mm) używając Regex
        // Akceptuje: 00:00 do 23:59. Odrzuca: 24:00, 123:00, 8:00 (musi być 08:00)
        val timeRegex = Regex("^([0-1][0-9]|2[0-3]):[0-5][0-9]$")

        val startShort = item.startTime.take(5)
        val endShort = item.endTime.take(5)

        if (!timeRegex.matches(startShort)) return "Błędny format godziny startu (wymagane GG:MM, np. 08:00)"
        if (!timeRegex.matches(endShort)) return "Błędny format godziny końca (wymagane GG:MM, np. 16:00)"

        // 2. Sprawdź czy Start < Koniec
        // Używamy prostego porównania stringów, bo format HH:mm to umożliwia
        if (startShort >= endShort) return "Godzina rozpoczęcia musi być wcześniejsza niż zakończenia"

        // 3. Walidacja temperatury
        if (item.targetTemp < 5.0 || item.targetTemp > 35.0) return "Temperatura musi być z zakresu 5 - 35 stopni"

        return null
    }

    fun fetchSchedule() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val token = "Bearer ${prefs.getString("auth_token", "")}"
                val data = api.getSchedule(token)
                _uiState.value = _uiState.value.copy(items = data, isLoading = false)
                applyFiltersAndSort()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = handleApiError(e))
            }
        }
    }

    // ... (reszta klasy i importy bez zmian) ...

    // Zastąp starą funkcję saveItem tą wersją:
    fun saveItem(item: ScheduleItem, onSuccess: () -> Unit) {
        // 1. Walidacja lokalna (Format czasu, Logika)
        val validationError = validateInput(item)
        if (validationError != null) {
            _uiState.value = _uiState.value.copy(error = validationError)
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val token = "Bearer ${prefs.getString("auth_token", "")}"
                val response = if (item.id == null) {
                    api.addScheduleItem(token, item)
                } else {
                    api.updateScheduleItem(token, "eq.${item.id}", item)
                }

                if (response.isSuccessful) {
                    // SUKCES (kod 200-299)
                    fetchSchedule()
                    onSuccess()
                } else {
                    // BŁĄD SERWERA (kod 400 - np. konflikt dat, kod 500)
                    val errorMsg = parseErrorBody(response.errorBody()?.string())
                    _uiState.value = _uiState.value.copy(isLoading = false, error = errorMsg)
                }
            } catch (e: Exception) {
                // BŁĄD SIECI (np. brak internetu)
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Błąd połączenia: ${e.message}")
            }
        }
    }

    // Zastąp starą funkcję deleteItem tą wersją:
    fun deleteItem(item: ScheduleItem) {
        viewModelScope.launch {
            try {
                val token = "Bearer ${prefs.getString("auth_token", "")}"
                item.id?.let { id ->
                    val response = api.deleteScheduleItem(token, "eq.$id")

                    if (response.isSuccessful) {
                        fetchSchedule()
                    } else {
                        val errorMsg = parseErrorBody(response.errorBody()?.string())
                        _uiState.value = _uiState.value.copy(error = errorMsg)
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Nie udało się usunąć: ${e.message}")
            }
        }
    }

    // Nowa funkcja pomocnicza do parsowania JSONa z PostgREST
    private fun parseErrorBody(errorJson: String?): String {
        if (errorJson.isNullOrEmpty()) return "Wystąpił nieznany błąd serwera."

        return try {
            // PostgREST zwraca: { "message": "Twój komunikat triggera...", "code": "P0001", ... }
            val jsonObject = JSONObject(errorJson)

            // Pobieramy pole "message" - tam siedzi komunikat z Twojego Triggera SQL
            jsonObject.optString("message", "Błąd serwera (brak szczegółów)")

        } catch (e: Exception) {
            "Błąd serwera: $errorJson"
        }
    }
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    private fun handleApiError(e: Exception): String {
        // Sprawdzamy, czy to błąd sieciowy z kodem HTTP (np. 400, 404, 500)
        if (e is retrofit2.HttpException) {
            return try {
                // TU JEST TWÓJ JSON Z BŁĘDEM:
                val errorBody = e.response()?.errorBody()?.string()

                if (errorBody != null) {
                    // PostgREST zwraca: {"message": "Tresc bledu...", "code": "...", ...}
                    // Musimy to sparsować ręcznie
                    val json = org.json.JSONObject(errorBody)
                    json.optString("message", "Wystąpił nieznany błąd serwera")
                } else {
                    "Błąd serwera: ${e.code()}"
                }
            } catch (parsingEx: Exception) {
                "Błąd odczytu błędu: ${e.message}"
            }
        }
        // Inne błędy (np. brak internetu)
        return e.message ?: "Nieznany błąd"
    }
    // ... Reszta kodu (onFilterChange, onSortChange, applyFiltersAndSort) bez zmian ...
    // ... Skopiuj je z poprzedniej wersji lub zostaw tak jak masz ...

    fun onFilterChange(day: Int?) {
        _uiState.value = _uiState.value.copy(selectedDayFilter = day)
        applyFiltersAndSort()
    }

    fun onSortChange(column: String) {
        val current = _uiState.value
        val newAsc = if (current.sortColumn == column) !current.isSortAscending else true
        _uiState.value = current.copy(sortColumn = column, isSortAscending = newAsc)
        applyFiltersAndSort()
    }

    private fun applyFiltersAndSort() {
        val state = _uiState.value
        var list = state.items
        if (state.selectedDayFilter != null) {
            list = list.filter { it.dayOfWeek == state.selectedDayFilter }
        }
        list = when (state.sortColumn) {
            "day" -> list.sortedBy { it.dayOfWeek }
            "start" -> list.sortedBy { it.startTime }
            "temp" -> list.sortedBy { it.targetTemp }
            else -> list
        }
        if (!state.isSortAscending) list = list.reversed()
        _uiState.value = state.copy(filteredItems = list)
    }
}