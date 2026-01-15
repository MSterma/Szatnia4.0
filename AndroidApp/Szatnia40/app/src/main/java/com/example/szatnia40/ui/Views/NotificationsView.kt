package com.example.szatnia40.ui.Views

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.szatnia40.data.remote.NotificationItem
import com.example.szatnia40.ui.NotificationsViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun NotificationsView(
    modifier: Modifier = Modifier,
    viewModel: NotificationsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    // Odświeżanie przy wejściu na ekran
    LaunchedEffect(Unit) {
        viewModel.fetchNotifications()
    }

    Column(modifier = modifier.padding(16.dp)) {

        // --- NAGŁÓWEK Z PRZYCISKIEM ODŚWIEŻANIA ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween, // Rozsuwa elementy na boki
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Powiadomienia", style = MaterialTheme.typography.headlineMedium)

            IconButton(
                onClick = { viewModel.fetchNotifications() },
                enabled = !state.isLoading // Blokujemy klikanie, gdy już ładuje
            ) {
                // Jeśli ładuje, możemy np. zmienić kolor ikony na szary
                val tint = if (state.isLoading) Color.Gray else MaterialTheme.colorScheme.primary
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Odśwież",
                    tint = tint
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Pasek ładowania
        if (state.isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        // Błędy
        if (state.error != null) {
            Text(state.error!!, color = Color.Red, modifier = Modifier.padding(bottom = 8.dp))
        }

        // Lista
        LazyColumn(
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(state.items) { item ->
                NotificationCard(
                    item = item,
                    onDeleteClick = { viewModel.deleteNotification(item) }
                )
            }
        }
    }
}

@Composable
fun NotificationCard(
    item: NotificationItem,
    onDeleteClick: () -> Unit
) {
    val isError = item.errorCode.uppercase().contains("ERR")
    val icon = if (isError) Icons.Default.Warning else Icons.Default.Info
    val iconColor = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 16.dp, bottom = 16.dp, end = 8.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(32.dp).padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = item.errorCode,
                        fontWeight = FontWeight.Bold,
                        color = iconColor,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = formatNotifDate(item.date),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = item.message, style = MaterialTheme.typography.bodyMedium)
            }

            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier.size(24.dp).align(Alignment.Top)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Usuń powiadomienie",
                    tint = Color.Gray
                )
            }
        }
    }
}

fun formatNotifDate(isoDate: String): String {
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val date = parser.parse(isoDate)
        val formatter = SimpleDateFormat("dd.MM HH:mm", Locale.getDefault())
        date?.let { formatter.format(it) } ?: isoDate
    } catch (e: Exception) {
        isoDate.take(16).replace("T", " ")
    }
}