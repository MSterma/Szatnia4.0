package com.example.szatnia40.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.szatnia40.ui.auth.AuthViewModel


@Composable
fun testView(authViewModel: AuthViewModel = hiltViewModel()){
    val token by authViewModel.tokenFlow.collectAsState()
    Column (
        modifier =  Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally

    ){

        Text("Mój super tajny token to: $token")
        Button(onClick = { authViewModel.logout() }) {
            Text(text = "Wyloguj")
        }
    }
}