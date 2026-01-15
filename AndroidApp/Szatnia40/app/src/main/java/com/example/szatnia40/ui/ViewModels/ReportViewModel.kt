package com.example.szatnia40.ui.ViewModels

import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.szatnia40.data.remote.MeasurementPoint
import com.example.szatnia40.data.remote.MeasurementRequest
import com.example.szatnia40.data.remote.PostgrestApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReportState(
    val data: List<MeasurementPoint> = emptyList(),
    val isLoading: Boolean = false,
    val selectedComponent: String = "temperatura_pieca", // Domyślny
    val selectedRange: String = "1h", // Domyślny (1h, 24h, 72h)
    val error: String? = null
)

@HiltViewModel
class ReportViewModel @Inject constructor(
    private val api: PostgrestApi,
    private val prefs: SharedPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportState())
    val uiState: StateFlow<ReportState> = _uiState.asStateFlow()

    // Mapa nazw wyświetlanych na nazwy techniczne w bazie
    val componentMap = mapOf(
        "Temperatura Pieca" to "temperatura_pieca",
        "Temperatura Hali" to "temperatura_hali",
        "Temperatura Pomieszczenia" to "temperatura_pomieszczenia",
        "Grzejnik 1" to "grzejnik_1",
        "Grzejnik 2" to "grzejnik_2",
        "Grzejnik 3" to "grzejnik_3"
    )

    init {
        fetchData()
    }

    fun onComponentChange(displayName: String) {
        val technicalName = componentMap[displayName] ?: return
        _uiState.value = _uiState.value.copy(selectedComponent = technicalName)
        fetchData()
    }

    fun onRangeChange(range: String) {
        _uiState.value = _uiState.value.copy(selectedRange = range)
        fetchData()
    }

    fun fetchData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val token = prefs.getString("auth_token", "") ?: ""
                val header = "Bearer $token"
                val state = _uiState.value

                // Wywołanie RPC: api.pobierz_pomiary
                val result = api.getMeasurements(
                    token = header,
                    body = MeasurementRequest(
                        komponent = state.selectedComponent,
                        zakres = state.selectedRange
                    )
                )

                // Sortujemy od najstarszych do najnowszych (żeby wykres szedł w prawo)
                // Zakładamy, że data to string ISO, więc sortowanie stringów zadziała
                val sortedData = result.sortedBy { it.data }

                _uiState.value = _uiState.value.copy(data = sortedData, isLoading = false)

            } catch (e: Exception) {
                Log.e("ReportVM", "Błąd: ${e.message}")
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }
}