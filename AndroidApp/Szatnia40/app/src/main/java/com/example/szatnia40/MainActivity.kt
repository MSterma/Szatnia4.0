package com.example.szatnia40

import com.example.szatnia40.ui.auth.AuthViewModel
import com.example.szatnia40.ui.sidebar
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.szatnia40.ui.AppScreen
import com.example.szatnia40.ui.currentStateView
import com.example.szatnia40.ui.loginView
import com.example.szatnia40.ui.manualControlView
import com.example.szatnia40.ui.testView
import com.example.szatnia40.ui.theme.Szatnia40Theme
import dagger.hilt.android.AndroidEntryPoint
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {

            Szatnia40Theme{
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {

                    Check()
                }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}
    @Composable
    fun Check(
        authViewModel: AuthViewModel = hiltViewModel()
    ) {
        var currentScreen by remember { mutableStateOf(AppScreen.Test) }
        BackHandler(enabled = currentScreen != AppScreen.Test) {
            currentScreen = AppScreen.Test
        }

        val token by authViewModel.tokenFlow.collectAsState()

        if (token != null) {
            //w przyszłości zamienić na jakiś widok
                sidebar(
                    currentScreen = currentScreen,
                    onScreenSelected = { newScreen ->
                        currentScreen = newScreen // Tutaj następuje zmiana ekranu!

                    }


                ){innerPadding ->
                    when (currentScreen) {
                        AppScreen.Test -> testView(authViewModel = authViewModel)
                        AppScreen.Manual -> manualControlView()
                        AppScreen.CurrState -> currentStateView();
                        AppScreen.Accesses -> testView(authViewModel = authViewModel)
                        AppScreen.Employees -> testView(authViewModel = authViewModel)
                    }
                // To jest to, co przekazujesz na wejście (content):

            }





        } else {
            loginView(authViewModel = authViewModel)
        }
    }
@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    Szatnia40Theme {
        Greeting("Android")
    }
}}