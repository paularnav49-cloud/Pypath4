package com.pypath.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.pypath.app.ui.AppState
import com.pypath.app.ui.AppViewModel
import com.pypath.app.ui.navigation.PyPathNavHost
import com.pypath.app.ui.theme.PyPathTheme

class MainActivity : ComponentActivity() {

    private val appViewModel: AppViewModel by viewModels { AppViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        // Keep the splash until saved progress is loaded, so we know whether to show onboarding.
        splash.setKeepOnScreenCondition { appViewModel.state.value is AppState.Loading }
        enableEdgeToEdge()
        setContent {
            PyPathTheme {
                PyPathNavHost(appViewModel)
            }
        }
    }
}
