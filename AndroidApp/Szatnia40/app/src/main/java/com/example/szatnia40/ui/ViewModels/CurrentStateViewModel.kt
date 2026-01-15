package com.example.szatnia40.ui.ViewModels

import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.szatnia40.data.remote.ControlRequest
import com.example.szatnia40.data.remote.PostgrestApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// Stan ekranu - to, co widzi użytkownik
data class DashboardState(
    val roomTemp: Float = 0f,
    val hallTemp: Float = 0f,
    val furnaceTemp: Float = 0f,
    val isFurnaceOn: Boolean = false,
    val isFanOn: Boolean = false,
    val heater1: Int = 0,
    val heater2: Int = 0,
    val heater3: Int = 0
)

@HiltViewModel
class CurrentStateViewModel @Inject constructor(
    private val api: PostgrestApi,
    private val prefs: SharedPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow<DashboardState?>(null)
    val uiState: StateFlow<DashboardState?> = _uiState.asStateFlow()

    init {
        fetchData()
    }

    fun fetchData() {
        viewModelScope.launch {
            try {
                val token = prefs.getString("auth_token", "") ?: ""
                val header = "Bearer $token"

                // 1. Pobieramy listę z bazy (Twoje wiersze)
                val componentsList = api.getSystemStatus(header)

                // 2. Mapujemy Twoje nazwy z bazy na zmienne w aplikacji
                val newState = DashboardState(
                    // Szukamy wiersza gdzie nazwa_komponentu to "temperatura_pomieszczenia"
                    roomTemp = componentsList.find { it.name == "temperatura_pomieszczenia" }?.currentValue ?: 0f,

                    hallTemp = componentsList.find { it.name == "temperatura_hali" }?.currentValue ?: 0f,

                    furnaceTemp = componentsList.find { it.name == "temperatura_pieca" }?.currentValue ?: 0f,

                    // Dla pieca i wiatraka: w bazie masz 1.0 lub 0.0. Sprawdzamy czy > 0.
                    isFurnaceOn = (componentsList.find { it.name == "piec" }?.currentValue ?: 0f) > 0,

                    isFanOn = (componentsList.find { it.name == "wiatrak" }?.currentValue ?: 0f) > 0,

                    // Grzejniki rzutujemy na Int (np. 2.5 -> 2, chyba że chcesz ułamki to zostaw Float)
                    heater1 = componentsList.find { it.name == "grzejnik_1" }?.currentValue?.toInt() ?: 0,
                    heater2 = componentsList.find { it.name == "grzejnik_2" }?.currentValue?.toInt() ?: 0,
                    heater3 = componentsList.find { it.name == "grzejnik_3" }?.currentValue?.toInt() ?: 0
                )

                _uiState.value = newState

            } catch (e: Exception) {
                Log.e("CurrentStateVM", "Błąd pobierania danych: ${e.message}")
            }
        }
    }

    // Metoda do sterowania (np. włączanie pieca)
    fun setControl(componentName: String, value: Float) {
        viewModelScope.launch {
            try {
                val token = prefs.getString("auth_token", "") ?: ""
                // Tutaj componentName to np. "piec" albo "grzejnik_1"
                api.setControl("Bearer $token", ControlRequest(componentName, value))
                fetchData() // Odśwież widok, żeby zobaczyć zmianę
            } catch (e: Exception) {
                Log.e("CurrentStateVM", "Błąd sterowania", e)
            }
        }
    }
}