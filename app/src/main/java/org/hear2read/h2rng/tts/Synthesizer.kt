/*
* This file is contributed to Hear2Read's Android App Development project
*
* Author: Nikhat Singla
* Date: June 2025
*/

package org.hear2read.h2rng.tts

import android.speech.tts.TextToSpeech.LANG_AVAILABLE
import android.speech.tts.TextToSpeech.LANG_NOT_SUPPORTED
import android.util.Log
import androidx.annotation.Keep
import org.hear2read.h2rng.data.H2RVoice
import org.hear2read.h2rng.utils.getFileWithExtension
import java.io.File

/**
 * Callback interface to receive audio and index reached callbacks from the engine
 *
 * @constructor Create empty H2RNGCallback
 */
// TODO: remove redundant Keep annotation (relevant proguard rules are present)
@Keep
interface H2RNGCallback {
    fun onAudio(samples: ShortArray)
    fun onIndexReached(index: Int)
    fun onDone()
}

/**
 * Synthesizer singleton that performs the inference for the TTS model. This uses ONNX Runtime
 * (ORT) to perform the inference. On first instantiation of this object, it loads the dynamic
 * libraries "libonnxruntime.so" and "libh2r-ng.so". Initialization of the engine requires the
 * data directory containing the espeak data, and the callbacks to feed the generated audio back
 */
object Synthesizer {
    init {
        // ORT must be loaded first since h2r-ng depends on it
//        System.loadLibrary("onnxruntime")
        System.loadLibrary("h2r-ng")
    }

    private val LOG_TAG = "H2RNG_" + Synthesizer::class.simpleName

    /**
     * Stores the [H2RVoice] object corresponding to the current voice that has been loaded
     */
    private var _curH2RVoice : H2RVoice? = null

    /**
     * This function is responsible for loading the ONNX model of the voice. It tries to locate the
     * ONNX model file and create an ORT session (equivalent to model loading) using this file. It
     * also cancels any load/speak job, if any.
     *
     * This function is only called from within the [TtsService.onSynthesizeText] which is further
     * only called on the synthesis thread. This negates the need for thread safety.
     *
     * @param selectedH2RVoice H2RVoice object corresponding to the model file
     * @return LANG_AVAILABLE or LANG_NOT_SUPPORTED depending whether the loading was successful
     */
    fun getOrLoadModel(
        selectedH2RVoice: H2RVoice
    ): Int {

        Log.d(
            LOG_TAG, "getOrLoadModel: path: ${selectedH2RVoice.fileLocation}, " +
                "voice: ${selectedH2RVoice.id}")
        // Skip if language already loaded
        // TODO?: this may cause a problem if there's a voice update with a different ID
        if (_curH2RVoice?.id == selectedH2RVoice.id) {
            return LANG_AVAILABLE
        }

        val assetLocation = selectedH2RVoice.fileLocation ?: run {
            Log.e(LOG_TAG, "getOrLoadModel: voice file location returned null")
            return LANG_NOT_SUPPORTED
        }

        val modelFile = File(assetLocation, "${selectedH2RVoice.id}.onnx")
        val configFile = File(assetLocation, "${selectedH2RVoice.id}.onnx.json")
        if(!modelFile.isFile or !configFile.isFile) {
            Log.e(LOG_TAG, "getOrLoadModel: couldn't find corresponding files: ${selectedH2RVoice.id}")
            return LANG_NOT_SUPPORTED
        }

        try {
            Log.d(LOG_TAG, "getOrLoadModel: setVoice: ${selectedH2RVoice.id}, ${assetLocation}")
            setVoice(selectedH2RVoice.id, assetLocation)
            _curH2RVoice = selectedH2RVoice
        } catch (e: Exception) {
            Log.e(LOG_TAG, "getOrLoadModel: setVoice failed: ${e.message}")
            _curH2RVoice = null
            return LANG_NOT_SUPPORTED
        }

        return LANG_AVAILABLE

    }


    @Keep
    external fun init(dataPath: String, callback: H2RNGCallback): Int

    @Keep
    external fun setVoice(voiceName: String, dataPath: String): Int

    @Keep
    external fun synthesize(
        text: String,   // TODO? create class corresponding to SynthRequest struct?
        speed: Float,
        pitch: Float,
        volume: Int,
        charMode: Boolean
    ): Int

    @Keep
    external fun terminate()
}
