package com.example.miapp_01.ui.theme


import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlin.random.Random

class SensorRepository {

    fun temperaturaFlow(): Flow<Double> = flow {
        var ultimaLectura: Double? = null

        while (true) {
            // Se conserva el error aleatorio del 5 %
            if (Random.nextDouble() < 0.05) {
                throw IllegalStateException(
                    "Fallo de comunicación con el sensor"
                )
            }

            var lectura: Double

            do {
                lectura = String.format(
                    java.util.Locale.US,
                    "%.1f",
                    20.0 + Random.nextDouble() * 20.0
                ).toDouble()
            } while (lectura == ultimaLectura)

            ultimaLectura = lectura

            emit(lectura)
            delay(1000)
        }
    }.flowOn(Dispatchers.Default)


    // Actividad 4: segundo sensor de humedad
    fun humedadFlow(): Flow<Double> = flow {
        while (true) {
            val humedad = 30.0 + Random.nextDouble() * 40.0
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
