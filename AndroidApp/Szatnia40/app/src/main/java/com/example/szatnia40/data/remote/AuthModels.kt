package com.example.szatnia40.data.remote
import com.google.gson.annotations.SerializedName

// Nowy model - wysyłamy tylko hasło
data class AdminLoginRequest(
    @SerializedName("pass") val pass: String
)

data class LoginResponse(
    @SerializedName("token") val token: String
)