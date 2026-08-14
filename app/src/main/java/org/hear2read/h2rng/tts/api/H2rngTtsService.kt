/*
* This file is contributed to Hear2Read's Android App Development project
*
* Author: Nikhat Singla
* Date: June 2025
*/

package org.hear2read.h2rng.tts.api

import android.speech.tts.SynthesisCallback
import android.speech.tts.SynthesisRequest
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.ERROR
import android.speech.tts.TextToSpeech.SUCCESS
import android.speech.tts.TextToSpeechService
import android.speech.tts.Voice
import android.util.Log
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
import org.hear2read.h2rng.tts.AndroidAudioOutput
import org.hear2read.h2rng.tts.EnglishTtsAdapter
import org.hear2read.h2rng.tts.H2rngTtsAdapter
import org.hear2read.h2rng.tts.SpeechParams
import org.hear2read.h2rng.tts.Synthesizer
import org.hear2read.h2rng.utils.TextSplitter
import org.hear2read.h2rng.tts.TtsPipeline
import org.hear2read.h2rng.tts.indicUnicodeRanges
import org.hear2read.h2rng.tts.iso3ToIso2Map
import org.hear2read.h2rng.utils.LOG_PREFIX
import java.util.Locale

/**
 * [TextToSpeechService] implementation for the Hear2ReadNG TTS synthesiser
 *
 */
class H2rngTtsService : TextToSpeechService() {
    private val LOG_TAG = LOG_PREFIX + H2rngTtsService::class.simpleName

    /**
     * Service scope used to wait for the [H2RVoiceRepository] to initialize
     */
    private val ttsServiceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /**
     * Stores the Locale of the current voice with the ISO 2 letter code of the language
     */
    private var curLang: Locale? = null


    // Variables used for the mixed text generation pipeline
    private lateinit var pipeline: TtsPipeline
    private lateinit var engEngine: EnglishTtsAdapter
    private lateinit var nativeEngine: H2rngTtsAdapter



    /**
     * onCreate first checks whether the H2RVoiceRepository has been initialized and the download
     * states synced before continuing. Then, it sets up the pipeline to generate mixed text, as
     * well as the engines that handle native and English text.
     *
     */
    override fun onCreate() {
        Log.i(LOG_TAG, "onCreate: Hear2Read TTS Service")
        super.onCreate()

        ttsServiceScope.launch {
            // Wait for voice repository to initialize
            H2RVoiceRepository.isReady.first { it == true }
        }

        // Will update to be configurable later
        val englishTtsPackage = "com.google.android.tts"
        engEngine = EnglishTtsAdapter(applicationContext, englishTtsPackage)
        nativeEngine = H2rngTtsAdapter(ttsServiceScope, filesDir.absolutePath)

        pipeline = TtsPipeline(
            englishEngine = engEngine,
            nativeEngine = nativeEngine,
            textSplitter = TextSplitter
        )
    }

    /**
     * Overrides [TextToSpeechService.onSynthesizeText]
     *
     * Main synthesis function. This takes a [SynthesisRequest] and [SynthesisCallback]. The
     * [SynthesisRequest] contains the text as well as all the relevant parameters including the
     * language. On receiving the request, first checks and loads the language then performs
     * synthesis. Synthesis is delegated to the [TtsPipeline] which takes care of mixed text
     * synthesis.
     *
     * This is expected to be a blocking function called from the synthesis thread only.
     *
     * @param request [SynthesisRequest] containing the text and the synthesis parameters such as
     * language
     * @param callback [SynthesisCallback] used to feed the synthesized audio
     */
    override fun onSynthesizeText(request: SynthesisRequest, callback: SynthesisCallback) {
        Log.d(LOG_TAG, "onSynthesizeText: language: ${request.language}, text: ${request.charSequenceText}")

        val androidOutput = AndroidAudioOutput(callback)
        val engSamplerate = engEngine.getSampleRate()

        val params = SpeechParams(
            speed = request.speechRate / 100f,
            pitch = request.pitch / 100f,
            volume = 100,
            charMode = false,
            sampleRate = engSamplerate
        )

        val text = request.charSequenceText.toString()
        val language = request.language

//        Log.d(LOG_TAG, "onSynthesizeText: language: $language")

        // Ensure the right voice is loaded
        if (prepareVoice(language) == TextToSpeech.LANG_NOT_SUPPORTED)
            return

        val nativeRange = indicUnicodeRanges[curLang?.isO3Language] ?: 0x0A00..0x0A7F

        // Wait till English Tts is ready
        runBlocking{ engEngine.waitForWarmup() }

        pipeline.processRequest(text, androidOutput, nativeRange, params)
    }

    /***
     * Check for setting correct voice for synthesis.
     * TODO: Seems like there are many redundancies, correct
     *
     * @param language the ISO code of the language to check the voice
     * @return Returns [TextToSpeech.LANG_AVAILABLE] or [TextToSpeech.LANG_NOT_SUPPORTED] depending
     * on whether language is available or not
     */
    private fun prepareVoice(language: String): Int {

        val langISO2 = if (language.length==3) iso3ToIso2Map[language] else language

        // English is always supported through alternate TTS
        if (langISO2 == "en") return TextToSpeech.LANG_AVAILABLE

        if (language.length==3) {
            val ret = isLanguageAvailable(language)

            if (ret == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.w(LOG_TAG, "isLanguageAvailable failed for: $language")
                return ret
            }
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
            return TextToSpeech.LANG_NOT_SUPPORTED
        }

        val loadVoiceReturn: Int = Synthesizer.getOrLoadModel(h2RVoice)

        if (loadVoiceReturn != TextToSpeech.LANG_AVAILABLE) {
            Log.e(LOG_TAG, "onSynthesizeText: unable to load language")
            return TextToSpeech.LANG_NOT_SUPPORTED
        }

        if (curLang?.language != h2RVoice.locale.language) {
//            Log.d(LOG_TAG, "Updating _curLang: $_curLang to ${h2RVoice.locale}")
            curLang = Locale.Builder().setLanguage(h2RVoice.locale.language).build()
        }

        return TextToSpeech.LANG_AVAILABLE
    }

    /**
     * Overriding function [TextToSpeechService.onStop].
     *
     */
    override fun onStop() {
        Log.d(LOG_TAG, "onStop: Hear2Read TTS Service")
        engEngine.stop()
        nativeEngine.stop()
    }

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
        engEngine.shutdown()
        nativeEngine.shutdown()
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
        Log.d(LOG_TAG, "onLoadLanguage: $lang")

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
        Log.d(LOG_TAG, "onLoadVoice: $voiceName")

        if (voiceName in getCurrentVoices().map { h2rVoice -> h2rVoice.id})
            return SUCCESS

        Log.w(LOG_TAG, "onLoadVoice voice not found $voiceName")

        return ERROR
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
         return isLanguageAvailable(lang)
     }

    /**
     * Helper function implementing functionality of [onIsLanguageAvailable]
     *
     * @param lang ISO 3 letter code of language
     * @return Returns [TextToSpeech.LANG_AVAILABLE] or [TextToSpeech.LANG_NOT_SUPPORTED] depending
     * on whether language is available or not
     */
    private fun isLanguageAvailable(lang: String?): Int {

//        if (lang == "eng") return TextToSpeech.LANG_AVAILABLE

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
        return arrayOf(curLang?.isO3Language ?: "", "", "")
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
        Log.d(LOG_TAG, "onGetDefaultVoiceNameFor $lang returning : " +
                (iso3Map[lang.toString()]?.id ?: ""))

        return iso3Map[lang.toString()]?.id ?: ""
    }
}