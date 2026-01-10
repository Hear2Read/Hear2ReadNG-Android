/*
* This file is contributed to Hear2Read's Android App Development project
*
* Author: Nikhat Singla
* Date: June 2025
*/

package org.hear2read.h2rng

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.speech.tts.TextToSpeech.LANG_AVAILABLE
import android.speech.tts.TextToSpeech.LANG_NOT_SUPPORTED
import android.util.Log
import org.hear2read.h2rng.data.H2RVoice
import org.hear2read.h2rng.utils.getFileWithExtension
import java.lang.Float.max
import java.nio.FloatBuffer
import java.nio.LongBuffer
import kotlin.time.DurationUnit
import kotlin.time.TimeSource

// TODO!: This needs to be moved to org.hear2read.h2rng.tts, but that requires changing the
//  function signatures in libphonemizer.so to match the package

/**
 * Constant of the max value for the wav array
 */
const val MAX_WAV_VALUE: Short = Short.MAX_VALUE

/**
 * Synthesizer singleton that performs the inference for the TTS model. This uses ONNX Runtime
 * (ORT) to perform the inference. On first instantiation of this object, it loads the dynamic
 * library "libphonemizer.so" which contains the necessary phonemization logic to convert the text
 * to phonemes. The phonemes are further processed to convert them to phoneme IDs as specified by
 * the .json file of the requested voice. These phoneme IDs serve as the input to the TTS model.
 *
 */
object Synthesizer {

    init {
        System.loadLibrary("phonemizer")
    }

    private val LOG_TAG = "H2RNG_" + Synthesizer::class.simpleName

    /**
     * Single instance of the [OrtEnvironment] used by the synthesizer
     */
    private var _env : OrtEnvironment? = null

    /**
     * Stores the reference to the [ONNX Runtime session][OrtSession] that is currently loaded for
     * the most recently selected language
     */
    private var _session : OrtSession? = null


    /**
     * UNUSED!
     *
     * Stores the Job object of the coroutine that is loading/has loaded the ONNX model for most
     * recently selected language
     *
     */
//    private var _loadJob : Job? = null

    /**
     * UNUSED!
     *
     * Stores the Job object of the coroutine that is performing the actual inference and then
     * producing the sound output.
     */
//    private var _speakJob : Job? = null

    /**
     * Stores the [H2RVoice] object corresponding to the current voice that has been loaded
     */
    private var _curH2RVoice : H2RVoice? = null

    /**
     * A monotonic time source, used for benchmarking the performance of various sections of code.
     */
    private val _timeSource = TimeSource.Monotonic

    /**
     * The definition of this function is provided in the dynamic library. It is responsible for
     * the phonemization (conversion of text to phonemes), as well as further converting the
     * phonemes to the phoneme IDs per the mapping available in the config.json file of the voice
     * model.
     *
     * @param text The text to be phonemized
     * @param jsonConfigPath The path to the config.json file associated with the voice
     * @return String containing comma separated list of converted phoneme IDs
     */
    // Marking a function as JvmStatic is really important, otherwise it crashes the app
    @JvmStatic
    external fun phonemizeWithSilence(text: String, jsonConfigPath: String) : String

    /**
     * The definition of this function is provided in the dynamic library. It initializes eSpeak
     * (used for phonemization internally) by providing the datapath to where the espeak data is
     * located.
     *
     * @param espeakDataPath Path to the folder containing the espeak data
     */
    @JvmStatic
    external fun initeSpeak(espeakDataPath: String)

