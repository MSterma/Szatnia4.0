package com.example.szatnia40.data.remote

import com.google.gson.annotations.SerializedName

// 1. Model do widoku: api.stan_systemu

data class ComponentStatus(
    @SerializedName("nazwa_komponentu") val name: String,

    // ZMIANA TUTAJ: Było "aktualny_odczyt", musi być "wartosc" (bo tak mówi log)
    @SerializedName("wartosc") val currentValue: Float?,

)
data class ConfigUpdate(
    @SerializedName("tryb_manualny") val isManualMode: Boolean
)
// 2. Model do widoku: api.pracownicy
data class Employee(
    val id: Int,
    val imie: String,
    val nazwisko: String,
    val karta: String,
    @SerializedName("aktywny") val isActive: Boolean // Nowe pole z bazy
)
data class EmployeeUpdate(
    val imie: String,
    val nazwisko: String,
    val karta: String,
    @SerializedName("aktywny") val isActive: Boolean
)
// 3. Model do funkcji RPC: api.pobierz_pomiary (Request i Response)
data class MeasurementRequest(
    val komponent: String,
    val zakres: String // "1h", "24h", "72h"
)

data class MeasurementPoint(
    val data: String, // Timestamp przyjdzie jako String
    val wartosc: Float
)

// 4. Model do funkcji RPC: api.ustaw_sterowanie
data class ControlRequest(
    val komponent: String,
    val wartosc: Float
)
data class AccessLog(
    val imie: String,
    val nazwisko: String,
    val data: String, // PostgREST zwraca datę jako String (ISO 8601)
    val typ: String   // np. "wejscie", "wyjscie"
)

data class ScheduleItem(
    val id: Int? = null, // Null przy dodawaniu, ustawione przy edycji
    @SerializedName("dzien_tygodnia") val dayOfWeek: Int, // 1-7
    @SerializedName("godzina_rozpoczecia") val startTime: String, // format "HH:mm:ss"
    @SerializedName("godzina_zakonczenia") val endTime: String,
    @SerializedName("zadana_temperatura") val targetTemp: Double
)

// Model do wysyłania (bez ID)
data class ScheduleRequest(
    @SerializedName("dzien_tygodnia") val dayOfWeek: Int,
    @SerializedName("godzina_rozpoczecia") val startTime: String,
    @SerializedName("godzina_zakonczenia") val endTime: String,
    @SerializedName("zadana_temperatura") val targetTemp: Float
)

data class NotificationItem(
    val id: Int,
    @SerializedName("data_wystapienia") val date: String,
    @SerializedName("kod_bledu") val errorCode: String,
    @SerializedName("komunikat") val message: String
)
