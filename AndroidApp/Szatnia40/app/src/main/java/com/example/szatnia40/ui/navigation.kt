package com.example.szatnia40.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun navigation() {
    val navController= rememberNavController();
    NavHost(navController = navController, startDestination = "loginView" ){
        composable("loginView"){ loginView() }
    }
}