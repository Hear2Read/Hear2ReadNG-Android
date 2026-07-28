package org.hear2read.h2rng.tts.api

import com.google.android.play.core.assetpacks.AssetPackManager
import com.google.android.play.core.assetpacks.AssetPackState
import com.google.android.play.core.assetpacks.AssetPackStateUpdateListener
import com.google.android.play.core.assetpacks.model.AssetPackStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.hear2read.h2rng.data.H2RVoiceRepository
import org.hear2read.h2rng.utils.getFileWithExtension
import org.hear2read.h2rng.utils.unzipWavs
import java.io.File

/**
 * Listener that updates [H2RVoiceRepository] when asset pack states change. This is used to
 * automatically update progress in the UI. Only used in PAD supported flavours
 *
 * @property filesDir Location of the directory to unzip additional voice data (wavs) to
 * @constructor
 *
 * @param assetPackManager AssetPackManager instance to fetch asset packs
 */
class H2RPackUpdateListener(assetPackManager: AssetPackManager, private val filesDir: File)
    : AssetPackStateUpdateListener {
    // Create a coroutine scope for updating repository from callbacks
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val packManager = assetPackManager

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
                    val downloadedBytes = state.bytesDownloaded()
                    val totalBytes = state.totalBytesToDownload()
                    val progress = if (totalBytes > 0) {
                        (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 0.99f)
                    } else {
                        0f
                    }
                    H2RVoiceRepository.updateDownloadProgress(
                        assetID = assetID,
                        downloadProgress = progress
                    )
                }

                AssetPackStatus.TRANSFERRING -> {
                    // Almost complete, still update progress
                    H2RVoiceRepository.updateDownloadProgress(
                        assetID = assetID,
                        downloadProgress = 0.99f
                    )
                }

                AssetPackStatus.COMPLETED -> {
                    // Download complete
                    val filePath = packManager.getPackLocation(assetID)?.assetsPath()
                    H2RVoiceRepository.updateDownloadInfo(
                        assetID = assetID,
                        isDownloaded = filePath != null,
                        size = state.totalBytesToDownload(),
                        filePath = filePath
                    )
                    val wavFile = getFileWithExtension(filePath.toString(), "onnx.zip") ?: run {
//                        Log.e(LOG_TAG, "getOrLoadModel: getFileWithExtension returned null")
                        return@launch
                    }
                    unzipWavs(wavFile, filesDir)
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