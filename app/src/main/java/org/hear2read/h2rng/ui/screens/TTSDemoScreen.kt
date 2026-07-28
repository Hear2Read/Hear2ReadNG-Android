package org.hear2read.h2rng.ui.screens

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import org.hear2read.h2rng.ui.components.GlobalTopBar
import org.hear2read.h2rng.ui.components.LoadingDialog
import org.hear2read.h2rng.ui.components.MediaControlButtons
import org.hear2read.h2rng.ui.components.ResponseTimeCard
import org.hear2read.h2rng.ui.components.TextInputSection
import org.hear2read.h2rng.ui.theme.AppColors
import org.hear2read.h2rng.utils.LOG_PREFIX

/**
 * Screen to demo the Hear2ReadNG TTS. Configured based on the language. Displays an text input
 * section and media control buttons to control the playback of the synthesis. Also has a response
 * time card to display the latency of the TTS on the device.
 *
 * On startup, it sends a blank string with the language set to the current language to the TTS and
 * waits until it returns, in order to ensure that the language is loaded. A loading dialog is
 * displayed until this process is completed
 *
 * @param navController [NavHostController] associated with the activity to navigate to and from
 * the screen
 * @param text Text to be read and displayed
 * @param isVoiceReady Signals whether TTS is ready with voice
 * @param playingIndex If not null, reading and value is index of sentence being read. If
 *          null, not reading text
 * @param responseTime Time taken to start playback of audio from button click
 * @param onPlaySentence Plays the sentence at index passed in, through the TTS
 * @param onStop Stops TTS playback
 * @param onUpdateText Called when text is updated
 */
@Composable
fun TTSDemoScreen(
    navController: NavHostController,
    text: String,
    isVoiceReady: Boolean,
    playingIndex: Int?,
    responseTime: Long?,
    onPlaySentence: (index: Int) -> Unit,
    onStop: () -> Unit,
    onUpdateText: (text: String) -> Unit
) {

    val LOG_TAG = LOG_PREFIX + "TTSDemoScreen"

    // Cleanup on navigation away
    DisposableEffect(Unit) {
        onDispose {
            onStop()
        }
    }

    Scaffold(
        topBar = {
            GlobalTopBar("Text to Speech Demo") {
                onStop()
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
                    text = text,
                    onTextChange = { newText ->
                        onUpdateText(newText)
                    },
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Media Control Buttons
                MediaControlButtons(
                    isPlaying = playingIndex != null,
                    hasText = text.isNotBlank(),
                    onPlayPause = {
//                        Log.d(LOG_TAG, "onPlayPause: playingIndex is ${playingIndex}")
                        if (playingIndex != null) {
                            onStop()
                        } else {
                            onPlaySentence(0)
                        }
                    },
                    onPrevious = {
                        val i = playingIndex?.let{ it - 1 }?: 0
                        onPlaySentence(i)
                    },
                    onNext = {
                        val i = playingIndex?.let{ it + 1 }?: 1
                        onPlaySentence(i)
                    },
                    onClearReset = {
                        onStop()
                        onUpdateText("")
                    }
                )
            }

            // Loading Dialog
            if (!isVoiceReady) {
                LoadingDialog()
            }
        }
    }
}