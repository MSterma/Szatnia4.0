package com.example.szatnia40.ui

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.szatnia40.data.remote.NotificationItem
import com.example.szatnia40.data.remote.PostgrestApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotificationsState(
    val items: List<NotificationItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val api: PostgrestApi,
    private val prefs: SharedPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationsState())
    val uiState: StateFlow<NotificationsState> = _uiState.asStateFlow()

    init {
        fetchNotifications()
    }

    fun fetchNotifications() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val token = "Bearer ${prefs.getString("auth_token", "")}"
                val data = api.getNotifications(token)
                _uiState.value = _uiState.value.copy(items = data, isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Błąd: ${e.message}")
            }
        }
    }

    // --- NOWA FUNKCJA USUWANIA ---
    fun deleteNotification(item: NotificationItem) {
        viewModelScope.launch {
            try {
                val token = "Bearer ${prefs.getString("auth_token", "")}"

                // Wywołanie API
                val response = api.deleteNotification(token, "eq.${item.id}")

                if (response.isSuccessful) {
                    // Usuwamy lokalnie z listy, żeby widok odświeżył się natychmiast
                    val currentList = _uiState.value.items.toMutableList()
                    currentList.remove(item)
                    _uiState.value = _uiState.value.copy(items = currentList)
                } else {
                    _uiState.value = _uiState.value.copy(error = "Nie udało się usunąć wpisu.")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Błąd sieci: ${e.message}")
            }
        }
    }
}