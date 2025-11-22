package com.example.szatnia40.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.szatnia40.ui.auth.AuthViewModel

@Composable
fun loginView(authViewModel: AuthViewModel = hiltViewModel()){

    var login by remember {
        mutableStateOf("")
    }
    var password by remember {
        mutableStateOf("")
    }
    Column (
       modifier =  Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally

    ){
        Text("Zaloguj się")
        Spacer(modifier =Modifier.height(16.dp))
        OutlinedTextField(value = login, onValueChange ={login =it}, label = { Text(text = "login")} )
        Spacer(modifier =Modifier.height(16.dp))
        OutlinedTextField(value = password, onValueChange ={password=it},
            label = { Text(text = "hasło")},
            visualTransformation =PasswordVisualTransformation()
        )
        Spacer(modifier =Modifier.height(16.dp))
        Button(onClick = { authViewModel.saveUserToken("$login $password")}) {
            Text(text = "zaloguj")

        }
    }
}
