package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.BrowserScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.BrowserViewModel

class MainActivity : ComponentActivity() {

    private var currentViewModel: BrowserViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val browserViewModel: BrowserViewModel = viewModel()
            currentViewModel = browserViewModel

            val isAmoled by browserViewModel.repository.isAmoledTheme.collectAsState()
            val isNightMode by browserViewModel.repository.isNightModeEnabled.collectAsState()
            val systemDark = isSystemInDarkTheme()
            val effectiveDark = isNightMode || systemDark

            // Handle incoming VIEW intent if app was opened with a URL
            LaunchedEffect(intent) {
                handleIncomingIntent(intent, browserViewModel)
            }

            MyApplicationTheme(
                darkTheme = effectiveDark,
                isAmoled = isAmoled
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    BrowserScreen(viewModel = browserViewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        currentViewModel?.let { handleIncomingIntent(intent, it) }
    }

    private fun handleIncomingIntent(intent: Intent?, viewModel: BrowserViewModel) {
        if (intent?.action == Intent.ACTION_VIEW) {
            val data = intent.dataString
            if (!data.isNullOrBlank()) {
                viewModel.createNewTab(url = data)
            }
        }
    }
}
