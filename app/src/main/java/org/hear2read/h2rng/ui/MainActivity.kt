// ==================== MainActivity.kt ====================
package org.hear2read.h2rng.ui

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.hear2read.h2rng.BuildConfig
import org.hear2read.h2rng.Hear2ReadNGApplication
import org.hear2read.h2rng.data.H2RVoiceRepository
import org.hear2read.h2rng.tts.TTS_NAME
import org.hear2read.h2rng.utils.LOG_PREFIX
import kotlin.coroutines.resume

/**
 * Main activity containing the UI part of the app along with the voice manager.
 *
 */
class MainActivity : ComponentActivity() {

    val LOG_TAG = LOG_PREFIX + MainActivity::class.simpleName
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen and keep it on screen while TTS initializes
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)

        // Keep the splash screen visible until TTS is ready
        splashScreen.setKeepOnScreenCondition { !(viewModel.isAppReady.value)}

        enableEdgeToEdge()

        setContent {
            val isAppReady by viewModel.isAppReady.collectAsStateWithLifecycle()

            // ONLY instantiate AppNavigation and screens AFTER the TTS is initialized and this
            // boolean becomes true
            if (isAppReady) {
                val navController = rememberNavController()
                AppNavigation(
                    navController = navController,
                    mainViewModel = viewModel
                )
            } else {
                // While waiting, render a completely empty Box. User will never see this empty Box
                // because Splash Screen is drawn over entire window
                Box(modifier = Modifier.fillMaxSize())
            }
        }
    }
}
