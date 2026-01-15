package com.example.szatnia40

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.szatnia40.ui.*
import com.example.szatnia40.ui.ViewModels.AuthViewModel // Upewnij się, że ścieżka jest poprawna (poprawiłem na standardową)
import com.example.szatnia40.ui.Views.AccessHistoryView
import com.example.szatnia40.ui.Views.EmployeesView
import com.example.szatnia40.ui.Views.NotificationsView
import com.example.szatnia40.ui.Views.ReportView
import com.example.szatnia40.ui.Views.ScheduleView
import com.example.szatnia40.ui.Views.currentStateView
import com.example.szatnia40.ui.Views.loginView
import com.example.szatnia40.ui.Views.manualControlView
import com.example.szatnia40.ui.Views.testView
import com.example.szatnia40.ui.theme.Szatnia40Theme
import com.example.szatnia40.worker.NotificationWorker
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Szatnia40Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // --- LOGIKA STARTOWA (Uprawnienia + Kanał + Worker) ---
                    val context = LocalContext.current

                    // Launcher do pytania o zgodę (Android 13+)
                    val permissionLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestPermission()
                    ) { isGranted ->
                        // Tutaj można dodać logikę, np. logowanie decyzji użytkownika
                    }

                    LaunchedEffect(Unit) {
                        // 1. NAJWAŻNIEJSZE: Utwórz kanał powiadomień NATYCHMIAST
                        // To odblokuje suwak w ustawieniach systemu!
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            val channelId = "szatnia_alerts" // Musi być identyczne jak w Workerze
                            val channelName = "Alerty Szatni"
                            val importance = NotificationManager.IMPORTANCE_HIGH

                            val channel = NotificationChannel(channelId, channelName, importance).apply {
                                description = "Powiadomienia o awariach i temperaturze"
                            }

                            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                            notificationManager.createNotificationChannel(channel)
                        }

                        // 2. Poproś o uprawnienia (jeśli Android 13+)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                        }

                        // 3. Uruchom Workera (co 15 minut)
                        val workRequest = PeriodicWorkRequestBuilder<NotificationWorker>(15, TimeUnit.MINUTES)
                            .build()

                        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                            "SzatniaNotificationWork",
                            ExistingPeriodicWorkPolicy.KEEP,
                            workRequest
                        )
                    }

                    // --- URUCHOMIENIE APLIKACJI ---
                    Check()
                }
            }
        }
    }
}

@Composable
fun Check(
    authViewModel: AuthViewModel = hiltViewModel()
) {
    var currentScreen by remember { mutableStateOf(AppScreen.Test) }

    BackHandler(enabled = currentScreen != AppScreen.Test) {
        currentScreen = AppScreen.Test
    }

    val token by authViewModel.tokenFlow.collectAsState()

    if (token != null) {
        sidebar(
            currentScreen = currentScreen,
            onScreenSelected = { newScreen ->
                currentScreen = newScreen
            },
            onLogout = {
                authViewModel.logout()
            }
        ) { innerPadding ->
            when (currentScreen) {
                AppScreen.Test -> testView(authViewModel = authViewModel)
                AppScreen.Report -> ReportView(modifier = Modifier.padding(innerPadding))
                AppScreen.Manual -> manualControlView(modifier = Modifier.padding(innerPadding))
                AppScreen.CurrState -> currentStateView()
                AppScreen.Accesses -> AccessHistoryView(modifier = Modifier.padding(innerPadding))
                AppScreen.Employees -> EmployeesView(modifier = Modifier.padding(innerPadding))
                AppScreen.Schedule -> ScheduleView(modifier = Modifier.padding(innerPadding))
                AppScreen.Notifications -> NotificationsView(modifier = Modifier.padding(innerPadding))
                else -> {} // Obsługa Login jest niżej, więc tu else jest ok
            }
        }
    } else {
        loginView(authViewModel = authViewModel)
    }
}