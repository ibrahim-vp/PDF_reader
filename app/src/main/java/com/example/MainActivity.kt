package com.example

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.ui.PdfViewModel
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.PdfViewerScreen
import com.example.ui.theme.MyApplicationTheme

enum class ScreenState {
    HOME,
    VIEWER
}

class MainActivity : ComponentActivity() {

    private val viewModel: PdfViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle intent when app is launched from WhatsApp or file manager
        handleIncomingIntent(intent)

        setContent {
            val isDarkMode by viewModel.isDarkMode.collectAsState()
            val systemDark = isSystemInDarkTheme()
            val effectiveDark = isDarkMode ?: systemDark

            MyApplicationTheme(darkTheme = effectiveDark) {
                val currentDoc by viewModel.currentDoc.collectAsState()
                val errorMessage by viewModel.errorMessage.collectAsState()
                val snackbarHostState = remember { SnackbarHostState() }

                var screenState by remember { mutableStateOf(ScreenState.HOME) }

                LaunchedEffect(currentDoc) {
                    if (currentDoc != null) {
                        screenState = ScreenState.VIEWER
                    }
                }

                LaunchedEffect(errorMessage) {
                    if (errorMessage != null) {
                        snackbarHostState.showSnackbar(errorMessage!!)
                        viewModel.clearError()
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = { SnackbarHost(snackbarHostState) }
                ) { innerPadding ->
                    Crossfade(
                        targetState = screenState,
                        modifier = Modifier.padding(innerPadding),
                        label = "ScreenTransition"
                    ) { target ->
                        when (target) {
                            ScreenState.HOME -> {
                                HomeScreen(
                                    viewModel = viewModel,
                                    onOpenPdf = {
                                        screenState = ScreenState.VIEWER
                                    }
                                )
                            }
                            ScreenState.VIEWER -> {
                                PdfViewerScreen(
                                    viewModel = viewModel,
                                    onBackClick = {
                                        viewModel.closeDocument()
                                        screenState = ScreenState.HOME
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    /**
     * Extracts PDF Uri from ACTION_VIEW or ACTION_SEND (e.g. from WhatsApp)
     */
    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return
        val action = intent.action
        var targetUri: Uri? = null

        if (Intent.ACTION_VIEW == action) {
            targetUri = intent.data
        } else if (Intent.ACTION_SEND == action) {
            @Suppress("DEPRECATION")
            targetUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                intent.getParcelableExtra(Intent.EXTRA_STREAM)
            }
        }

        if (targetUri != null) {
            viewModel.loadFromUri(targetUri)
        }
    }
}
