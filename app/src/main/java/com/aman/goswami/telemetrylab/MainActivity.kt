package com.example.telemetrylab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.aman.goswami.telemetrylab.TelemetryScreen
import com.aman.goswami.telemetrylab.TelemetryViewModel
import com.example.telemetrylab.ui.theme.TelemetryLabTheme
import kotlin.getValue

class MainActivity : ComponentActivity() {

    private val viewModel: TelemetryViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return TelemetryViewModel(application) as T
            }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TelemetryLabTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background){
                    TelemetryScreen(viewModel = viewModel)
                }
            }
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        viewModel.cleanup()
    }
}