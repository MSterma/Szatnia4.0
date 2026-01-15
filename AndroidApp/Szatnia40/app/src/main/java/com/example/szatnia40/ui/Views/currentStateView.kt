package com.example.szatnia40.ui.Views

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.szatnia40.ui.ViewModels.CurrentStateViewModel

@Composable
fun currentStateView(
    viewModel: CurrentStateViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(50.dp))

        Text(
            text = "Aktualny stan układu",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(20.dp)
        )

        if (state == null) {
            Box(modifier = Modifier.height(200.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            Text("Pobieranie danych...", color = Color.Gray)
        } else {
            val data = state!!

            // --- TEMPERATURY ---
            Card(modifier = Modifier.padding(10.dp).fillMaxWidth()) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text("Temperatury", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Divider(modifier = Modifier.padding(vertical = 5.dp))
                    InfoRow("Pomieszczenie", "%.1f ℃".format(data.roomTemp))
                    InfoRow("Hala", "%.1f ℃".format(data.hallTemp))
                    InfoRow("Piec (Temp)", "%.1f ℃".format(data.furnaceTemp))
                }
            }

            // --- STATUS URZĄDZEŃ ---
            Card(modifier = Modifier.padding(10.dp).fillMaxWidth()) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text("Status Urządzeń", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Divider(modifier = Modifier.padding(vertical = 5.dp))

                    // Status pieca (Czy włączony - 1.0)
                    StatusRow("Piec (Status)", data.isFurnaceOn)

                    // Status wiatraka
                    StatusRow("Wiatrak", data.isFanOn)
                }
            }

            // --- GRZEJNIKI ---
            Card(modifier = Modifier.padding(10.dp).fillMaxWidth()) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text("Moc Grzejników", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Divider(modifier = Modifier.padding(vertical = 5.dp))
                    InfoRow("Grzejnik 1", data.heater1.toString())
                    InfoRow("Grzejnik 2", data.heater2.toString())
                    InfoRow("Grzejnik 3", data.heater3.toString())
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Button(onClick = { viewModel.fetchData() }) {
                Text("Odśwież")
            }
            Spacer(modifier = Modifier.height(50.dp))
        }
    }
}

// Funkcje pomocnicze dla ładniejszego kodu
@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 16.sp)
        Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun StatusRow(label: String, isActive: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 16.sp)
        if (isActive) {
            Text("WŁĄCZONY", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold) // Zielony
        } else {
            Text("WYŁĄCZONY", color = Color.Red, fontWeight = FontWeight.Bold)
        }
    }
}