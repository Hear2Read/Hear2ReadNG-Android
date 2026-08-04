/*
* This file is contributed to Hear2Read's Android App Development project
*
* Author: Nikhat Singla
* Date: June 2025
*/

package org.hear2read.h2rng.tts.api

import android.database.ContentObserver
import android.media.AudioFormat
import android.speech.tts.SynthesisCallback
import android.speech.tts.SynthesisRequest
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.ERROR
import android.speech.tts.TextToSpeech.SUCCESS
import android.speech.tts.TextToSpeechService
import android.speech.tts.Voice
import android.util.Log
import androidx.annotation.Keep
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.hear2read.h2rng.data.H2RVoice
import org.hear2read.h2rng.data.H2RVoiceRepository
import org.hear2read.h2rng.data.H2RVoiceRepository.getCurrentVoices
import org.hear2read.h2rng.tts.H2RNGCallback
import org.hear2read.h2rng.tts.Synthesizer
import org.hear2read.h2rng.tts.iso3ToIso2Map
import org.hear2read.h2rng.utils.LOG_PREFIX
import java.util.Locale
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.coroutineContext

/**
 * [TextToSpeechService] implementation for the Hear2ReadNG TTS synthesiser
 *
 */
class TtsService : TextToSpeechService(), H2RNGCallback {
    private val LOG_TAG = LOG_PREFIX + TtsService::class.simpleName

    /**
     * Service scope used to wait for the [H2RVoiceRepository] to initialize
     */
    private val ttsServiceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    /**
     * Stores the Locale of the current voice with the ISO 2 letter code of the language
     */
    private var _curLang: Locale? = null

    /**
     * Stores the value of the current rate of speech
     */
    private var _curRate: Int = 100

    /***
     * Stores current SynthesisCallback
     */
    private var _callback: SynthesisCallback? = null

    /**
     * onCreate checks whether the H2RVoiceRepository has been initialized and the download states
     * synced before continuing. Also ensures that the espeak data dir has been copied.
     *
     */
    override fun onCreate() {
        Log.i(LOG_TAG, "onCreate: Hear2Read TTS Service")
        super.onCreate()

        ttsServiceScope.launch {
            // Wait for voice repository to initialize
            H2RVoiceRepository.isReady.first { it == true }
        }

        Synthesizer.init(filesDir.absolutePath, this)
    }


    /**
     * Overrides [TextToSpeechService.onLoadLanguage]
     *
     * Called by Android when it expects a language to be loaded. However, this is not guaranteed
     * to be called before a language is changed. Therefore, all we do is check if the language is
     * downloaded and return a response based on that.
     *
     * @param lang The ISO 3 letter code of the language
     * @param country The ISO code of the country, we ignore this
     * @param variant The variant, we ignore this as well
     * @return Either TextToSpeech.LANG_AVAILABLE or TextToSpeech.LANG_NOT_SUPPORTED depending on
     * whether the voice is found
     */
    override fun onLoadLanguage(lang: String?, country: String?, variant: String?): Int {
//    Log.d(LOG_TAG, "onLoadLanguage: $lang")

        val currentVoices = getCurrentVoices()

        for (voice in currentVoices) {
        // Log.d(LOG_TAG, "onLoadLanguage checking ${voice.locale.isO3Language}, isDownloaded? ${voice.isDownloaded}")

            if (voice.isDownloaded && voice.locale.isO3Language == lang) {
        // Log.d(LOG_TAG, "Known lang $lang called inside onLoadLanguage")

                return TextToSpeech.LANG_AVAILABLE
            }
        }

        return TextToSpeech.LANG_NOT_SUPPORTED
    }

    override fun onLoadVoice(voiceName: String?): Int {
//        Log.d(LOG_TAG, "onLoadVoice: $voiceName")

        if (voiceName in getCurrentVoices().map { h2rVoice -> h2rVoice.id})
            return SUCCESS

        Log.w(LOG_TAG, "onLoadVoice voice not found $voiceName")

        return ERROR
    }


