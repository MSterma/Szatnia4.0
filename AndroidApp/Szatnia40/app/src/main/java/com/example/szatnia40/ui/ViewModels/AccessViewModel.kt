package com.example.szatnia40.ui.ViewModels

import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.szatnia40.data.remote.AccessLog
import com.example.szatnia40.data.remote.PostgrestApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AccessState(
    val logs: List<AccessLog> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AccessViewModel @Inject constructor(
    private val api: PostgrestApi,
    private val prefs: SharedPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccessState())
    val uiState: StateFlow<AccessState> = _uiState.asStateFlow()

    init {
        fetchLogs()
    }

    fun fetchLogs() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val token = prefs.getString("auth_token", "") ?: ""
                val header = "Bearer $token"

                // Pobieramy logi (zakładamy, że API ma parametr sortowania,
                // ale dla pewności sortujemy też lokalnie po dacie string)
                val logs = api.getAccessHistory(header)

                // Sortowanie malejąco (od najnowszych)
                val sortedLogs = logs.sortedByDescending { it.data }

                _uiState.value = _uiState.value.copy(logs = sortedLogs, isLoading = false)
            } catch (e: Exception) {
                Log.e("AccessVM", "Błąd: ${e.message}")
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }
}