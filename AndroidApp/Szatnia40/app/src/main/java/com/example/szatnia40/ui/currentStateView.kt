package com.example.szatnia40.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun currentStateView(){
    var roomTemp = 15f;
    var furnaceTemp = 100f;
    var hallTemp=20f;
    var fan = true;
    var heater1=0;
    var heater2=3;
    var heater3=5;
    var furnace=true
    Column (
        modifier =  Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally

    ){
        Spacer(modifier =Modifier.height(100.dp))
        Row( modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {

                Text(text = "Aktualny stan układu", fontSize = 20.sp,fontWeight = FontWeight.Bold )

            }
        }
        Row( modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "temperatura pomieszczenia:")

            }

            Column {
                Text(text = " %.1f \u2103".format(roomTemp))
            }
        }
        Row( modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "temperatura hali:")

            }

            Column {
                Text(text = " %.1f \u2103".format(hallTemp))
            }
        }
        Row( modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "temperatura pieca:")

            }

            Column {
                Text(text = " %.1f \u2103".format(furnaceTemp))
            }
        }


        Row( modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "Piec: ")
            }
            Column {
               if (furnace){
                   Text(text = "Aktywny", color=Color.Green)
               }else{
                   Text(text = "Niaktywny", color = Color.Red)

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
                Text(text = "Grzejnik 1: ")
            }
            Column {

                Text(text = heater1.toString())


            }


        }

        Row( modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "Grzejnik 2: ")
            }
            Column {
                Text(text = heater2.toString())

            }


        }
        Row( modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "Grzejnik 3: ")
            }
            Column {
                Text(text = heater3.toString())

            }


        }
        Row( modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "Wiatrak: ")
            }
            Column {
                if (fan){
                    Text(text = "Aktywny", color=Color.Green)
                }else{
                    Text(text = "Niaktywny",color=Color.Red)

                }

            }


        }






    }


}