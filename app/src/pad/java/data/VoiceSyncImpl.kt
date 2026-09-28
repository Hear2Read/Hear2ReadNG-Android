package data

import android.content.Context
import android.util.Log
import com.google.android.play.core.assetpacks.AssetPackException
import com.google.android.play.core.assetpacks.AssetPackManagerFactory
import com.google.android.play.core.assetpacks.model.AssetPackErrorCode
import com.google.android.play.core.assetpacks.model.AssetPackStatus
import com.google.android.play.core.common.LocalTestingException
import com.google.android.play.core.ktx.requestPackStates
import org.hear2read.h2rng.data.H2RVoiceRepository
import org.hear2read.h2rng.data.VoiceSync

/**
 * PAD supported implementation of the [VoiceSync] interface. Uses AssetPackManager to get voice
 * states
 *
 */
class VoiceSyncImpl: VoiceSync {
    val LOG_TAG = "H2RNG_VoiceSyncImplPad"
    override suspend fun syncVoiceStates(context: Context) {
        val voices = H2RVoiceRepository.getCurrentVoices()
        val packList = voices.map{voice -> voice.assetID }
        Log.d(LOG_TAG, "syncDownloadStates: packlist: $packList")
        val assetPackManager = AssetPackManagerFactory.getInstance(context)

        for(pack in packList) {
            // Adding a try/catch block as all voice packs may not be included in the build while
            // testing locally
            try {
                // Check if location exists: this doesn't need internet connection and ensures that
                // downloaded packs are available offline
                val packPath = assetPackManager.getPackLocation(pack)?.assetsPath()
                if (packPath != null) {
                    H2RVoiceRepository.updateDownloadInfo(pack, true, 0L,
                        packPath)
                    continue
                }
                val packStates = assetPackManager.requestPackStates(listOf(pack))
                val packState = packStates.packStates()[pack]
                Log.d(LOG_TAG,
                    "syncDownloadStates: packstate of: ${pack} is ${packState.toString()}")

                if (packState != null) {
                    // TODO: Check for other error codes?
                    Log.d(LOG_TAG,
                        "syncDownloadStates: packstate error code: ${packState.errorCode()}")
                    if (packState.errorCode() == AssetPackErrorCode.PACK_UNAVAILABLE) {
                        Log.w(LOG_TAG, "AssetPack $pack not found! Deleting from repository")
                        H2RVoiceRepository.removeVoiceAssetId(pack)
                        continue
                    }
//                val assetPackLocation = assetPackManager.getPackLocation(voice.assetID)
                    val filePath = assetPackManager.getPackLocation(pack)?.assetsPath()
                    val isDownloaded = (packState.status() == AssetPackStatus.COMPLETED)
                            && !(filePath == null)
                    val size = packState.totalBytesToDownload()

                    Log.d(LOG_TAG,
                        "syncDownloadStates: adding: ${pack} isDownloaded? ${isDownloaded}")
                    H2RVoiceRepository.updateDownloadInfo(pack, isDownloaded, size, filePath)
                }
            }
            catch (e: LocalTestingException) {
                Log.w(LOG_TAG,
                    "AssetPack $pack not found in local testing! Deleting from repository")
                H2RVoiceRepository.removeVoiceAssetId(pack)
            }
            catch (e: AssetPackException) {
                Log.w(LOG_TAG,
                    "Error obtaining asset pack information, check internet: ${e.message}")
                H2RVoiceRepository.removeVoiceAssetId(pack)
            }
            catch (e: Exception) {
                Log.w(LOG_TAG,
                    "Unexpected error loading asset packs: ${e.message}")
            }
        }
    }
}