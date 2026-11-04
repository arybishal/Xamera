package com.bishal.adaptivecamera

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.bishal.adaptivecamera.ui.diagnostics.DiagnosticsScreen
import com.bishal.adaptivecamera.ui.diagnostics.DiagnosticsViewModel
import com.bishal.adaptivecamera.ui.theme.AdaptiveCameraTheme

class MainActivity : ComponentActivity() {

    private val diagnosticsViewModel: DiagnosticsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AdaptiveCameraTheme {
                DiagnosticsScreen(viewModel = diagnosticsViewModel)
            }
        }
    }
}