    /**
     * Overrides [TextToSpeechService.onSynthesizeText]
     *
     * Main synthesis function. This takes a [SynthesisRequest] and [SynthesisCallback]. The
     * [SynthesisRequest] contains the text as well as all the relevant parameters including the
     * language. On receiving the request, first checks and loads the language then performs
     * synthesis.
     *
     * This is expected to be a blocking function called from the synthesis thread only.
     *
     * @param request [SynthesisRequest] containing the text and the synthesis parameters such as
     * language
     * @param callback [SynthesisCallback] used to feed the synthesized audio
     */
    override fun onSynthesizeText(request: SynthesisRequest, callback: SynthesisCallback) {
//        Log.d(LOG_TAG, "onSynthesizeText: Hear2Read TTS Service")

        _callback = callback

        val text = request.charSequenceText.toString()
        val rate = request.speechRate / 100f
        val pitch = request.pitch / 100f
        val language = request.language
        val langISO2 = if (language.length==3) iso3ToIso2Map[language] else language
        val country = request.country
        val variant = request.variant

//        Log.d(LOG_TAG, "onSynthesizeText: language: $language")

        val ret = isLanguageAvailable(language, country, variant)

        if (ret == TextToSpeech.LANG_NOT_SUPPORTED) {
            callback.error()
            return
        }

        var h2RVoice: H2RVoice? = null
        val availableVoices = getCurrentVoices().filter{ it.isDownloaded }

        for (voice in availableVoices) {
            if (voice.locale.language == langISO2) {
                h2RVoice = voice
                break
            }
        }

        if (h2RVoice == null) {
            Log.w(LOG_TAG, "No appropriate voice found for synthesis language: $langISO2")
            return
        }

        val loadVoiceReturn: Int = Synthesizer.getOrLoadModel(h2RVoice)

        if (loadVoiceReturn != TextToSpeech.LANG_AVAILABLE) {
            Log.e(LOG_TAG, "onSynthesizeText: unable to load language")
            return
        }

        if (_curLang?.language != h2RVoice.locale.language) {
//            Log.d(LOG_TAG, "Updating _curLang: $_curLang to ${h2RVoice.locale}")
            _curLang = Locale.Builder().setLanguage(h2RVoice.locale.language).build()
        }

        callback.start(h2RVoice.sampleRate, AudioFormat.ENCODING_PCM_16BIT, 1)

        if (text.isBlank() || text.isEmpty()) {
            callback.done()
            return
        }

        Synthesizer.synthesize(text, speed = rate, pitch = pitch, volume = 100, charMode = false )

    }


    /**
     * Logging function to print current dispatcher and thread, to check where the audio callback
     * is being called
     *
     */
//    suspend fun printCurrentDispatcher() {
//        val dispatcher = coroutineContext[ContinuationInterceptor]
//        Log.d(LOG_TAG,"Current Dispatcher is: $dispatcher, current thread: ${Thread.currentThread().name}")
//    }


    /**
     * Override [TextToSpeechService.onDestroy]. Check documentation there. Stops any synthesis
     * pipeline if running, and stops the [ttsServiceScope] to halt TTS initialization
     *
     */
    override fun onDestroy() {
        // TODO?: provide way to close model and espeak in Synthesizer
        Log.d(LOG_TAG, "onDestroy: Hear2Read TTS Service")
        super.onDestroy()
//        _curPipeline.get()?.stop()
        ttsServiceScope.cancel()
        Synthesizer.terminate()
    }


    /**
     * Override function [TextToSpeechService.onGetVoices]. Check documentation there. Note the
     * structure of the voice data there, as opposed to [H2RVoice]
     *
     * @return List of voice data in the format required by Android
     */
    override fun onGetVoices(): MutableList<Voice> {
        // Wait to initialize repo
        runBlocking {
            H2RVoiceRepository.isReady.first { it == true }
        }
        val returnVoices = getCurrentVoices().mapNotNull { h2rVoice ->
             if (h2rVoice.isDownloaded) {
              Voice(
                /* name = */ h2rVoice.id,
                /* locale = */ Locale.Builder()
                    .setLanguage(h2rVoice.locale.isO3Language)   //ISO-3 language code
                    //.setRegion("IN")         // ISO country code (optional)
                    .build(),
                /* quality = */ Voice.QUALITY_HIGH,
                /* latency = */ Voice.LATENCY_NORMAL,
                /* requiresNetworkConnection = */ false,
                /* features = */ setOf()
              )
             }
            else null
        }
//        Log.d(LOG_TAG, "onGetVoices returning: $returnVoices")
        return returnVoices.toMutableList()
    }


    /**
     * Override function [TextToSpeechService.onIsLanguageAvailable]. Check documentation there.
     * Note lang is ISO 3 letter code.
     *
     * @param lang ISO 3 letter code of language
     * @param country ISO code of country
     * @param variant Variant name
     * @return Returns [TextToSpeech.LANG_AVAILABLE] or [TextToSpeech.LANG_NOT_SUPPORTED] depending
     * on whether language is available or not
     */
     override fun onIsLanguageAvailable(lang: String?, country: String?, variant: String?): Int {
//         Log.d(LOG_TAG, "onIsLangAvailable: Hear2Read TTS Service: $lang, $country, $variant")
         return isLanguageAvailable(lang, country, variant)
     }


    /**
     * Helper function implementing functionality of [onIsLanguageAvailable]
     *
     * @param lang ISO 3 letter code of language
     * @param country ISO code of country
     * @param variant Variant name
     * @return Returns [TextToSpeech.LANG_AVAILABLE] or [TextToSpeech.LANG_NOT_SUPPORTED] depending
     * on whether language is available or not
     */
    private fun isLanguageAvailable(lang: String?, country: String?, variant: String?): Int {

        runBlocking {
            H2RVoiceRepository.isReady.first { it == true }
        }

        val availableVoices = getCurrentVoices().filter { it.isDownloaded }

        for (voice in availableVoices) {
//            Log.d(LOG_TAG, "onIsLangAvailable: checking: ${voice.locale.isO3Language}, ${voice.id}")
            if (voice.locale.isO3Language == lang){ // onIsLanguageAvailable is guaranteed to have ISO3
//                Log.i(LOG_TAG, "Known language $lang returned: $country, $variant")
                return TextToSpeech.LANG_AVAILABLE
            }
        }

        return TextToSpeech.LANG_NOT_SUPPORTED
    }


