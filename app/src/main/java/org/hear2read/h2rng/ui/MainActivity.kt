// ==================== MainActivity.kt ====================
package org.hear2read.h2rng.ui

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.google.android.play.core.assetpacks.AssetPackManager
import com.google.android.play.core.assetpacks.AssetPackManagerFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.hear2read.h2rng.Hear2ReadNGApplication

/**
 * Main activity containing the UI part of the app along with the voice manager. Initializes an
 * instance of [TextToSpeech] with the Hear2ReadNG engine to be used throughout to demo the TTS.
 * Shows a splashscreen until the [TextToSpeech] is initialized.
 *
 */
class MainActivity : ComponentActivity() {

    val LOG_TAG = "H2RNG_" + MainActivity::class.simpleName
    /**
     * tts: The [TextToSpeech] that is used throughout all the screens in the activity
     * assetPackManager The AssetPackManager instance that is used throughout the activity to
     * access and download voices
     */
    private var tts: TextToSpeech? = null
    private lateinit var assetPackManager: AssetPackManager
    private var isTTSReady = false
//    private var isRepoSynced = false

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen and keep it on screen while TTS initializes
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)

        // Keep the splash screen visible until TTS is ready
        // TODO: this doesn't stop the setContent so that is delayed in a slightly hacky fashion
        splashScreen.setKeepOnScreenCondition { !(isTTSReady /*&& isRepoSynced*/)}

        // Initialize AssetPackManager
        assetPackManager = AssetPackManagerFactory.getInstance(this)

        val app = application as Hear2ReadNGApplication

        lifecycleScope.launch {
            /*
             Haven't found a way to do this in a cleaner way, and not having the repository
             initialized means that the TTS won't return the correct available voices
             */
            while (!app.isVoiceRepositoryReady()) {
                delay(50)
            }
            // Initialize TTS
            initializeBasicTTS()
//            syncDownloadStates()

            while(!isTTSReady){
                delay(100)
            }

            setContent {
                val navController = rememberNavController()
                AppNavigation(
                    navController = navController,
                    getTTS = { tts },
                    setTTS = { tts = it },
                    assetPackManager = assetPackManager
                )
            }
        }
    }

    /**
     * Initialize base [TextToSpeech] instance. The language isn't set in this stage and will only
     * be done once the [TTSDemoScreen] is shown.
     *
     */
    private fun initializeBasicTTS() {
        Log.d(LOG_TAG, "initializeBasicTTS()")
//        runBlocking { populateSizes(assetPackManager) }
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                // TTS initialized successfully
                Log.d(LOG_TAG, "TTS initialized successfully")
                Log.d(LOG_TAG, "Available languages: ${tts?.availableLanguages}")
                isTTSReady = true
            } else {
                // Handle initialization failure
                Log.e(LOG_TAG, "TTS initialization failure!")
                isTTSReady = true // TODO show error
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        tts?.stop()
        tts?.shutdown()
    }
}