    @JvmStatic
    external fun termeSpeak()

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
    @JvmStatic
    fun getOrLoadModel(
        selectedH2RVoice: H2RVoice
    ): Int {

        Log.d(
            LOG_TAG, "getOrLoadModel: path: ${selectedH2RVoice.fileLocation}, " +
                "voice: ${selectedH2RVoice.id}")
        // Skip if language already loaded
        // TODO?: this may cause a problem if there's a voice update with a different ID
        if ((_curH2RVoice?.id == selectedH2RVoice.id) && (_session != null)) {
            return LANG_AVAILABLE
        }

        val assetLocation = selectedH2RVoice.fileLocation?: run {
            Log.e(LOG_TAG,"getOrLoadModel: voice file location returned null")
            return LANG_NOT_SUPPORTED
        }

        val modelFile = getFileWithExtension(assetLocation, "onnx") ?: run {
            Log.e(LOG_TAG,"getOrLoadModel: getFileWithExtension returned null")
            return LANG_NOT_SUPPORTED
        }

        Log.d(LOG_TAG, "getOrLoadModel: AssetPack: " + modelFile.absolutePath)

        // TODO!: Clean up unused code in net release
//        try {
//            _loadJob?.cancel()
//        } catch (e: Exception) {
//            Log.w(LOG_TAG,"Error canceling prev job: ${e.message}")
//            e.printStackTrace()
//        }

//        val loadJob = CoroutineScope(Dispatchers.IO).launch {
//        }


        try {
//            try {
//                _session?.close()
//                _speakJob?.cancel()
//            } catch (e: Exception) {
//                Log.w(LOG_TAG, "Error closing previous session or coroutine: ${e.message}")
//                e.printStackTrace()
//            }

            Log.d(LOG_TAG, "Current thread is ${Thread.currentThread().name}")

            // Not sure if this is necessary
            _session?.close()

            _env ?: kotlin.run {
                _env = OrtEnvironment.getEnvironment()
            }

            val startGetEnv = _timeSource.markNow()
//            val env = OrtEnvironment.getEnvironment()!!
            val endGetEnv = _timeSource.markNow()
            Log.d(LOG_TAG, "GetEnv took time: ${(endGetEnv - startGetEnv).toDouble(DurationUnit.MILLISECONDS)}")

            val startSession = _timeSource.markNow()
            _session = _env?.createSession(modelFile.absolutePath, OrtSession.SessionOptions())
            val endSession = _timeSource.markNow()
            Log.d(LOG_TAG, "Session took time: ${(endSession - startSession).toDouble(DurationUnit.MILLISECONDS)}")
            _curH2RVoice = selectedH2RVoice
        } catch (e: Exception) {
            // TODO have a better fallback
            Log.e(LOG_TAG, "Model loading failed:  ${e.message}")
            e.printStackTrace()
            _curH2RVoice = null
        }

//        _loadJob = loadJob
        // We usually call this from onSynthesizeText(), which requires it to be blocking
//        if (synchronous)
//            _loadJob!!.join()
        return LANG_AVAILABLE
    }

