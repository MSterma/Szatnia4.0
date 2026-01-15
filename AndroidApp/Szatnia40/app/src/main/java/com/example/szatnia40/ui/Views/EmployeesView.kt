package com.example.szatnia40.ui.Views

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.szatnia40.data.remote.Employee
import com.example.szatnia40.ui.ViewModels.EmployeesViewModel

@Composable
fun EmployeesView(
    modifier: Modifier = Modifier,
    viewModel: EmployeesViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    // Stan do obsługi dialogu edycji
    var showDialog by remember { mutableStateOf(false) }
    var selectedEmployee by remember { mutableStateOf<Employee?>(null) }

    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = "Pracownicy",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(20.dp)
        )

        if (state.isLoading && state.list.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(state.list) { employee ->
                    EmployeeRow(
                        employee = employee,
                        onClick = {
                            selectedEmployee = employee
                            showDialog = true
                        }
                    )
                }
            }
        }
    }

    // Wyświetl dialog, jeśli wybrano pracownika
    if (showDialog && selectedEmployee != null) {
        EditEmployeeDialog(
            employee = selectedEmployee!!,
            onDismiss = { showDialog = false },
            onSave = { id, name, surname, card, active ->
                viewModel.updateEmployee(id, name, surname, card, active)
                showDialog = false
            }
        )
    }
}

@Composable
fun EmployeeRow(employee: Employee, onClick: () -> Unit) {
    // Kolor karty: szary jeśli nieaktywny, biały jeśli aktywny
    val cardColor = if (employee.isActive) MaterialTheme.colorScheme.surface else Color.LightGray.copy(alpha = 0.3f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = if (employee.isActive) MaterialTheme.colorScheme.primary else Color.Gray
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${employee.imie} ${employee.nazwisko}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = if (employee.isActive) Color.Unspecified else Color.Gray
                )
                Text(
                    text = "Karta: ${employee.karta}",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                if (!employee.isActive) {
                    Text(text = "(Konto zablokowane)", fontSize = 12.sp, color = Color.Red)
                }
            }

            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Edytuj",
                tint = Color.Gray
            )
        }
    }
}

@Composable
fun EditEmployeeDialog(
    employee: Employee,
    onDismiss: () -> Unit,
    onSave: (Int, String, String, String, Boolean) -> Unit
) {
    var name by remember { mutableStateOf(employee.imie) }
    var surname by remember { mutableStateOf(employee.nazwisko) }
    var card by remember { mutableStateOf(employee.karta) }
    var isActive by remember { mutableStateOf(employee.isActive) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Edycja: ${employee.imie} ${employee.nazwisko}") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Imię") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = surname,
                    onValueChange = { surname = it },
                    label = { Text("Nazwisko") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = card,
                    onValueChange = { card = it },
                    label = { Text("Nr Karty") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Przełącznik aktywności
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Konto aktywne?", modifier = Modifier.weight(1f))
                    Switch(checked = isActive, onCheckedChange = { isActive = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(employee.id, name, surname, card, isActive) }
            ) {
                Text("Zapisz")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Anuluj")
            }
        }
    )
}