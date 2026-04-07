package org.hear2read.h2rng.ui

import android.speech.tts.TextToSpeech
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import org.hear2read.h2rng.ui.screens.LanguageSelectionScreen
import org.hear2read.h2rng.ui.screens.TTSDemoScreen
import java.util.Locale

/**
 * App navigation composable.for general UI navigation in "local" flavour. Does not have the
 * VoiceManagerScreen
 *
 * @param navController The [NavHostController] instance used
 * @param getTTS Function to access the single TTS instance created in the [MainActivity]
 * @param setTTS Function to set the single TTS instance
 * @param assetPackManager The [AssetPackManager] instance created in the MainActivity for managing
 * the voice download and access
 */
@Composable
fun AppNavigation(
    navController: NavHostController,
    getTTS: () -> TextToSpeech?,
    setTTS: (TextToSpeech?) -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = "language_selection"
    ) {
        composable("language_selection") {
            LanguageSelectionScreen(
                navController = navController,
                getTTS = getTTS,
                onLanguageSelected = { langLocale ->
                    navController.navigate("tts_demo/${langLocale.language}")
                },
                onInstallVoiceClick = {
                    // do nothing
//                    navController.navigate("install_voices")
                }
            )
        }

        composable("tts_demo/{languageCode}") { backStackEntry ->
            val languageCode = backStackEntry.arguments?.getString("languageCode") ?: "en"
            TTSDemoScreen(
                langLocale = Locale.Builder().setLanguage(languageCode).build(),
                navController = navController,
                getTTS = getTTS,
                setTTS = setTTS
            )
        }
    }
}