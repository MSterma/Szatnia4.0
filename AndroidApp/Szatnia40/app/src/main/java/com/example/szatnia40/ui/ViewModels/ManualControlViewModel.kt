package com.example.szatnia40.ui.ViewModels

import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.szatnia40.data.remote.ConfigUpdate
import com.example.szatnia40.data.remote.ControlRequest
import com.example.szatnia40.data.remote.PostgrestApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// Stan widoku (UI State)
data class ManualControlState(
    val isManualMode: Boolean = false,
    val furnace: Boolean = false, // Domyślnie wyłączony
    val fan: Boolean = true,      // Domyślnie włączony (zgodnie z Twoją tabelą)
    val heater1: Float = 0f,
    val heater2: Float = 0f,
    val heater3: Float = 0f,
    val isLoading: Boolean = false
)

@HiltViewModel
class ManualControlViewModel @Inject constructor(
    private val api: PostgrestApi,
    private val prefs: SharedPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(ManualControlState())
    val uiState: StateFlow<ManualControlState> = _uiState.asStateFlow()

    // --- Metody do aktualizacji suwaków w UI ---
    fun onManualModeChange(value: Boolean) { _uiState.value = _uiState.value.copy(isManualMode = value) }
    fun onFurnaceChange(value: Boolean) { _uiState.value = _uiState.value.copy(furnace = value) }
    fun onFanChange(value: Boolean) { _uiState.value = _uiState.value.copy(fan = value) }
    fun onHeater1Change(value: Float) { _uiState.value = _uiState.value.copy(heater1 = value) }
    fun onHeater2Change(value: Float) { _uiState.value = _uiState.value.copy(heater2 = value) }
    fun onHeater3Change(value: Float) { _uiState.value = _uiState.value.copy(heater3 = value) }

    // --- GŁÓWNA LOGIKA WYSYŁANIA ---
    fun saveSettings() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true) // Pokaż kręciołek

            try {
                val token = prefs.getString("auth_token", "") ?: ""
                val authHeader = "Bearer $token"
                val state = _uiState.value

                if (!state.isManualMode) {
                    // SCENARIUSZ 1: Wyłączamy tryb manualny
                    // Tylko aktualizujemy tabelę konfiguracji
                    api.updateConfig(authHeader, update = ConfigUpdate(isManualMode = false))
                    Log.d("ManualVM", "Tryb manualny wyłączony w bazie")
                } else {
                    // SCENARIUSZ 2: Włączamy tryb manualny i wysyłamy nastawy

                    // A. Włącz flagę w konfiguracji
                    api.updateConfig(authHeader, update = ConfigUpdate(isManualMode = true))

                    // B. Wyślij ustawienia dla każdego urządzenia
                    // Nazwy ("piec", "wiatrak"...) muszą być identyczne jak w Twojej tabeli SQL!

                    // Piec (Boolean -> 1.0 lub 0.0)
                    api.setControl(authHeader, ControlRequest("piec", if (state.furnace) 1f else 0f))

                    // Wiatrak
                    api.setControl(authHeader, ControlRequest("wiatrak", if (state.fan) 1f else 0f))

                    // Grzejniki
                    api.setControl(authHeader, ControlRequest("grzejnik_1", state.heater1))
                    api.setControl(authHeader, ControlRequest("grzejnik_2", state.heater2))
                    api.setControl(authHeader, ControlRequest("grzejnik_3", state.heater3))

                    Log.d("ManualVM", "Wysłano pełne sterowanie manualne")
                }

            } catch (e: Exception) {
                Log.e("ManualVM", "Błąd zapisu: ${e.message}")
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false) // Ukryj kręciołek
            }
        }
    }
}