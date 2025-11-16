package com.example.szatnia40.ui.auth
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val securePrefs: SharedPreferences
) : ViewModel() {

    private val _tokenFlow = MutableStateFlow<String?>(null)

    val tokenFlow: StateFlow<String?> = _tokenFlow.asStateFlow()

    init {
        loadToken()
    }

    private fun loadToken() {
        viewModelScope.launch(Dispatchers.IO) {
            val token = securePrefs.getString("auth_token", null)
            _tokenFlow.value = token // Ustaw wczytaną wartość
        }
    }

    fun saveUserToken(token: String) {
        viewModelScope.launch(Dispatchers.IO) {
            securePrefs.edit {
                putString("auth_token", token)
            }
            _tokenFlow.value = token
        }
    }

    fun logout() {
        viewModelScope.launch(Dispatchers.IO) {
            securePrefs.edit {
                remove("auth_token")
            }
            _tokenFlow.value = null
        }
    }
}