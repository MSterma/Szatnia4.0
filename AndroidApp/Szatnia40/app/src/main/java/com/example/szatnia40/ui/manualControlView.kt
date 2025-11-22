package com.example.szatnia40.ui


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
fun manualControlView(){
    var furnace by remember { mutableStateOf(true) } // to zmiany na wartości z bazy
    var fan by remember { mutableStateOf(true) }
    var heater1 by remember { mutableFloatStateOf(0f) }
    var heater2 by remember { mutableFloatStateOf(0f) }
    var heater3 by remember { mutableFloatStateOf(0f) }

    Column (
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start

    ){
        Spacer(modifier =Modifier.height(100.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            Column {


                Text(text = "Uruchom wiatrak")
            }
              Column {
                  Switch(
                      checked = fan,
                      onCheckedChange = {
                          fan = it
                      }
                  )
              }
        }
        Row( modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "Uruchom piec")

            }

            Column {
                Switch(
                    checked = furnace,
                    onCheckedChange = {
                        furnace = it
                    }
                )
            }
        }
        Row( modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "Grzejnik 1")

            }

            Column {
                Column {
                    Slider(
                        value = heater1,
                        onValueChange = { heater1 = it },
                        steps = 6,
                        valueRange = 0f..5f,
                        modifier = Modifier.width(200.dp)
                    )
                    Text(text = "%.0f".format(heater1))
                }
            }
        }

        Row( modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "Grzejnik 2")

            }

            Column {
                Column {
                    Slider(
                        value = heater2,
                        onValueChange = { heater2 = it },
                        steps = 6,
                        valueRange = 0f..5f,
                        modifier = Modifier.width(200.dp)
                    )
                    Text(text = "%.0f".format(heater2))
                }
            }
        }
        Row( modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "Grzejnik 3")

            }

            Column {
                Column {
                    Slider(
                        value = heater3,
                        onValueChange = { heater3 = it },
                        steps = 6,
                        valueRange = 0f..5f,
                        modifier = Modifier.width(200.dp)
                    )
                    Text(text = "%.0f".format(heater3))
                }            }
        }
        Row( modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = { /*TODO*/ }) {
                Text(text = "Wyślij do kontrolera")
            }
        }



        



    }
}
