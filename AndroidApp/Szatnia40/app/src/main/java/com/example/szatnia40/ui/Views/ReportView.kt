package com.example.szatnia40.ui.Views

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.szatnia40.data.remote.MeasurementPoint
import com.example.szatnia40.ui.ViewModels.ReportViewModel
import java.text.SimpleDateFormat
import java.util.Locale

// --- GŁÓWNY WIDOK RAPORTU ---
@Composable
fun ReportView(
    modifier: Modifier = Modifier,
    viewModel: ReportViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    // Lista nazw do wyświetlenia w dropdownie
    val componentNames = viewModel.componentMap.keys.toList()

    // Znajdź aktualną nazwę wyświetlaną
    val currentDisplayName = viewModel.componentMap.entries
        .find { it.value == state.selectedComponent }?.key ?: "Wybierz"

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {

        // --- NAGŁÓWEK ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Raport Pomiary", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            IconButton(onClick = { viewModel.fetchData() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Odśwież")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // --- WYBÓR KOMPONENTU ---
        Text("Wybierz komponent:", fontSize = 14.sp, color = Color.Gray)
        ComponentDropdown(
            items = componentNames,
            selectedItem = currentDisplayName,
            onItemSelected = { viewModel.onComponentChange(it) }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // --- WYBÓR ZAKRESU ---
        Text("Zakres czasu:", fontSize = 14.sp, color = Color.Gray)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            RangeButton("1h", state.selectedRange) { viewModel.onRangeChange("1h") }
            RangeButton("24h", state.selectedRange) { viewModel.onRangeChange("24h") }
            RangeButton("72h", state.selectedRange) { viewModel.onRangeChange("72h") }
        }

        Spacer(modifier = Modifier.height(30.dp))

        // --- WYKRES ---
        if (state.isLoading) {
            Box(Modifier.fillMaxWidth().height(250.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (state.data.isEmpty()) {
            Box(Modifier.fillMaxWidth().height(250.dp), contentAlignment = Alignment.Center) {
                Text("Brak danych dla wybranego okresu", color = Color.Gray)
            }
        } else {
            Text("Wykres wartości:", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(10.dp))

            // Rysowanie wykresu
            CustomChart(
                dataPoints = state.data,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp) // Zwiększyłem wysokość dla lepszej czytelności osi
                    .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
                    .padding(10.dp)
            )
        }
    }
}

// --- KOMPONENT: PRZYCISK ZAKRESU ---
@Composable
fun RangeButton(label: String, currentSelection: String, onClick: () -> Unit) {
    val isSelected = label == currentSelection
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray
        )
    ) {
        Text(label)
    }
}

// --- KOMPONENT: DROPDOWN ---
@Composable
fun ComponentDropdown(items: List<String>, selectedItem: String, onItemSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selectedItem,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
            modifier = Modifier.fillMaxWidth().clickable { expanded = true },
            enabled = false,
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledBorderColor = MaterialTheme.colorScheme.outline,
                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
        Box(modifier = Modifier.matchParentSize().clickable { expanded = true })

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            items.forEach { label ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onItemSelected(label)
                        expanded = false
                    }
                )
            }
        }
    }
}

// --- NOWY, ULEPSZONY WYKRES ---
@Composable
fun CustomChart(
    dataPoints: List<MeasurementPoint>,
    modifier: Modifier = Modifier
) {
    // Kolory
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = Color.LightGray.copy(alpha = 0.5f)
    val textColor = android.graphics.Color.DKGRAY

    Canvas(modifier = modifier) {
        if (dataPoints.isEmpty()) return@Canvas

        // 1. KONFIGURACJA MARGINESÓW (Miejsce na napisy)
        val paddingLeft = 120f // Miejsce na wartości Y
        val paddingBottom = 60f // Miejsce na czas X

        // Obszar roboczy samego wykresu
        val chartWidth = size.width - paddingLeft
        val chartHeight = size.height - paddingBottom

        // 2. OBLICZENIA SKALI
        val maxVal = dataPoints.maxOf { it.wartosc }
        val minVal = dataPoints.minOf { it.wartosc }
        val range = if (maxVal == minVal) 1f else maxVal - minVal

        // Funkcje pomocnicze
        fun getX(index: Int): Float {
            return paddingLeft + (index.toFloat() / (dataPoints.size - 1).coerceAtLeast(1)) * chartWidth
        }

        fun getY(value: Float): Float {
            val normalized = (value - minVal) / range
            return (1f - normalized) * chartHeight
        }

        // 3. RYSOWANIE SIATKI I OPISÓW OSI Y
        val stepsY = 4
        val textPaint = Paint().apply {
            color = textColor
            textSize = 30f
            textAlign = Paint.Align.RIGHT
            typeface = Typeface.DEFAULT
        }

        for (i in 0..stepsY) {
            val ratio = i.toFloat() / stepsY
            val value = minVal + (range * ratio)
            val yPos = (1f - ratio) * chartHeight

            drawLine(
                color = gridColor,
                start = Offset(paddingLeft, yPos),
                end = Offset(size.width, yPos),
                strokeWidth = 2f
            )

            drawContext.canvas.nativeCanvas.drawText(
                "%.1f".format(value),
                paddingLeft - 15f,
                yPos + 10f,
                textPaint
            )
        }

        // 4. RYSOWANIE SIATKI I OPISÓW OSI X
        val stepsX = 4
        val timePaint = Paint().apply {
            color = textColor
            textSize = 28f
            textAlign = Paint.Align.CENTER
        }

        for (i in 0..stepsX) {
            val index = ((dataPoints.size - 1) * (i.toFloat() / stepsX)).toInt()
            if (index < dataPoints.size) {
                val point = dataPoints[index]
                val xPos = getX(index)

                drawLine(
                    color = gridColor,
                    start = Offset(xPos, 0f),
                    end = Offset(xPos, chartHeight),
                    strokeWidth = 2f
                )

                val timeLabel = formatChartDate(point.data)
                drawContext.canvas.nativeCanvas.drawText(
                    timeLabel,
                    xPos,
                    chartHeight + 40f,
                    timePaint
                )
            }
        }

        // 5. RYSOWANIE LINII WYKRESU
        val path = Path()
        dataPoints.forEachIndexed { index, point ->
            val x = getX(index)
            val y = getY(point.wartosc)

            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }

        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 6.dp.toPx())
        )

        if (dataPoints.size < 30) {
            dataPoints.forEachIndexed { index, point ->
                drawCircle(
                    color = lineColor,
                    radius = 4.dp.toPx(),
                    center = Offset(getX(index), getY(point.wartosc))
                )
            }
        }
    }
}

fun formatChartDate(isoDate: String): String {
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val date = parser.parse(isoDate)
        val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
        date?.let { formatter.format(it) } ?: "??:??"
    } catch (e: Exception) {
        if (isoDate.contains("T")) {
            isoDate.substringAfter("T").take(5)
        } else {
            isoDate
        }
    }
}