package com.example.szatnia40.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.input.key.Key.Companion.Home
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun Navigation() {
    val navController= rememberNavController();
    NavHost(navController = navController, startDestination = "loginView" ){
        composable("loginView"){ loginView() }
    }
}