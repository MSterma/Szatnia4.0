package com.example.szatnia40.ui.Views

import android.content.ContentValues
import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.szatnia40.data.remote.AccessLog
import com.example.szatnia40.ui.ViewModels.AccessViewModel
import java.io.OutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun AccessHistoryView(
    modifier: Modifier = Modifier,
    viewModel: AccessViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Column(modifier = modifier.fillMaxSize()) {

        // --- NAGŁÓWEK Z PRZYCISKAMI ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Historia Wejść",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Row {
                // Przycisk PDF
                IconButton(onClick = {
                    if (state.logs.isNotEmpty()) {
                        generatePDF(context, state.logs)
                    } else {
                        Toast.makeText(context, "Brak danych do eksportu", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = "Eksport PDF")
                }

                // Przycisk Odświeżania
                IconButton(onClick = { viewModel.fetchLogs() }) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Odśwież")
                }
            }
        }

        // --- LISTA DANYCH ---
        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (state.error != null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Błąd: ${state.error}", color = Color.Red)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(state.logs) { log ->
                    AccessLogItem(log)
                }
            }
        }
    }
}

@Composable
fun AccessLogItem(log: AccessLog) {
    // Proste formatowanie daty (zakładamy że przychodzi ISO, np. 2026-01-10T15:00:00)
    val formattedDate = try {
        val parsed = LocalDateTime.parse(log.data, DateTimeFormatter.ISO_DATE_TIME) // Parsowanie ISO (może wymagać api level 26+)
        parsed.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
    } catch (e: Exception) {
        log.data // Jak się nie uda sparsować, wyświetl surową
    }

    // Kolor w zależności od typu (wejscie/wyjscie)
    val statusColor = if (log.typ.lowercase().contains("wej")) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = statusColor)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "${log.imie} ${log.nazwisko}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = formattedDate,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
            Text(
                text = log.typ.uppercase(),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color.DarkGray
            )
        }
    }
}

// --- FUNKCJA GENERUJĄCA PDF ---
fun generatePDF(context: Context, data: List<AccessLog>) {
    val pdfDocument = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // Format A4
    val page = pdfDocument.startPage(pageInfo)
    val canvas = page.canvas
    val paint = Paint()

    // Nagłówek
    paint.textSize = 18f
    paint.isFakeBoldText = true
    canvas.drawText("Raport Dostępu - Szatnia40", 40f, 50f, paint)

    paint.textSize = 12f
    paint.isFakeBoldText = false
    val dateNow = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
    canvas.drawText("Wygenerowano: $dateNow", 40f, 70f, paint)

    // Linie tabeli
    var y = 110f
    paint.textSize = 12f

    // Nagłówki kolumn
    paint.isFakeBoldText = true
    canvas.drawText("Data", 40f, y, paint)
    canvas.drawText("Pracownik", 200f, y, paint)
    canvas.drawText("Typ", 450f, y, paint)
    y += 20f
    paint.isFakeBoldText = false

    // Wiersze
    for (log in data) {
        // Formatowanie daty do PDF
        val dateStr = try {
            LocalDateTime.parse(log.data, DateTimeFormatter.ISO_DATE_TIME)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
        } catch (e: Exception) { log.data }

        canvas.drawText(dateStr, 40f, y, paint)
        canvas.drawText("${log.imie} ${log.nazwisko}", 200f, y, paint)
        canvas.drawText(log.typ, 450f, y, paint)

        y += 20f

        // Jeśli koniec strony, przerwij (prosta obsługa 1 strony dla przykładu)
        if (y > 800f) break
    }

    pdfDocument.finishPage(page)

    // Zapisywanie pliku w "Pobrane" (działa na Android 10+)
    try {
        val filename = "Raport_Dostepu_${System.currentTimeMillis()}.pdf"
        val outputStream: OutputStream?

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = context.contentResolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
            outputStream = uri?.let { context.contentResolver.openOutputStream(it) }
        } else {
            // Dla starszych Androidów (wymaga uprawnień WRITE_EXTERNAL_STORAGE w Manifeście)
            val file = java.io.File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), filename)
            outputStream = java.io.FileOutputStream(file)
        }

        outputStream?.use {
            pdfDocument.writeTo(it)
        }

        Toast.makeText(context, "Zapisano PDF w Pobranych", Toast.LENGTH_LONG).show()

    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Błąd zapisu PDF: ${e.message}", Toast.LENGTH_LONG).show()
    } finally {
        pdfDocument.close()
    }
}