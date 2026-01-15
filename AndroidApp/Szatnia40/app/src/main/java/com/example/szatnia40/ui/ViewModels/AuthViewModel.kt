package com.example.szatnia40.ui.ViewModels

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.szatnia40.data.remote.AdminLoginRequest
import com.example.szatnia40.data.remote.PostgrestApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val api: PostgrestApi,
    private val prefs: SharedPreferences
) : ViewModel() {

    // Stan tokena (czy zalogowany)
    private val _tokenFlow = MutableStateFlow<String?>(prefs.getString("auth_token", null))
    val tokenFlow: StateFlow<String?> = _tokenFlow.asStateFlow()

    // Stan formularza
    private val _login = MutableStateFlow("")
    val login: StateFlow<String> = _login.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Funkcje do aktualizacji pól z widoku
    fun onLoginChange(newLogin: String) { _login.value = newLogin }
    fun onPasswordChange(newPass: String) { _password.value = newPass }

    // --- GŁÓWNA FUNKCJA LOGOWANIA ---
    fun performLogin() {
        // 1. Walidacja lokalna: Czy login to "admin"?
        if (_login.value.trim() != "admin") {
            _error.value = "Błędny login użytkownika"
            return
        }

        // 2. Jeśli login OK, wysyłamy zapytanie do API
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                // Wysyłamy TYLKO hasło, bo Twoja funkcja SQL tak wymaga
                val response = api.loginAdmin(AdminLoginRequest(pass = _password.value))

                // Zapisujemy token
                prefs.edit().putString("auth_token", response.token).apply()
                _tokenFlow.value = response.token

            } catch (e: Exception) {
                _error.value = "Błąd logowania: Sprawdź hasło"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun logout() {
        prefs.edit().remove("auth_token").apply()
        _tokenFlow.value = null
        // Czyścimy formularz przy wylogowaniu
        _login.value = ""
        _password.value = ""
        _error.value = null
    }
}