    /**
     * Primary function that handles text to phoneme conversion, model inference and playback.
     * - Calls [phonemizeWithSilence] to convert the text to phoneme IDs
     * - Uses the loaded ORT session to perform inference and get an array of audio samples
     * - Calls callback to return the audio sample
     * speakers
     *
     * @param text The text to be synthesized
     * @param h2RVoice H2RVoice object corresponding to the voice
     * @param rate 1..100 represents rate of speech
     * @param amplitude 0..100 represents volume of speech
     * @param callback Callback to return synthesized audio
     * @return Job associated with speaking
     */
    @JvmStatic
    fun speak(
        text: String,
        h2RVoice: H2RVoice,
        rate: Int,
        amplitude: Int,
//        assetPackPath: String,
//        callback: ((shortArray : ShortArray) -> Int),
        audioCallback: ((byteArray: ByteArray) -> Int)
    ) {
//        if (_loadJob == null)
//            return null

        Log.d(LOG_TAG, "Running synthesizer")

//        try {
//            _speakJob?.cancel()
//            Log.d(LOG_TAG, "Previous speaking stopped.")
//        } catch (e : Exception) {
//            Log.w(LOG_TAG, "Error closing previous session or coroutine: ${e.message}")
//            e.printStackTrace()
//        }

//        val speakJob = CoroutineScope(Dispatchers.Default).launch {
//
//
//            while (! _loadJob!!.isCompleted) {
//                // TODO: to track max time limit
//            }
//
//
//        }

        _env ?: kotlin.run {
            _env = OrtEnvironment.getEnvironment()
        }

        // Check if the voice is loaded
        if ((_curH2RVoice?.id != h2RVoice.id) || (_session == null)) {
            getOrLoadModel(h2RVoice)
        }

//        _speakJob = speakJob
        val assetPackPath = h2RVoice.fileLocation ?: run {
            Log.e(LOG_TAG, "speak: assetPackPath is null")
            return
        }
        val jsonConfigFile =
            getFileWithExtension(assetPackPath, "json") ?: return
//            val jsonConfigFile = File(context.filesDir, langToFile[selectedLanguage]!! + ".json")

        //            Log.d(LOG_TAG, "Model file is ${modelFile.isFile()}")
        Log.d(LOG_TAG,"Config file is ${jsonConfigFile.isFile}")
        //            Log.d(LOG_TAG, isDirectoryPathValid(context.filesDir.absolutePath))

        val startEspeak = _timeSource.markNow()
        val outputTextToPhonemes = phonemizeWithSilence(text, jsonConfigFile.absolutePath)
        val endEspeak = _timeSource.markNow()
        Log.d(LOG_TAG, "Espeak took time: ${(endEspeak - startEspeak).toDouble(DurationUnit.MILLISECONDS)}")

        // TODO?: frame skips observed on app startup. Optimise for multi-threading
        val startGetEnv = _timeSource.markNow()
//            val env = OrtEnvironment.getEnvironment()!!
        val endGetEnv = _timeSource.markNow()
        Log.d(LOG_TAG, "GetEnv took time: ${(endGetEnv - startGetEnv).toDouble(DurationUnit.MILLISECONDS)}")

        Log.d(LOG_TAG, "outputTextToPhonemes: $outputTextToPhonemes")

        val startRep = _timeSource.markNow()
//        val phonemeIdsArr = outputTextToPhonemes
//            //                .replace(";", ",")
//            //                .replace(" ", ",")
//            //                .replace("\t", ",")
//            //                .replace("\n", ",") // normalise text first into a common delimiter
//            .split(",")
//            .filter { it.isNotBlank() } // filter out empty tokens
//            .map { it.toLong() }
//            .toLongArray()

        val sentenceArray = outputTextToPhonemes
            .split(";") // sentence array
            .filter { it.isNotBlank() } // filter out empty sentences
            .map {
                it
                    .split(",") // split each sentence by comma to get token(s)
                    .filter { token -> token.isNotBlank() } // remove empty token(s)
                    .map { token -> token.toLong() } // typecast each token to Long
                    .toLongArray() // convert the iterable into Kotlin LongArray
            }

        // Added for Debugging
        for (sentence in sentenceArray) {
            Log.d(LOG_TAG, "Sentence Begins")
            for (token in sentence) {
                Log.d(LOG_TAG, "Token: $token")
            }
        }

        val endRep = _timeSource.markNow()
        Log.d(LOG_TAG, "Replacement took time: ${(endRep - startRep).toDouble(DurationUnit.MILLISECONDS)}")

        //            Log.d(LOG_TAG, phonemeIdsArr.toString())

        val startInputPrep = _timeSource.markNow()

        // TODO: even verify if it is the correct way to control speed
        // TODO: safely handle phonemeLenScale values out of bounds
        var phonemeLenScale: Float = (1.0f / (((rate.toFloat() - 50.0f) / 25.0f) + 1.0f))
        if (rate < 50) {
            phonemeLenScale = (1.0f / (((rate.toFloat()) / 75.0f) + (1.0f / 3.0f)))
        }

        // TODO: load scalesArr values from JSON
        val scalesArr = floatArrayOf(0.667f, phonemeLenScale, 0.8f)
        val scales = FloatBuffer.wrap(scalesArr)
        val scalesShape = longArrayOf(scalesArr.size.toLong())
        val speakerID = LongBuffer.wrap(longArrayOf(0))
        val speakerIDShape = longArrayOf(1)

        for (itr in sentenceArray.indices) {
            val sentenceStartTime = _timeSource.markNow()
            Log.d(LOG_TAG, "Sentence $itr begins")

            val phonemeIdsArr = sentenceArray[itr]
            val phonemeIds = LongBuffer.wrap(phonemeIdsArr)
            val phonemeIdsShape = longArrayOf(1, phonemeIdsArr.size.toLong())

            val phonemeIdsLengthsArr = longArrayOf(phonemeIdsArr.size.toLong())
            val phonemeIdsLengths = LongBuffer.wrap(phonemeIdsLengthsArr)
            val phonemeIdLengthsShape = longArrayOf(phonemeIdsLengthsArr.size.toLong())

            val ortInputs = mutableMapOf(
                "input" to OnnxTensor.createTensor(_env, phonemeIds, phonemeIdsShape),
                "input_lengths" to OnnxTensor.createTensor(
                    _env,
                    phonemeIdsLengths,
                    phonemeIdLengthsShape
                ),
                "scales" to OnnxTensor.createTensor(_env, scales, scalesShape),
            )
            if (h2RVoice.isMultiSpeaker) {
                ortInputs["sid"] = OnnxTensor.createTensor(_env, speakerID, speakerIDShape)
            }
            val endInputPrep = _timeSource.markNow()
            Log.d(
                LOG_TAG,
                "Input prep took time: ${(endInputPrep - sentenceStartTime).toDouble(DurationUnit.MILLISECONDS)}"
            )

            try {
                val start = _timeSource.markNow()
                _session?.run(ortInputs)?.use { results ->
                    val end = _timeSource.markNow()
                    val inferenceTimeMillis = (end - start).toDouble(DurationUnit.MILLISECONDS)
                    Log.d(LOG_TAG, "ORT took time: $inferenceTimeMillis ms")

                    val outputTensor =
                        results.get(0) as OnnxTensor // Since output map will have only one node which is called "output"
                    val outputShape: LongArray = outputTensor.info.shape as LongArray
                    val outputBuffer = outputTensor.floatBuffer

                    val audioCount = outputShape[3].toInt()
                    val outputArr = FloatArray(audioCount)
                    outputBuffer.get(outputArr)

                    // Find max value in outputArr
                    var maxAudioValue: Float = Float.NEGATIVE_INFINITY
                    for (i in 0 until audioCount) {
                        val value = outputArr[i]
                        if (value > maxAudioValue) {
                            maxAudioValue = value
                        }
                    }

                    // TODO: discuss if it is the correct and preferred way
                    // TODO: handle amplitude out of bounds
                    val audioScale = MAX_WAV_VALUE * (amplitude.toFloat() / 100.0f) / max(
                        0.01f,
                        maxAudioValue
                    ) // 0.01f to avoid division by zero
//                val shortArray = ShortArray(audioCount)
                    val byteArray = ByteArray(audioCount * Short.SIZE_BYTES)
                    for (i in 0 until audioCount) {
                        val scaled = outputArr[i] * audioScale

//                    shortArray[i] = scaled.coerceIn(
//                        Short.MIN_VALUE.toFloat(),
//                        Short.MAX_VALUE.toFloat()
//                    ).toInt().toShort()

                        val intVal = scaled.coerceIn(
                            Short.MIN_VALUE.toFloat(),
                            Short.MAX_VALUE.toFloat()
                        ).toInt()

                        byteArray[2 * i] = (intVal).toByte()
                        byteArray[2 * i + 1] = (intVal shr 8).toByte()
                    }

                    val realTimeMillis = audioCount.toDouble() * 1000.0 / 22500.0
                    Log.d(LOG_TAG, "Real time: $realTimeMillis ms")

                    Log.d(LOG_TAG, "Inference ratio: ${inferenceTimeMillis / realTimeMillis}")

//                callback(shortArray)
                    audioCallback(byteArray)
                }
            } catch (e: Exception) {
                Log.e(LOG_TAG, "Exception ${e.message} has occurred.")
                e.printStackTrace()
            }
        }

//        return _speakJob
    }
}