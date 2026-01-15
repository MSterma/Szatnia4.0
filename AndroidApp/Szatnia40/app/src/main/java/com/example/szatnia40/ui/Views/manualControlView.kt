package com.example.szatnia40.ui.Views

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.szatnia40.ui.ViewModels.ManualControlViewModel

@Composable
fun manualControlView(
    modifier: Modifier = Modifier,
    viewModel: ManualControlViewModel = hiltViewModel() // Wstrzykujemy ViewModel
) {
    // Pobieramy stan z ViewModelu
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {

        // --- PRZEŁĄCZNIK TRYBU MANUALNEGO ---
        Row(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Sterowanie manualne", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            Column {
                Switch(
                    checked = state.isManualMode,
                    onCheckedChange = { viewModel.onManualModeChange(it) }
                )
            }
        }

        // --- OPCJE STEROWANIA ---
        AnimatedVisibility(
            visible = state.isManualMode,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column {
                ControlRow("Uruchom wiatrak") {
                    Switch(
                        checked = state.fan,
                        onCheckedChange = { viewModel.onFanChange(it) }
                    )
                }

                ControlRow("Uruchom piec") {
                    Switch(
                        checked = state.furnace,
                        onCheckedChange = { viewModel.onFurnaceChange(it) }
                    )
                }

                HeaterRow("Grzejnik 1", state.heater1) { viewModel.onHeater1Change(it) }
                HeaterRow("Grzejnik 2", state.heater2) { viewModel.onHeater2Change(it) }
                HeaterRow("Grzejnik 3", state.heater3) { viewModel.onHeater3Change(it) }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // --- PRZYCISK WYSYŁANIA ---
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = { viewModel.saveSettings() }, // Wyślij do bazy
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoading // Zablokuj przycisk jak wysyła
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = androidx.compose.ui.graphics.Color.White
                    )
                } else {
                    Text(if (state.isManualMode) "Zastosuj ustawienia manualne" else "Wyłącz tryb manualny")
                }
            }
        }
        Spacer(modifier = Modifier.height(80.dp))
    }
}

// Funkcje pomocnicze zostawiamy bez zmian
@Composable
fun ControlRow(label: String, content: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label)
        content()
    }
}

@Composable
fun HeaterRow(label: String, value: Float, onValueChange: (Float) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label)
        Column(horizontalAlignment = Alignment.End) {
            Slider(
                value = value,
                onValueChange = onValueChange,
                steps = 5, // Zwiększyłem liczbę kroków dla precyzji 0.5
                valueRange = 0f..5f,
                modifier = Modifier.width(180.dp)
            )
            Text(text = "%.1f".format(value))
        }
    }
}