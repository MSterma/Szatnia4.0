package com.example.szatnia40.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu // Dodano import
import androidx.compose.material.icons.outlined.Info // Zmiana z AutoMirrored na standardowy
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Divider // Zmiana z HorizontalDivider
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api // Często potrzebne przy TopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton // Dodano import
import androidx.compose.material3.MaterialTheme // Dodano import
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold // Dodano import
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar // Dodano import
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch // Dodano import
enum class AppScreen { Test, Manual,CurrState,Employees,Accesses }
@OptIn(ExperimentalMaterial3Api::class) // Wymagane dla TopAppBar w starszych wersjach M3
@Composable
fun sidebar(
    currentScreen: AppScreen,
    onScreenSelected: (AppScreen) -> Unit,
    content: @Composable (PaddingValues) -> Unit,

) {

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerContent = {
            ModalDrawerSheet {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Spacer(Modifier.height(12.dp))
                    Text("Szatnia 4.0", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleLarge)

                    // ZMIANA: HorizontalDivider -> Divider (dla kompatybilności z BOM 2023.10.01)
                    Divider()

                    Text("Dostępne opcje", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleMedium)
                    NavigationDrawerItem(
                        label = { Text(" Wygeneruj raport") },
                        selected = false,
                        onClick = { scope.launch {   drawerState.close()} }
                    )
                    NavigationDrawerItem(
                        label = { Text("Sterowanie manualne") },
                        selected = currentScreen == AppScreen.Manual,
                        onClick = {
                            onScreenSelected(AppScreen.Manual)
                            scope.launch {   drawerState.close()} }
                    )
                    NavigationDrawerItem(
                        label = { Text("Aktualny stan") },
                        selected = currentScreen == AppScreen.CurrState,
                        onClick = {
                            onScreenSelected(AppScreen.CurrState)
                            scope.launch {   drawerState.close()} }
                    )
                    NavigationDrawerItem(
                        label = { Text("Pracownicy") },
                        selected = currentScreen == AppScreen.Employees,
                        onClick = {
                            onScreenSelected(AppScreen.Employees)
                            scope.launch {   drawerState.close()} }
                    )
                    NavigationDrawerItem(
                        label = { Text("Dostęp do pomieszczenia") },
                        selected = currentScreen == AppScreen.Accesses,
                        onClick = {
                            onScreenSelected(AppScreen.Accesses)
                            scope.launch {   drawerState.close()} }
                    )

                    // ZMIANA: HorizontalDivider -> Divider
                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    Spacer(Modifier.height(12.dp))
                }
            }
        },
        drawerState = drawerState
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Szatnia 4.0") },
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch {
                                if (drawerState.isClosed) {
                                    drawerState.open()
                                } else {
                                    drawerState.close()
                                }
                            }
                        }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    }
                )
            }
        ) { innerPadding ->
            content(innerPadding)
        }
    }
}