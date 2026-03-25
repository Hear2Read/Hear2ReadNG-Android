package org.hear2read.h2rng.ui.screens

import androidx.compose.foundation.background
import org.hear2read.h2rng.ui.components.GlobalTopBar
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.android.play.core.assetpacks.AssetPackManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.hear2read.h2rng.tts.api.H2RPackUpdateListener
import org.hear2read.h2rng.ui.components.VoiceDownloadItem
import org.hear2read.h2rng.ui.theme.AppColors
import org.hear2read.h2rng.data.H2RVoiceRepository
import java.io.File

/**
 * Voice manager screen to download and delete voices. Contains a list of
 * [VoiceDownloadItems][VoiceDownloadItem] which is populated and configured by listening to the
 * [H2RVoiceRepository] singleton
 *
 * @param navController [NavController] associated with the activity to navigate to and from the
 * screen
 * @param assetPackManager [AssetPackManager] instance used to download and delete voices
 * TODO: remove assetPackManager and pass functions instead
 */
@Composable
fun VoiceManagerScreen(
    navController: NavController,
    assetPackManager: AssetPackManager,
    filesDir: File
) {
    // Observe voices from repository
    val voices by H2RVoiceRepository.voices.collectAsState()
    val scope = rememberCoroutineScope()

    // Register listener for progress updates
    DisposableEffect(Unit) {
        val listener = H2RPackUpdateListener(assetPackManager, filesDir)
        assetPackManager.registerListener(listener)

        onDispose {
            assetPackManager.unregisterListener(listener)
        }
    }

    Scaffold (
        topBar = {
            GlobalTopBar("Voice Manager") {
                navController.navigateUp()
            }
        },
        modifier = Modifier.fillMaxWidth()
    ){ paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.backgroundColor)
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                items(voices) { voiceItem ->
                    VoiceDownloadItem(
                        voiceItem,
                        onDownload = { voice ->
                            scope.launch(Dispatchers.IO) {
//                                installVoice(voice, assetPackManager)
                                assetPackManager.fetch(listOf(voice.assetID))
                            }
                        },
                        onDelete = { voice ->
                            scope.launch(Dispatchers.IO) {
//                                deleteVoice(voice, assetPackManager)
                                // Delete voice
                                assetPackManager.removePack(voice.assetID)
                                H2RVoiceRepository.updateDownloadState(voice.assetID, false)
                                H2RVoiceRepository.updateDownloadSize(voice.assetID, 0L)
                            }
                        }
                    )
                }
            }
        }
    }
}