package com.example.szatnia40.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.szatnia40.R
import com.example.szatnia40.data.remote.PostgrestApi
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class NotificationWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val api: PostgrestApi,
    private val prefs: SharedPreferences
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val token = prefs.getString("auth_token", null)
            if (token == null) return Result.success() // Nie zalogowany - nie sprawdzaj

            // 1. Pobierz ostatnio widziane ID (domyślnie 0)
            val lastId = prefs.getInt("last_notification_id", 0)

            // 2. Zapytaj API o nowsze wpisy (id > lastId)
            val bearerToken = "Bearer $token"
            val newItems = api.getNewNotifications(bearerToken, "gt.$lastId")

            if (newItems.isNotEmpty()) {
                val latest = newItems.first()

                // 3. Wyświetl powiadomienie systemowe
                showSystemNotification(latest.errorCode, latest.message)

                // 4. Zapisz nowe ID jako ostatnio widziane
                prefs.edit().putInt("last_notification_id", latest.id).apply()
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry() // Spróbuj ponownie później, jeśli błąd sieci
        }
    }

    private fun showSystemNotification(title: String, message: String) {
        val channelId = "szatnia_alerts"
        val context = applicationContext

        // Kanał powiadomień (wymagany dla Android 8.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Alerty Szatni",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Powiadomienia o awariach i temperaturze"
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        // Budowanie powiadomienia
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Upewnij się, że masz ikonę
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        // Wyświetlenie (try-catch na wypadek braku uprawnień w Android 13)
        try {
            NotificationManagerCompat.from(context).notify(System.currentTimeMillis().toInt(), notification)
        } catch (e: SecurityException) {
            // Brak uprawnień POST_NOTIFICATIONS
        }
    }
}