package com.example.miapp_01.ui.theme

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlin.random.Random

class SensorRepository {
    fun temperaturaFlow(): Flow<Double> = flow {
        while (true) {
            if (Random.nextDouble() < 0.05) { // 5% de probabilidad de error
                throw IllegalStateException("Fallo de comunicación con el sensor")
            }
            val lectura = 20.0 + Random.nextDouble() * 20.0 // 20-40°C
            emit(lectura)
            delay(1000)
        }
    }.flowOn(Dispatchers.Default) // actividad 5: Ejecución en hilo secundario

    // actividad 4: Segundo sensor de Humedad
    fun humedadFlow(): Flow<Double> = flow {
        while (true) {
            val humedad = 30.0 + Random.nextDouble() * 40.0 // 30-70%
            emit(humedad)
            delay(1000)
        }
    }.flowOn(Dispatchers.Default)

    // Cuenta regresiva
    fun cuentaRegresiva(desde: Int): Flow<Int> = flow {
        for (i in desde downTo 0) {
            emit(i)
            delay(1000)
        }
    }
}