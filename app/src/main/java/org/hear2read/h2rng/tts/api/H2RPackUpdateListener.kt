package org.hear2read.h2rng.tts.api

import com.google.android.play.core.assetpacks.AssetPackState
import com.google.android.play.core.assetpacks.AssetPackStateUpdateListener
import com.google.android.play.core.assetpacks.model.AssetPackStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.hear2read.h2rng.data.H2RVoiceRepository

/**
 * Listener that updates [H2RVoiceRepository] when asset pack states change. This is used to
 * automatically update progress in the UI
 *
 */
class H2RPackUpdateListener : AssetPackStateUpdateListener {

    // Create a coroutine scope for updating repository from callbacks
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onStateUpdate(state: AssetPackState) {
        val assetID = state.name() // Assuming pack name is voice ID

        scope.launch {
            when (state.status()) {
                AssetPackStatus.PENDING -> {
                    // Download is queued
                    H2RVoiceRepository.updateDownloadSize(assetID, state.totalBytesToDownload())
                }

                AssetPackStatus.DOWNLOADING -> {
                    // Update progress during download
                    H2RVoiceRepository.updateDownloadProgress(
                        assetID = assetID,
                        downloadedBytes = state.bytesDownloaded(),
                        totalBytes = state.totalBytesToDownload()
                    )
                }

                AssetPackStatus.TRANSFERRING -> {
                    // Almost complete, still update progress
                    H2RVoiceRepository.updateDownloadProgress(
                        assetID = assetID,
                        downloadedBytes = state.bytesDownloaded(),
                        totalBytes = state.totalBytesToDownload()
                    )
                }

                AssetPackStatus.COMPLETED -> {
                    // Download complete
                    H2RVoiceRepository.updateDownloadInfo(
                        assetID = assetID,
                        isDownloaded = true,
                        size = state.totalBytesToDownload()
                    )
                }

                AssetPackStatus.FAILED -> {
                    // Download failed - reset progress
                    H2RVoiceRepository.resetDownloadProgress(assetID)
                }

                AssetPackStatus.CANCELED -> {
                    // Download canceled - reset progress
                    H2RVoiceRepository.resetDownloadProgress(assetID)
                }

                AssetPackStatus.NOT_INSTALLED -> {
                    // Not downloaded
                    H2RVoiceRepository.updateDownloadState(assetID, isDownloaded = false)
                }

                else -> {
                    // Handle other states as needed
                }
            }
        }
    }
}