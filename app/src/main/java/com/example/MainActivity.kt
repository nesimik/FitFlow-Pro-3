package com.example

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.AppViewModel
import com.example.ui.FitFlowApp
import com.example.ui.theme.FitFlowTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: AppViewModel = viewModel()

            val themeMode by vm.settings.themeMode.collectAsStateWithLifecycle()
            val accent by vm.settings.accent.collectAsStateWithLifecycle()
            val amoled by vm.settings.amoled.collectAsStateWithLifecycle()
            val fontScale by vm.settings.fontScale.collectAsStateWithLifecycle()
            val keepScreenOn by vm.settings.keepScreenOn.collectAsStateWithLifecycle()
            val activeWorkout by vm.activeWorkout.collectAsStateWithLifecycle()

            if (keepScreenOn && activeWorkout != null) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }

            FitFlowTheme(
                themeMode = themeMode,
                accentHex = accent,
                amoled = amoled,
                fontScale = fontScale
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    FitFlowApp(vm)
                }
            }
        }
    }
}
