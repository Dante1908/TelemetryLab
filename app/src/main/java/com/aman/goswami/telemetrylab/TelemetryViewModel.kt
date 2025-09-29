package com.aman.goswami.telemetrylab

import android.app.Application
import android.content.Context
import android.os.BatteryManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class TelemetryState(
    val isRunning: Boolean = false,
    val computeLoad: Int = 2,
    val currentLatency: Long = 0L,
    val averageLatency: Double = 0.0,
    val jankPercentage: Double = 0.0,
    val jankFrameCount: Int = 0,
    val frameCounter: Int = 0,
    val isPowerSaveMode: Boolean = false,
    val latencyHistory: List<Long> = emptyList()
)

class TelemetryViewModel(private val application: Application) : ViewModel() {

    private val _uiState = MutableStateFlow(TelemetryState())
    val uiState: StateFlow<TelemetryState> = _uiState.asStateFlow()

    private var computationJob: Job? = null
    private var frameCount = 0
    private val maxHistorySize = 100
    private val jankThresholdMs = 16L

    private val convolutionKernel = arrayOf(
        floatArrayOf(1f, 0f, -1f),
        floatArrayOf(2f, 0f, -2f),
        floatArrayOf(1f, 0f, -1f)
    )

    fun startComputation() {
        if (_uiState.value.isRunning) return

        _uiState.update { it.copy(isRunning = true) }

        computationJob = viewModelScope.launch {
            TelemetryForegroundService.startService(application, _uiState.value.computeLoad)

            var totalLatency = 0.0
            var jankCount = 0
            val frameTimes = mutableListOf<Long>()

            while (isActive) {
                val startTime = System.currentTimeMillis()

                val isPowerSave = checkBatterySaverMode()
                _uiState.update { it.copy(isPowerSaveMode = isPowerSave) }

                val frequency = if (isPowerSave) 10 else 20
                val effectiveComputeLoad = if (isPowerSave) {
                    maxOf(1, _uiState.value.computeLoad - 1)
                } else {
                    _uiState.value.computeLoad
                }

                performConvolutionWork(effectiveComputeLoad)

                val endTime = System.currentTimeMillis()
                val frameTime = endTime - startTime

                frameTimes.add(frameTime)
                if (frameTimes.size > maxHistorySize) {
                    frameTimes.removeAt(0)
                }

                totalLatency += frameTime
                val averageLatency = totalLatency / (frameCount + 1)

                if (frameTime > jankThresholdMs) {
                    jankCount++
                }

                val jankPercentage = if (frameCount > 0) {
                    (jankCount.toDouble() / frameCount) * 100.0
                } else {
                    0.0
                }
                if (frameCount % 5 == 0) {
                    _uiState.update { state ->
                        state.copy(
                            currentLatency = frameTime,
                            averageLatency = averageLatency,
                            jankPercentage = jankPercentage,
                            jankFrameCount = jankCount,
                            frameCounter = frameCount,
                            latencyHistory = frameTimes.toList()
                        )
                    }
                }

                frameCount++

                val targetFrameTime = 1000L / frequency
                val sleepTime = maxOf(0L, targetFrameTime - frameTime)
                delay(sleepTime)
            }
        }
    }

    fun stopComputation() {
        computationJob?.cancel()
        computationJob = null
        _uiState.update { it.copy(isRunning = false) }
        TelemetryForegroundService.stopService(application)
    }

    fun setComputeLoad(load: Int) {
        _uiState.update { it.copy(computeLoad = load.coerceIn(1, 5)) }
    }

    private fun performConvolutionWork(passes: Int) {
        val size = 256
        var array = FloatArray(size * size) { (it % 256).toFloat() }

        repeat(passes) {
            array = applyConvolution(array, size, size)
        }
    }

    private fun applyConvolution(input: FloatArray, width: Int, height: Int): FloatArray {
        val output = FloatArray(width * height)
        val kernelSize = 3
        val kernelOffset = kernelSize / 2

        for (y in 0 until height) {
            for (x in 0 until width) {
                var sum = 0f

                for (ky in 0 until kernelSize) {
                    for (kx in 0 until kernelSize) {
                        val posY = y + ky - kernelOffset
                        val posX = x + kx - kernelOffset

                        val clampedY = posY.coerceIn(0, height - 1)
                        val clampedX = posX.coerceIn(0, width - 1)

                        val inputValue = input[clampedY * width + clampedX]
                        val kernelValue = convolutionKernel[ky][kx]
                        sum += inputValue * kernelValue
                    }
                }

                output[y * width + x] = sum
            }
        }

        return output
    }

    private fun checkBatterySaverMode(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            val powerManager = application.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            powerManager.isPowerSaveMode
        } else {
            false
        }
    }

    fun cleanup() {
        computationJob?.cancel()
        TelemetryForegroundService.stopService(application)
    }
}