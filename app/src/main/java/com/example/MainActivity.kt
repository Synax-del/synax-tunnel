package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.AppThemeMode
import com.example.ui.CipherTunnelApp
import com.example.ui.MainViewModel
import com.example.ui.theme.CipherTunnelTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        setContent {
            val viewModel: MainViewModel = viewModel()
            val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()
            val darkTheme = when (appSettings.themeMode) {
                AppThemeMode.DAY -> false
                AppThemeMode.NIGHT -> true
                AppThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            CipherTunnelTheme(darkTheme = darkTheme) {
                CipherTunnelApp(viewModel = viewModel)
            }
        }
    }
}
