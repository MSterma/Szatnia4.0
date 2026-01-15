package com.example.szatnia40.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

enum class AppScreen { Test,Schedule, Report, Manual, CurrState, Employees, Accesses, Login,Notifications }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun sidebar(
    currentScreen: AppScreen,
    onScreenSelected: (AppScreen) -> Unit,
    onLogout: () -> Unit, // <--- NOWY PARAMETR: Callback do wylogowania
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
                    Divider()
                    Text("Dostępne opcje", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleMedium)

                    // --- Elementy Menu ---
                    NavigationDrawerItem(
                        label = { Text("Wygeneruj raport") },
                        selected = currentScreen == AppScreen.Report,
                        onClick = { onScreenSelected(AppScreen.Report); scope.launch { drawerState.close() } }
                    )
                    NavigationDrawerItem(
                        label = { Text("Harmonogram") },
                        selected = currentScreen == AppScreen.Schedule,
                        onClick = { onScreenSelected(AppScreen.Schedule); scope.launch { drawerState.close() } }
                    )
                    NavigationDrawerItem(
                        label = { Text("Sterowanie manualne") },
                        selected = currentScreen == AppScreen.Manual,
                        onClick = { onScreenSelected(AppScreen.Manual); scope.launch { drawerState.close() } }
                    )
                    NavigationDrawerItem(
                        label = { Text("Aktualny stan") },
                        selected = currentScreen == AppScreen.CurrState,
                        onClick = { onScreenSelected(AppScreen.CurrState); scope.launch { drawerState.close() } }
                    )
                    NavigationDrawerItem(
                        label = { Text("Pracownicy") },
                        selected = currentScreen == AppScreen.Employees,
                        onClick = { onScreenSelected(AppScreen.Employees); scope.launch { drawerState.close() } }
                    )
                    NavigationDrawerItem(
                        label = { Text("Dostęp do pomieszczenia") },
                        selected = currentScreen == AppScreen.Accesses,
                        onClick = { onScreenSelected(AppScreen.Accesses); scope.launch { drawerState.close() } }
                    )
                    NavigationDrawerItem(
                        label = { Text("Powiadomienia") },
                        selected = currentScreen == AppScreen.Notifications,
                        onClick = { onScreenSelected(AppScreen.Notifications); scope.launch { drawerState.close() } }
                    )

                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    // --- PRZYCISK WYLOGUJ ---
                    NavigationDrawerItem(
                        label = { Text(text = "Wyloguj") },
                        // Uwaga: Jeśli Icons.AutoMirrored nie działa, użyj Icons.Default.ExitToApp lub Icons.Filled.ExitToApp
                        icon = { Icon(imageVector = Icons.Filled.ExitToApp, contentDescription = "Wyloguj") },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            onLogout() // Delegujemy akcję do MainActivity
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )


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
                            scope.launch { if (drawerState.isClosed) drawerState.open() else drawerState.close() }
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