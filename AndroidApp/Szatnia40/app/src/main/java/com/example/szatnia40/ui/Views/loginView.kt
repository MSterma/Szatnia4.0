package com.example.szatnia40.ui.Views

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.szatnia40.ui.ViewModels.AuthViewModel

@Composable
fun loginView(
    authViewModel: AuthViewModel
) {
    val login by authViewModel.login.collectAsState()
    val password by authViewModel.password.collectAsState()
    val isLoading by authViewModel.isLoading.collectAsState()
    val error by authViewModel.error.collectAsState()

    // Manager do sterowania fokusem (żeby przełączać z loginu na hasło)
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Logowanie Administratora", fontSize = 24.sp, modifier = Modifier.padding(bottom = 32.dp))

        // --- POLE LOGINU ---
        OutlinedTextField(
            value = login,
            onValueChange = { authViewModel.onLoginChange(it) },
            label = { Text("Login") },
            // 1. Tylko jedna linia (blokuje enter jako nową linię)
            singleLine = true,
            // 2. Ustawienia klawiatury: Przycisk "Dalej" (Strzałka w prawo/dół)
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Next
            ),
            // 3. Co robić po kliknięciu "Dalej": Przejdź do następnego pola (hasła)
            keyboardActions = KeyboardActions(
                onNext = { focusManager.clearFocus(); focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) }
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // --- POLE HASŁA ---
        OutlinedTextField(
            value = password,
            onValueChange = { authViewModel.onPasswordChange(it) },
            label = { Text("Hasło") },
            visualTransformation = PasswordVisualTransformation(),
            // 1. Tylko jedna linia (To rozwiązuje Twój główny problem)
            singleLine = true,
            // 2. Ustawienia klawiatury: Typ Hasło + Przycisk "Gotowe" (Ptazsek)
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            // 3. Co robić po kliknięciu "Gotowe": Ukryj klawiaturę i spróbuj się zalogować
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus() // Chowa klawiaturę
                    authViewModel.performLogin() // Wywołuje logowanie
                }
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (error != null) {
            Text(
                text = error!!,
                color = Color.Red,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        Button(
            onClick = {
                focusManager.clearFocus()
                authViewModel.performLogin()
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Zaloguj")
            }
        }
    }
}