    /**
     * Overriding function [TextToSpeechService.onGetLanguage]. Check documentation there
     *
     * @return Array of [lang ISO3 code, country code, variant]
     */
    override fun onGetLanguage(): Array<String> {
//        Log.d(LOG_TAG, "onGetLanguage")
        return arrayOf(_curLang?.isO3Language ?: "", "", "")
    }


    /**
     * Overriding function [TextToSpeechService.onIsValidVoiceName]. Check documentation there.
     * The name should match [H2RVoice.id]
     *
     * @param voiceName
     * @return [TextToSpeech.SUCCESS] or [TextToSpeech.ERROR] depending on if the voice name is
     * valid or not
     */
    override fun onIsValidVoiceName(voiceName: String?): Int {
//        Log.d(LOG_TAG, "onIsValidVoiceName: $voiceName")
        val availableVoices = getCurrentVoices().filter { it.isDownloaded }
        return if (availableVoices.any { h2RVoice: H2RVoice -> h2RVoice.id == voiceName }) {
//            Log.d(LOG_TAG, "onIsValidVoiceName: $voiceName success")
            TextToSpeech.SUCCESS
        } else {
            TextToSpeech.ERROR
        }
    }


    /**
     * Overriding function [TextToSpeechService.onGetDefaultVoiceNameFor]. Check documentation
     * there. Finds the available [H2RVoice] whose [H2RVoice.locale.isO3Language] matches the lang
     * parameter provided and returns the [H2RVoice.id]
     *
     * @param lang ISO 3 letter code of language
     * @param country ISO code of country
     * @param variant Variant name
     * @return [H2RVoice.id] of voice if found, or empty string if not
     */
    override fun onGetDefaultVoiceNameFor(
        lang: String?,
        country: String?,
        variant: String?
    ): String {
        val availableVoices = getCurrentVoices().filter { it.isDownloaded }
        val iso3Map = availableVoices.associateBy { h2RVoice ->
            h2RVoice.locale.isO3Language
        }
//        Log.d(LOG_TAG, "onGetDefaultVoiceNameFor $lang returning : " +
//                (iso3Map[lang.toString()]?.id ?: ""))

        return iso3Map[lang.toString()]?.id ?: ""
    }


    /**
     * Overriding function [TextToSpeechService.onStop].
     *
     */
    override fun onStop() {
        // TODO: need to stop synthesis somehow
//        Log.d(LOG_TAG, "onStop: Hear2Read TTS Service")
//        _curPipeline.get()?.stop()
//        Synthesizer.terminate()
    }

    /**
     * Implementing interface function for audio callback
     *
     * @param samples
     */
    @Keep
    override fun onAudio(samples: ShortArray) {
//        Log.d(LOG_TAG, "onAudio length ${samples.size}")
        val byteArray = ByteArray(samples.size * Short.SIZE_BYTES)
        for (i in 0 until samples.size) {
            val intVal = samples[i].toInt()

            byteArray[2 * i] = (intVal).toByte()
            byteArray[2 * i + 1] = (intVal shr 8).toByte()
        }
        feedAudioToCallback(byteArray)
    }

    /**
     * Function to feed audio to the [SynthesisCallback] completely.
     *
     * @param audioData The raw audio data in bytes to be fed to the [SynthesisCallback]
     * @return 1 if completed, 0 if interrupted
     */
    private fun feedAudioToCallback(audioData: ByteArray): Int {
//        Log.d(LOG_TAG, "Feeding audio length ${audioData.size}")
        _callback?.let {  }
        val maxBufferSize = _callback?.maxBufferSize?: 0
        var offset = 0

        while (offset < audioData.size) {
            val chunkSize = minOf(maxBufferSize, audioData.size - offset)
//            Log.d(LOG_TAG, "Feeding chunk at $offset, chunk $chunkSize")
            val written = _callback?.audioAvailable(
                audioData,
                offset,
                chunkSize
            )?:-1
            if (written < 0) return 0
            offset += chunkSize
        }
//        Log.d(LOG_TAG, "Exiting feedAudioCallback, offset: $offset, ")

        return 1
    }

    /**
     * Callback function when synthesis is completed
     *
     */
    @Keep
    override fun onDone() {
//        Log.d(LOG_TAG, "onDone")
        _callback?.done()
    }

    /**
     * Callback function when index is reached
     *
     * @param index
     */
    @Keep
    override fun onIndexReached(index: Int) {
//        TODO("Not yet implemented")
//        Log.d(LOG_TAG, "onIndexReached $index")
    }

}