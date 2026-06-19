package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.CameraView
import com.example.ui.MainHubView
import com.example.ui.AraknoViewModel
import com.example.ui.AuthState
import com.example.ui.AuthScreen
import com.example.ui.theme.AraknoTheme

class MainActivity : ComponentActivity() {

    // ViewModel retrieval from Application class safely
    private val viewModel: AraknoViewModel by viewModels {
        AraknoViewModel.Factory((application as AraknoApplication).repository)
    }

    @OptIn(ExperimentalAnimationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AraknoTheme(darkTheme = true, dynamicColor = false) {
                val authState by viewModel.authState.collectAsStateWithLifecycle()
                val context = LocalContext.current
                var currentScreen by remember { mutableStateOf("hub") } // "hub" or "camera"

                if (authState is AuthState.Loading) {
                    // Show a splash/loading state while session is being checked
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.background
                    ) { innerPadding ->
                        Box(modifier = Modifier.padding(innerPadding)) {
                            if (authState !is AuthState.Authenticated) {
                                AuthScreen(
                                    state = authState,
                                    onLogin = { e, p -> viewModel.login(e, p, context) },
                                    onRegister = { e, p, n -> viewModel.register(e, p, n, context) },
                                    onReset = { viewModel.resetAuthState() }
                                )
                            } else {
                                // Animated transition between main hub dashboard and the scanner camera
                                AnimatedContent(
                                    targetState = currentScreen,
                                    transitionSpec = {
                                        if (targetState == "camera") {
                                            slideInHorizontally(animationSpec = tween(350), initialOffsetX = { it }) togetherWith
                                                    slideOutHorizontally(animationSpec = tween(350), targetOffsetX = { -it })
                                        } else {
                                            slideInHorizontally(animationSpec = tween(350), initialOffsetX = { -it }) togetherWith
                                                    slideOutHorizontally(animationSpec = tween(350), targetOffsetX = { it })
                                        }
                                    },
                                    label = "screen_transitions"
                                ) { screen ->
                                    when (screen) {
                                        "hub" -> MainHubView(
                                            viewModel = viewModel,
                                            onNavigateToScan = { currentScreen = "camera" },
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        "camera" -> CameraView(
                                            viewModel = viewModel,
                                            onResultSelected = { currentScreen = "hub" },
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
