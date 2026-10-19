package com.example.miapp_01.ui.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

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
        if (jobSensor?.isActive == true) return
        _uiState.update { it.copy(monitoreando = true) }

        jobSensor = viewModelScope.launch {
            // actividad 4 y 7: Combinar Flujos de Temperatura, Humedad y Umbral
            combine(
                repo.temperaturaFlow(),
                repo.humedadFlow(),
                umbralAlerta
            ) { temp, hum, umbral ->
                Triple(temp, hum, umbral)
            }
                //actividad 6: Hasta 3 reintentos
                .retry(3) { e -> e is IllegalStateException }
                .map { (temp, hum, umbral) ->
                    Triple("%.1f".format(temp).toDouble(), "%.1f".format(hum).toDouble(), umbral)
                }
                // actividad 2: Filtrar lecturas mayores a 25 °C
                .filter { (temp, _, _) -> temp > 25.0 }
                // actividad 2: Evitar valores repetidos consecutivos
                .distinctUntilChanged()
                // actividad 6: Captura de errores tras agotar reintentos
                .catch { e ->
                    _alertas.emit("Error crítico: ${e.message}")
                    _uiState.update { it.copy(monitoreando = false) }
                }
                .collect { (temp, hum, umbral) ->
                    // actividad 7: evaluación de alertas según umbral dinámico
                    if (temp > umbral) {
                        _alertas.emit("¡Alerta! Temp: $temp °C superó el umbral de $umbral °C")
                    }
                    _uiState.update { estado ->
                        val nuevas = (estado.lecturas + temp).takeLast(10)
                        estado.copy(
                            actual = temp,
                            humedad = hum,
                            lecturas = nuevas,
                            promedio = nuevas.average()
                        )
                    }
                }
        }
    }

    // actividad 1: Detener monitoreo cancelando el Job
    fun detener() {
        jobSensor?.cancel()
        _uiState.update { it.copy(monitoreando = false) }
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