package data

import android.content.Context
import android.util.Log
import org.hear2read.h2rng.data.H2RVoiceRepository
import org.hear2read.h2rng.data.VoiceSync
import org.hear2read.h2rng.utils.LOG_PREFIX
import org.hear2read.h2rng.utils.copyAssets
import java.io.File

/**
 * Implementation of the [VoiceSync] interface for "local" flavour. Copies assets to filesDir and
 * updates the voice repository based on the files that are successfully copied and present in
 * device storage
 */
class VoiceSyncImpl: VoiceSync {
    val LOG_TAG = LOG_PREFIX + "VoiceSyncImplLocal"
    override suspend fun syncVoiceStates(context: Context) {
        // get voice locations and populate repository
        // get wav locations and add to voice data struct
        copyAssets(context, "wavs")
        copyAssets(context, "Voices")

        val voices = H2RVoiceRepository.getCurrentVoices()
        val voiceFileList = voices.map{voice -> voice.id }

        val voiceDir = File(context.filesDir, "Voices")
        val voiceDirList = voiceDir.list()?:emptyArray<String>()

        Log.d(LOG_TAG, "Found voice files: $voiceDirList")

        for(voice in voices) {
            Log.d(LOG_TAG, "Checking voice : ${voice.id}")
            val file = voice.id
            val onnxFile = file + ".onnx"
            val configFile = onnxFile + ".json"

            if(!(onnxFile in voiceDirList) or !(configFile in voiceDirList)) {
                H2RVoiceRepository.removeVoice(voice.assetID)
            }
            else {
                Log.d(LOG_TAG, "Found voice : ${voice.id}")
                val voiceFilePath = File(voiceDir, onnxFile).absolutePath
                H2RVoiceRepository.updateDownloadInfo(voice.assetID,
                                                 true,
                                                        0L,
                                                             voiceDir.absolutePath)
            }

        }
    }
}