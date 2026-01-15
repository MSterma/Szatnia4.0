package com.example.szatnia40.ui.ViewModels

import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.szatnia40.data.remote.Employee
import com.example.szatnia40.data.remote.EmployeeUpdate
import com.example.szatnia40.data.remote.PostgrestApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EmployeesState(
    val list: List<Employee> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class EmployeesViewModel @Inject constructor(
    private val api: PostgrestApi,
    private val prefs: SharedPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(EmployeesState())
    val uiState: StateFlow<EmployeesState> = _uiState.asStateFlow()

    init {
        fetchEmployees()
    }

    fun fetchEmployees() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val token = prefs.getString("auth_token", "") ?: ""
                val header = "Bearer $token"

                val employees = api.getEmployees(header)
                // Sortujemy po ID, żeby lista nie skakała
                val sortedList = employees.sortedBy { it.id }

                _uiState.value = _uiState.value.copy(list = sortedList, isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun updateEmployee(id: Int, name: String, surname: String, card: String, isActive: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val token = prefs.getString("auth_token", "") ?: ""
                val header = "Bearer $token"

                // Wysyłamy PATCH dla konkretnego ID
                val response = api.updateEmployee(
                    token = header,
                    id = "eq.$id",
                    update = EmployeeUpdate(name, surname, card, isActive)
                )

                if (response.isSuccessful) {
                    Log.d("EmployeesVM", "Zaktualizowano pracownika ID: $id")
                    fetchEmployees() // Odśwież listę po sukcesie
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("EmployeesVM", "Błąd API: ${response.code()} - $errorBody")
                    _uiState.value = _uiState.value.copy(isLoading = false, error = "Błąd zapisu: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("EmployeesVM", "Wyjątek: ${e.message}")
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }
}