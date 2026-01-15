package com.example.szatnia40.ui.Views


import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.szatnia40.data.remote.ScheduleItem
import com.example.szatnia40.ui.ViewModels.ScheduleViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleView(
    modifier: Modifier = Modifier,
    viewModel: ScheduleViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<ScheduleItem?>(null) }

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            FloatingActionButton(onClick = {
                viewModel.clearError() // Czyścimy błędy przed otwarciem
                itemToEdit = null
                showDialog = true
            }) {
                Icon(Icons.Default.Add, contentDescription = "Dodaj")
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier
            .padding(innerPadding)
            .padding(16.dp)) {

            Text("Harmonogram", style = MaterialTheme.typography.headlineMedium)

            // Opcjonalnie: Błąd ogólny (np. podczas pobierania listy), jeśli dialog jest zamknięty
            if (state.error != null && !showDialog) {
                Text(state.error!!, color = Color.Red, modifier = Modifier.padding(vertical = 8.dp))
            }

            Spacer(modifier = Modifier.height(10.dp))

            // --- FILTR DZIEŃ TYGODNIA ---
            DayFilterDropdown(
                selectedDay = state.selectedDayFilter,
                onDaySelected = { viewModel.onFilterChange(it) },
                daysMap = viewModel.daysMap
            )

            Spacer(modifier = Modifier.height(10.dp))

            // --- NAGŁÓWEK TABELI (Sortowanie) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HeaderCell(text = "Dzień", weight = 0.3f, onClick = { viewModel.onSortChange("day") })
                HeaderCell(text = "Start", weight = 0.2f, onClick = { viewModel.onSortChange("start") })
                HeaderCell(text = "Koniec", weight = 0.2f, onClick = { /* opcjonalnie */ })
                HeaderCell(text = "Temp", weight = 0.2f, onClick = { viewModel.onSortChange("temp") })
                Spacer(modifier = Modifier.width(70.dp)) // Miejsce na wyrównanie do ikonek akcji
            }

            // --- LISTA REKORDÓW ---
            // Pasek ładowania widoczny tylko gdy nie ma dialogu (żeby nie dublować)
            if (state.isLoading && !showDialog) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            LazyColumn {
                items(state.filteredItems) { item ->
                    ScheduleRow(
                        item = item,
                        daysMap = viewModel.daysMap,
                        onEditClick = {
                            viewModel.clearError() // Czyścimy błędy przed otwarciem
                            itemToEdit = item
                            showDialog = true
                        },
                        onDeleteClick = { viewModel.deleteItem(item) }
                    )
                }
            }
        }
    }

    // --- OKNO DIALOGOWE (DODAJ / EDYTUJ) ---
    if (showDialog) {
        ScheduleEditDialog(
            item = itemToEdit,
            daysMap = viewModel.daysMap,
            error = state.error,        // Przekazujemy błąd do środka
            isLoading = state.isLoading, // Przekazujemy stan ładowania
            onDismiss = {
                showDialog = false
                viewModel.clearError()
            },
            onConfirm = { newItem ->
                viewModel.saveItem(newItem, onSuccess = {
                    showDialog = false
                })
            }
        )
    }
}

// --- KOMPONENTY POMOCNICZE ---

@Composable
fun RowScope.HeaderCell(text: String, weight: Float, onClick: () -> Unit) {
    Text(
        text = text,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .weight(weight)
            .clickable { onClick() }
            .padding(4.dp),
        fontSize = 14.sp
    )
}

@Composable
fun ScheduleRow(
    item: ScheduleItem,
    daysMap: Map<Int, String>,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Skrócona nazwa dnia (np. Poniedziałek -> Pon)
        val dayName = daysMap[item.dayOfWeek]?.take(3) ?: "?"

        Text(dayName, modifier = Modifier.weight(0.3f))

        // Obcinamy sekundy z czasu (08:00:00 -> 08:00)
        val startFormatted = if (item.startTime.length >= 5) item.startTime.take(5) else item.startTime
        val endFormatted = if (item.endTime.length >= 5) item.endTime.take(5) else item.endTime

        Text(startFormatted, modifier = Modifier.weight(0.2f))
        Text(endFormatted, modifier = Modifier.weight(0.2f))
        Text("${item.targetTemp}°C", modifier = Modifier.weight(0.2f), fontWeight = FontWeight.Bold)

        // Przyciski akcji
        Row(modifier = Modifier.width(70.dp)) {
            IconButton(onClick = onEditClick, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Default.Edit, "Edytuj", tint = Color.Gray)
            }
            IconButton(onClick = onDeleteClick, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Default.Delete, "Usuń", tint = Color.Red)
            }
        }
    }
}

