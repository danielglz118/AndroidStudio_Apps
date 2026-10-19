package com.example.miapp_01

import android.R.attr.button
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImage

import com.example.miapp_01.ui.theme.MiAPP_01Theme
import com.example.miapp_01.ui.theme.SensorViewModel

class MainActivity : ComponentActivity() {
        private val vm: SensorViewModel by viewModels()
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContent {
                MiAPP_01Theme {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        Pantalla(vm)
                    }
                }
            }
        }
    }

    @Composable
    fun Pantalla(vm: SensorViewModel) {
        val estado by vm.uiState.collectAsStateWithLifecycle()
        val contador by vm.contador.collectAsStateWithLifecycle()
        val umbral by vm.umbralAlerta.collectAsStateWithLifecycle()
        val snackbar = remember { SnackbarHostState() }

        LaunchedEffect(Unit) {
            vm.alertas.collect { snackbar.showSnackbar(it) }
        }

        Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Temperatura actual: ${estado.actual} °C",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    "Humedad actual: ${estado.humedad} %",
                    style = MaterialTheme.typography.titleMedium
                )
                Text("Promedio (últimas 10 > 25°C): ${"%.1f".format(estado.promedio)} °C")
                Text("Lecturas: ${estado.lecturas.joinToString()}")

                HorizontalDivider()

                Text("Umbral de Alerta: ${"%.1f".format(umbral)} °C")
                Slider(
                    value = umbral.toFloat(),
                    onValueChange = { vm.actualizarUmbral(it.toDouble()) },
                    valueRange = 20f..40f
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = vm::iniciar, enabled = !estado.monitoreando) {
                        Text("Iniciar")
                    }
                    Button(onClick = vm::detener, enabled = estado.monitoreando) {
                        Text("Detener")
                    }
                }

                HorizontalDivider()

                Button(onClick = { vm.iniciarContador(10) }) {
                    Text("Cuenta regresiva 10s")
                }
                contador?.let {
                    Text("Faltan: $it s", style = MaterialTheme.typography.titleLarge)
                }
            }
        }
    }