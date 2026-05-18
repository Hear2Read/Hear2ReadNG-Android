package org.hear2read.h2rng.ui.screens

import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import org.hear2read.h2rng.ui.components.GlobalTopBar
import org.hear2read.h2rng.ui.components.LanguageListItem
import org.hear2read.h2rng.ui.components.VoiceManagerBtnProvider
import org.hear2read.h2rng.ui.theme.AppColors
import java.util.Locale

/**
 * Language selection screen used to navigate to corresponding [TTSDemoScreen] to test the language.
 * Consists of a list of [LanguageListItem]'s which is populated by obtaining the available voices
 * as returned by the TTS instance. If there are no voices downloaded, displays a message instead.
 * Also contains a floating action button that navigates to the [VoiceManagerScreen].
 *
 * @param navController [NavHostController] associated with the activity to navigate to and from
 * the screen
 * @param getTTS Function to access the single TTS instance created in the [MainActivity]
 * @param onLanguageSelected Function called when the [LanguageListItem] is clicked. Will navigate
 * to the corresponding [TTSDemoScreen]
 * @param onInstallVoiceClick Function called when floating action button to add voices clicked.
 * Will navigate to the [VoiceManagerScreen]
 */
@Composable
fun LanguageSelectionScreen(
    navController: NavHostController,
    getTTS: () -> TextToSpeech?,
    onLanguageSelected: (Locale) -> Unit,
    onInstallVoiceClick: () -> Unit
) {
    val LOG_TAG = "H2RNG_" + "LanguageSelectionScreen"
    val availableLangs = getTTS()?.availableLanguages?.toList()?.sortedBy { it.displayLanguage }
        ?: emptyList()

    Log.d(LOG_TAG, "availableLangs: $availableLangs")

    Scaffold(
        topBar = {
            GlobalTopBar("Select Language") {
                navController.navigateUp()
            }
        },
        floatingActionButton = { VoiceManagerBtnProvider.get().floatingButton(onInstallVoiceClick) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.backgroundColor)
                .padding(paddingValues)
        ) {
            // Loading Dialog
//            if (isLoading) {
//                LoadingDialog()
//            }
            if (availableLangs.isEmpty()) {
                EmptyStateView()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(availableLangs) { languageLocale ->
                        LanguageListItem(
                            langLocale = languageLocale,
                            onClick = { onLanguageSelected(languageLocale) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Empty state view that is shown when no voices have been found
 *
 */
@Composable
private fun EmptyStateView() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
            .semantics {
                contentDescription = "No voices available. Please add voices."
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "No Voices Available",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = AppColors.textColor
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Please add voices",
            fontSize = 16.sp,
            color = AppColors.textColor,
//            color = AppColors.textSecondaryColor,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}