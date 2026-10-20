package com.example.miapp_01.ui.theme


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

data class SensorUiState(
    val actual: Double = 0.0,
    val humedad: Double = 0.0,
    val promedio: Double = 0.0,
    val lecturas: List<Double> = emptyList(),
    val monitoreando: Boolean = false
)

class SensorViewModel(
    private val repo: SensorRepository = SensorRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(SensorUiState())
    val uiState: StateFlow<SensorUiState> = _uiState.asStateFlow()

    // Solo conserva la alerta más reciente para que no se acumulen
    private val _alertas = MutableSharedFlow<String>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val alertas: SharedFlow<String> = _alertas.asSharedFlow()

    private val _contador = MutableStateFlow<Int?>(null)
    val contador: StateFlow<Int?> = _contador.asStateFlow()

    // Actividad 7: umbral configurable
    val umbralAlerta = MutableStateFlow(30.0)

    // Actividad 3: Flow frío convertido en StateFlow caliente
    val temperaturaCalienteState: StateFlow<Double> =
        repo.temperaturaFlow()
            .retry(3) { error ->
                error is IllegalStateException
            }
            .catch { error ->
                if (error is CancellationException) throw error
                emit(0.0)
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = 0.0
            )

    private var jobSensor: Job? = null
    private var jobContador: Job? = null

    private fun redondear(valor: Double): Double =
        String.format(Locale.US, "%.1f", valor).toDouble()

    fun iniciar() {
        if (jobSensor?.isActive == true) return

        _uiState.update { it.copy(monitoreando = true) }

        jobSensor = viewModelScope.launch {
            try {
                while (isActive) {
                    var ocurrioError = false

                    try {
                        // Actividad 4 y 7: combine de temperatura, humedad y umbral
                        combine(
                            repo.temperaturaFlow(),
                            repo.humedadFlow(),
                            umbralAlerta
                        ) { temp, hum, umbral ->
                            Triple(temp, hum, umbral)
                        }
                            // Actividad 6: retry(3) + catch
                            .retry(3) { error ->
                                error is IllegalStateException
                            }
                            .map { (temp, hum, umbral) ->
                                Triple(redondear(temp), redondear(hum), umbral)
                            }
                            // Actividad 2: filter
                            .filter { (temp, _, _) -> temp > 25.0 }
                            // Actividad 2: distinctUntilChanged comparando SOLO la temperatura.
                            // Evita que la misma lectura se procese dos veces cuando
                            // combine vuelve a emitir por un cambio de humedad.
                            .distinctUntilChanged { anterior, nuevo ->
                                anterior.first == nuevo.first
                            }
                            .catch { error ->
                                if (error is CancellationException) {
                                    throw error
                                }

                                ocurrioError = true

                                _alertas.emit(
                                    "Error del sensor: ${error.message}. " +
                                            "Se agotaron los 3 reintentos. " +
                                            "Se reiniciará el monitoreo."
                                )
                            }
                            .collect { (temp, hum, umbral) ->

                                if (temp > umbral) {
                                    android.util.Log.d("SENSOR", "temp=$temp umbral=$umbral -> alerta")
                                    _alertas.emit("¡Alerta! Temp: $temp °C superó el umbral de $umbral °C")
                                }

                                _uiState.update { estado ->
                                    val nuevas =
                                        (estado.lecturas + temp).takeLast(10)

                                    estado.copy(
                                        actual = temp,
                                        humedad = hum,
                                        lecturas = nuevas,
                                        promedio = nuevas.average()
                                    )
                                }
                            }

                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        ocurrioError = true

                        _alertas.emit(
                            "Error inesperado: ${e.message}. " +
                                    "Se intentará reiniciar el monitoreo."
                        )
                    }

                    if (ocurrioError && isActive) {
                        delay(1000)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } finally {
                _uiState.update { it.copy(monitoreando = false) }
            }
        }
    }

    // Actividad 1: detener el monitoreo cancelando el Job
    fun detener() {
        jobSensor?.cancel()
        jobSensor = null

        // Limpiar el mensaje de alerta mostrado en pantalla
        _alertas.tryEmit("")

        _uiState.update { it.copy(monitoreando = false) }
    }

    fun actualizarUmbral(nuevoUmbral: Double) {
        umbralAlerta.value = nuevoUmbral
    }

    fun iniciarContador(segundos: Int = 10) {
        jobContador?.cancel()

        jobContador = viewModelScope.launch {
            try {
                repo.cuentaRegresiva(segundos)
                    .catch { error ->
                        if (error is CancellationException) throw error
                        _alertas.emit(
                            "Error en la cuenta regresiva: ${error.message}"
                        )
                    }
                    .collect { _contador.value = it }
            } finally {
                _contador.value = null
            }
        }
    }
}