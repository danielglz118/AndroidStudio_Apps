package com.example.miapp_01.ui.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

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
    private val _alertas = MutableSharedFlow<String>()
    val alertas: SharedFlow<String> = _alertas.asSharedFlow()
    private val _contador = MutableStateFlow<Int?>(null)
    val contador: StateFlow<Int?> = _contador.asStateFlow()
    //actividad 7: Umbral configurable con Slider
    val umbralAlerta = MutableStateFlow(30.0)

    // actividad 3: ejemplo de Flow frío convertido a StateFlow caliente con stateIn
    val temperaturaCalienteState: StateFlow<Double> = repo.temperaturaFlow()
        .retry(3)
        .catch { emit(0.0) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.0
        )

    private var jobSensor: Job? = null
    private var jobContador: Job? = null

    fun iniciar() {
        fun iniciar() {
            if (jobSensor?.isActive == true) return

            _uiState.update {
                it.copy(monitoreando = true)
            }

            jobSensor = viewModelScope.launch {
                while (isActive) {
                    try {
                        combine(
                            repo.temperaturaFlow(),
                            repo.humedadFlow(),
                            umbralAlerta
                        ) { temp, hum, umbral ->
                            Triple(temp, hum, umbral)
                        }
                            .retry(3) { error ->
                                error is IllegalStateException
                            }
                            .catch { error ->
                                _alertas.emit(
                                    "Error del sensor: ${error.message}. " +
                                            "Se volverá a intentar."
                                )
                            }
                            .collect { (temp, hum, umbral) ->
                                if (temp > umbral) {
                                    _alertas.emit(
                                        "¡Alerta! Temp: $temp °C " +
                                                "superó el umbral de $umbral °C"
                                    )
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
                    }

                    delay(1000)
                }
            }
        }
    }

    // actividad 1: Detener monitoreo cancelando el Job


    fun detener() {
        jobSensor?.cancel()
        jobSensor = null

        _uiState.update {
            it.copy(monitoreando = false)
        }
    }


    fun actualizarUmbral(nuevoUmbral: Double) {
        umbralAlerta.value = nuevoUmbral
    }

    fun iniciarContador(segundos: Int = 10) {
        jobContador?.cancel()
        jobContador = viewModelScope.launch {
            repo.cuentaRegresiva(segundos)
                .onCompletion { _contador.value = null }
                .collect { _contador.value = it }
        }
    }
}