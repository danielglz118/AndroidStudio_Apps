package com.example.miapp_01.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Label
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun mainTextFiekd(value:String,onValueChange:(String)-> Unit,label: String){
    OutlinedTextField(
    value = value,
        onValueChange = onValueChange,
        label = {Text(Label)},
        keyboardOptions = KeyboardOptions(KeyboardType.Number),
    modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 30.dp)
    )

}

