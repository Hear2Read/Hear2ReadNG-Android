package org.hear2read.h2rng.ui.screens

import android.speech.tts.TextToSpeech
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import org.hear2read.h2rng.tts.getSampleText
import org.hear2read.h2rng.tts.initializeTTS
import org.hear2read.h2rng.tts.playSentence
import org.hear2read.h2rng.ui.components.GlobalTopBar
import org.hear2read.h2rng.ui.components.LoadingDialog
import org.hear2read.h2rng.ui.components.MediaControlButtons
import org.hear2read.h2rng.ui.components.ResponseTimeCard
import org.hear2read.h2rng.ui.components.TextInputSection
import org.hear2read.h2rng.ui.theme.AppColors
import org.hear2read.h2rng.utils.splitIntoSentences
import java.util.Locale

/**
 * Screen to demo the Hear2ReadNG TTS. Configured based on the language. Displays an text input
 * section and media control buttons to control the playback of the synthesis. Also has a response
 * time card to display the latency of the TTS on the device.
 *
 * On startup, it sends a blank string with the language set to the current language to the TTS and
 * waits until it returns, in order to ensure that the language is loaded. A loading dialog is
 * displayed until this process is completed
 *
 * @param langLocale [Locale] of the language, expected to be constructed using the ISO 2 letter
 * code
 * @param navController [NavHostController] associated with the activity to navigate to and from
 * the screen
 * @param getTTS Function to access the single TTS instance created in the [MainActivity]
 * @param setTTS Function to set the single TTS instance TODO: try to remove this redundancy
 */
@Composable
fun TTSDemoScreen(
    langLocale: Locale,
    navController: NavHostController,
    getTTS: () -> TextToSpeech?,
    setTTS: (TextToSpeech?) -> Unit
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var currentText by remember { mutableStateOf(getSampleText(context, langLocale.isO3Language)) }
    var sentences by remember { mutableStateOf(splitIntoSentences(currentText)) }
    var currentSentenceIndex by remember { mutableIntStateOf(0) }
    var isPlaying by remember { mutableStateOf(false) }
    var responseTime by remember { mutableStateOf<Long?>(null) }
    var playStartTime by remember { mutableStateOf<Long?>(null) }

    /**
     * Helper function used to capture the behaviour when a sentence is to be played back. Used
     * when the play button is pressed as well as when the previous or next button are pressed.
     * Plays the current sentence and queues the next sentence when the current sentence has
     * completed playback. TODO: queue before without flushing to enable continous playback?
     *
     */
    fun onPlay(){
        val tts = getTTS()
        if (sentences.isNotEmpty() && currentSentenceIndex < sentences.size) {
            playStartTime = System.currentTimeMillis()
            responseTime = null
            playSentence(
                tts = tts,
                sentence = sentences[currentSentenceIndex],
                index = currentSentenceIndex,
                onStart = {
                    playStartTime?.let { startTime ->
                        if (responseTime == null)
                            responseTime = System.currentTimeMillis() - startTime
                    }
                },
                onComplete = {
                    if (currentSentenceIndex < sentences.size - 1) {
                        currentSentenceIndex++
                        playSentence(
                            tts = tts,
                            sentence = sentences[currentSentenceIndex],
                            index = currentSentenceIndex,
                            onStart = {},
                            onComplete = {
                                if (currentSentenceIndex >= sentences.size - 1) {
                                    isPlaying = false
                                }
                            }
                        )
                    } else {
                        isPlaying = false
                    }
                }
            )
            isPlaying = true
        }

    }

    // Initialize TTS
    LaunchedEffect(langLocale) {
        initializeTTS(
            context = context,
            langLocale = langLocale,
            getTTS = getTTS,
            setTTS = setTTS,
            onInitialized = { isLoading = false },
            onError = {
                isLoading = false
                // TODO: Handle error
            }
        )
    }

    // Cleanup on navigation away
    DisposableEffect(Unit) {
        onDispose {
            getTTS()?.stop()
        }
    }

    Scaffold(
        topBar = {
            GlobalTopBar("Text to Speech Demo") {
                getTTS()?.stop()
                navController.navigateUp()
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.backgroundColor)
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Response Time Display
                ResponseTimeCard(responseTime)

                Spacer(modifier = Modifier.height(16.dp))

                // Text Input Section
                TextInputSection(
                    text = currentText,
                    onTextChange = { newText ->
                        currentText = newText
                        sentences = splitIntoSentences(newText)
                        currentSentenceIndex = 0
                        responseTime = null
                    },
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Media Control Buttons
                MediaControlButtons(
                    isPlaying = isPlaying,
                    hasText = currentText.isNotBlank(),
                    hasPrevious = currentSentenceIndex > 0,
                    hasNext = currentSentenceIndex < sentences.size - 1,
                    onPlayPause = {
                        val tts = getTTS()
                        if (isPlaying) {
                            tts?.stop()
                            isPlaying = false
                        } else {
                            onPlay()
                        }
                    },
                    onPrevious = {
                        getTTS()?.stop()
                        isPlaying = false
                        responseTime = null
                        if (currentSentenceIndex > 0) {
                            currentSentenceIndex--
                        }
                        onPlay()
                    },
                    onNext = {
                        getTTS()?.stop()
                        isPlaying = false
                        responseTime = null
                        if (currentSentenceIndex < sentences.size - 1) {
                            currentSentenceIndex++
                        }
                        onPlay()
                    },
                    onClearReset = {
                        getTTS()?.stop()
                        isPlaying = false
                        responseTime = null
                        currentSentenceIndex = 0

                        if (currentText.isBlank()) {
                            currentText = getSampleText(context, langLocale.isO3Language)
                            sentences = splitIntoSentences(currentText)
                        } else {
                            currentText = ""
                            sentences = emptyList()
                        }
                    }
                )
            }

            // Loading Dialog
            if (isLoading) {
                LoadingDialog()
            }
        }
    }
}