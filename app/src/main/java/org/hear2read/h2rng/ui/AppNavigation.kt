package org.hear2read.h2rng.ui

import addFlavorSpecificScreens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import org.hear2read.h2rng.tts.getSampleTextID
import org.hear2read.h2rng.ui.screens.LanguageSelectionScreen
import org.hear2read.h2rng.ui.screens.TTSDemoScreen
import java.util.Locale

/**
 * App navigation composable.for general UI navigation in the PAD related flavors
 *
 * @param navController The [NavHostController] instance used
 * @param mainViewModel The [MainViewModel] used for UI
 */
@Composable
fun AppNavigation(
    navController: NavHostController,
    mainViewModel: MainViewModel
) {
    // All states to be observed by UI
    val isVoiceReady by mainViewModel.isVoiceReady.collectAsStateWithLifecycle()
    val rawText by mainViewModel.rawText.collectAsStateWithLifecycle()
    val playingIndex by mainViewModel.playingIndex.collectAsStateWithLifecycle()
    val responseTime by mainViewModel.responseTime.collectAsStateWithLifecycle()
    val isNextEnabled by mainViewModel.isNextEnabled.collectAsStateWithLifecycle()

    NavHost(
        navController = navController,
        startDestination = "language_selection"
    ) {
        composable("language_selection") {
            LanguageSelectionScreen(
                navController = navController,
                onGetAvailableLanguages = { mainViewModel.getAvailableLanguages() },
                onLanguageSelected = { langLocale ->
                    navController.navigate("tts_demo/${langLocale.language}")
                },
                onInstallVoiceClick = {
                    navController.navigate("install_voices")
                }
            )
        }

        composable("tts_demo/{languageCode}") { backStackEntry ->
            val languageCode = backStackEntry.arguments?.getString("languageCode") ?: "en"
            val langLocale = Locale.Builder().setLanguage(languageCode).build()
            val textID = getSampleTextID(langLocale.isO3Language)
            val text = stringResource(textID)

            LaunchedEffect(languageCode) {
                mainViewModel.defaultText = text
                mainViewModel.updateText(text)
                mainViewModel.initializeVoice(langLocale)
            }

            TTSDemoScreen(
                navController = navController,
                text = rawText,
                isVoiceReady = isVoiceReady,
                playingIndex = playingIndex,
                responseTime = responseTime,
                onUpdateText = { newText: String -> mainViewModel.updateText(newText) },
                onPlaySentence = { index: Int
                    ->  mainViewModel.playSentence(index)},
                onStop = { mainViewModel.stopTts() }
            )
        }

        // In case of PAD, add language download screen
        addFlavorSpecificScreens(navController)
    }
}