@Composable
fun DayFilterDropdown(
    selectedDay: Int?,
    onDaySelected: (Int?) -> Unit,
    daysMap: Map<Int, String>
) {
    var expanded by remember { mutableStateOf(false) }
    val displayText = if (selectedDay != null) daysMap[selectedDay] ?: "Błąd" else "Wszystkie dni"

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = displayText,
            onValueChange = {},
            readOnly = true,
            label = { Text("Filtruj dzień") },
            trailingIcon = {
                Icon(Icons.Default.KeyboardArrowDown, "Rozwiń", Modifier.clickable { expanded = true })
            },
            modifier = Modifier.fillMaxWidth()
        )
        Box(modifier = Modifier
            .matchParentSize()
            .clickable { expanded = true })

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Wszystkie") },
                onClick = { onDaySelected(null); expanded = false }
            )
            daysMap.forEach { (id, name) ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = { onDaySelected(id); expanded = false }
                )
            }
        }
    }
}

@Composable
fun ScheduleEditDialog(
    item: ScheduleItem?,
    daysMap: Map<Int, String>,
    error: String?,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (ScheduleItem) -> Unit
) {
    // Stan formularza
    var selectedDay by remember { mutableStateOf(item?.dayOfWeek ?: 1) }
    var startTime by remember { mutableStateOf(item?.startTime?.take(5) ?: "08:00") }
    var endTime by remember { mutableStateOf(item?.endTime?.take(5) ?: "16:00") }
    var tempStr by remember { mutableStateOf(item?.targetTemp?.toString() ?: "21.0") }

    var dayExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (item == null) "Dodaj wpis" else "Edytuj wpis") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()) // Scroll dla małych ekranów
            ) {
                // --- MIEJSCE NA BŁĄD ---
                if (error != null) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))
                            .padding(8.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                }

                // --- POKAZUJEMY LOADING ---
                if (isLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                }

                // Wybór dnia
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = daysMap[selectedDay] ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Dzień tygodnia") },
                        trailingIcon = { Icon(Icons.Default.KeyboardArrowDown, null) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(modifier = Modifier.matchParentSize().clickable { dayExpanded = true })

                    DropdownMenu(expanded = dayExpanded, onDismissRequest = { dayExpanded = false }) {
                        daysMap.forEach { (id, name) ->
                            DropdownMenuItem(
                                text = { Text(name) },
                                onClick = { selectedDay = id; dayExpanded = false }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Czas Start
                OutlinedTextField(
                    value = startTime,
                    onValueChange = { startTime = it },
                    label = { Text("Start (HH:mm)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                // Czas Stop
                OutlinedTextField(
                    value = endTime,
                    onValueChange = { endTime = it },
                    label = { Text("Koniec (HH:mm)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                // Temperatura
                OutlinedTextField(
                    value = tempStr,
                    onValueChange = { tempStr = it },
                    label = { Text("Temperatura (°C)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val tempVal = tempStr.toDoubleOrNull() ?: 21.0
                    // Prosta walidacja czasu (dodajemy :00 jeśli brak sekund)
                    val sTime = if(startTime.length == 5) "$startTime:00" else startTime
                    val eTime = if(endTime.length == 5) "$endTime:00" else endTime

                    onConfirm(
                        ScheduleItem(
                            id = item?.id,
                            dayOfWeek = selectedDay,
                            startTime = sTime,
                            endTime = eTime,
                            targetTemp = tempVal
                        )
                    )
                },
                enabled = !isLoading // Blokujemy przycisk podczas zapisu
            ) { Text("Zapisz") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Anuluj") }
        }
    )
}