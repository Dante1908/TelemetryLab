package com.aman.goswami.telemetrylab

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelemetryScreen(viewModel: TelemetryViewModel) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value

    Scaffold(topBar = { TopAppBar(title = { Text("Telemetry Lab") }) }) { paddingValues ->
        Surface(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (uiState.isPowerSaveMode) {
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                        Text(text = "POWER-SAVE MODE", modifier = Modifier.fillMaxWidth().padding(8.dp), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
                ControlSection(isRunning = uiState.isRunning, onStart = { viewModel.startComputation() }, onStop = { viewModel.stopComputation() })
                ComputeLoadSection(computeLoad = uiState.computeLoad, onComputeLoadChange = { viewModel.setComputeLoad(it) }, isEnabled = !uiState.isRunning)
                PerformanceDashboard(uiState = uiState)
                AnimatedCounter(uiState.frameCounter)
            }
        }
    }
}

@Composable
private fun ControlSection(
    isRunning: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "Computation Control", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Button(onClick = onStart, enabled = !isRunning){
                    Text("Start")
                }
                Button(onClick = onStop, enabled = isRunning){
                    Text("Stop")
                }
            }
            Text(text = if (isRunning) "Status: RUNNING" else "Status: STOPPED", color = if (isRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ComputeLoadSection(
    computeLoad: Int,
    onComputeLoadChange: (Int) -> Unit,
    isEnabled: Boolean
) {
    Card(modifier = Modifier.fillMaxWidth()){
        Column(modifier = Modifier.padding(16.dp)){
            Text(text = "Compute Load: $computeLoad", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Slider(value = computeLoad.toFloat(), onValueChange = { onComputeLoadChange(it.toInt()) }, valueRange = 1f..5f, steps = 3, enabled = isEnabled)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("1 (Low)")
                Text("3 (Medium)")
                Text("5 (High)")
            }
        }
    }
}

@Composable
private fun PerformanceDashboard(uiState: TelemetryState) {
    Card(modifier = Modifier.fillMaxWidth()){
        Column(modifier = Modifier.padding(16.dp)){
            Text(text = "Performance Dashboard", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            MetricRow(label = "Current Frame Latency:", value = "${uiState.currentLatency} ms")
            MetricRow(label = "Moving Average:", value = "%.2f ms".format(uiState.averageLatency))
            MetricRow(label = "Jank % (last 30s):", value = "%.1f%%".format(uiState.jankPercentage))
            MetricRow(label = "Jank Frames:", value = uiState.jankFrameCount.toString())
            Spacer(modifier = Modifier.height(8.dp))
            Text("Jank Level:")
            LinearProgressIndicator(progress = { (uiState.jankPercentage / 100f).coerceIn(0.0, 1.0).toFloat() }, modifier = Modifier.fillMaxWidth().height(8.dp))
        }
    }
}

@Composable
private fun MetricRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween){
        Text(label)
        Text(value, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun AnimatedCounter(frameCount: Int) {
    Card(modifier = Modifier.fillMaxWidth()){
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally){
            Text(text = "Frame Counter", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = frameCount.toString(), fontSize = 32.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

            Text("Frames Processed")
        }
    }
}