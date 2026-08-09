package com.unmark.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.unmark.app.ui.BatchScreen
import com.unmark.app.ui.EditorScreen
import com.unmark.app.ui.theme.UnmarkTheme

private enum class Screen { EDITOR, BATCH }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            UnmarkTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    UnmarkApp()
                }
            }
        }
    }
}

@Composable
private fun UnmarkApp() {
    var screen by remember { mutableStateOf(Screen.EDITOR) }
    when (screen) {
        Screen.EDITOR -> EditorScreen(onOpenBatch = { screen = Screen.BATCH })
        Screen.BATCH -> BatchScreen(onBack = { screen = Screen.EDITOR })
    }
}
