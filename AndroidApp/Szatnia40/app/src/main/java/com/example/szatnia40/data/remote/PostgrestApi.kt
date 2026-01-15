package com.example.szatnia40.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Query

interface PostgrestApi {

    // --- LOGOWANIE (bez zmian) ---
    @POST("rpc/login_as_admin")
    suspend fun loginAdmin(@Body request: AdminLoginRequest): LoginResponse

    // --- POBIERANIE STANU (Widok: api.stan_systemu) ---
    // Zwraca LISTĘ komponentów
    @GET("stan_systemu")
    suspend fun getSystemStatus(
        @Header("Authorization") token: String
    ): List<ComponentStatus>
    @PATCH("pracownicy")
    suspend fun updateEmployee(
        @Header("Authorization") token: String,
        @Query("id") id: String, // np. "eq.18"
        @Body update: EmployeeUpdate
    ): Response<Unit>
    // --- STEROWANIE (RPC: api.ustaw_sterowanie) ---
    @POST("rpc/ustaw_sterowanie")
    suspend fun setControl(
        @Header("Authorization") token: String,
        @Body body: ControlRequest
    ): Response<Unit>
    @PATCH("konfiguracja")
    suspend fun updateConfig(
        @Header("Authorization") token: String,
        @Query("id") id: String = "eq.1",
        @Body update: ConfigUpdate
    ): Response<Unit>
    // --- WYKRESY (RPC: api.pobierz_pomiary) ---
    @POST("rpc/pobierz_pomiary")
    suspend fun getMeasurements(
        @Header("Authorization") token: String,
        @Body body: MeasurementRequest
    ): List<MeasurementPoint>

    // --- PRACOWNICY (Widok: api.pracownicy) ---
    @GET("pracownicy")
    suspend fun getEmployees(
        @Header("Authorization") token: String
    ): List<Employee>
    @GET("historia_wejsc")
    suspend fun getAccessHistory(
        @Header("Authorization") token: String,
        @Query("order") order: String = "data.desc"
    ): List<AccessLog>
    @GET("harmonogram")
    suspend fun getSchedule(
        @Header("Authorization") token: String,
        @Query("order") order: String = "dzien_tygodnia.asc,godzina_rozpoczecia.asc"
    ): List<ScheduleItem>

    // 2. Dodawanie wpisu
    @POST("harmonogram")
    suspend fun addScheduleItem(
        @Header("Authorization") token: String,
        @Body item: ScheduleItem
    ): Response<Unit>

    // 3. Edycja wpisu (PATCH wymaga ID w URL)
    @PATCH("harmonogram")
    suspend fun updateScheduleItem(
        @Header("Authorization") token: String,
        @Query("id") id: String, // np. "eq.5"
        @Body item: ScheduleItem
    ): Response<Unit>

    // 4. Usuwanie (opcjonalnie, skoro edytujemy)
    @DELETE("harmonogram")
    suspend fun deleteScheduleItem(
        @Header("Authorization") token: String,
        @Query("id") id: String
    ): Response<Unit>
    @GET("powiadomienia")
    suspend fun getNotifications(
        @Header("Authorization") token: String,
        @Query("order") order: String = "data_wystapienia.desc" // Najnowsze na górze
    ): List<NotificationItem>
    @GET("powiadomienia")
    suspend fun getNewNotifications(
        @Header("Authorization") token: String,
        @Query("id") idFilter: String, // np. "gt.50"
        @Query("order") order: String = "id.desc",
        @Query("limit") limit: String = "1" // Pobieramy tylko najnowsze, żeby nie spamować
    ): List<NotificationItem>
    // W pliku PostgrestApi.kt

    @DELETE("powiadomienia")
    suspend fun deleteNotification(
        @Header("Authorization") token: String,
        @Query("id") id: String // np. "eq.123"
    ): retrofit2.Response<Unit>
}