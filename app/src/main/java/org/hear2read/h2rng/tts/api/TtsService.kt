/*
* This file is contributed to Hear2Read's Android App Development project
*
* Author: Nikhat Singla
* Date: June 2025
*/

package org.hear2read.h2rng.tts.api

import android.media.AudioFormat
import android.speech.tts.SynthesisCallback
import android.speech.tts.SynthesisRequest
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeechService
import android.speech.tts.Voice
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.hear2read.h2rng.Hear2ReadNGApplication
import org.hear2read.h2rng.data.H2RVoice
import org.hear2read.h2rng.data.H2RVoiceRepository.getCurrentVoices
import org.hear2read.h2rng.Synthesizer
import org.hear2read.h2rng.tts.iso3ToIso2Map
import org.hear2read.h2rng.utils.copyDataDir
import java.util.Locale

/**
 * [TextToSpeechService] implementation for the Hear2ReadNG TTS synthesiser
 *
 */
class TtsService : TextToSpeechService() {
    private val LOG_TAG = "H2RNG_" + TtsService::class.simpleName
//    private val _timesource = TimeSource.Monotonic

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

    /**
     * onCreate checks whether the H2RVoiceRepository has been initialized and the download states
     * synced before continuing. Also ensures that the espeak data dir has been copied.
     *
     */
    override fun onCreate() {
        Log.i(LOG_TAG, "onCreate: Hear2Read TTS Service")
        super.onCreate()

        val app = application as Hear2ReadNGApplication

        ttsServiceScope.launch {
            /*
             Haven't found a way to do this in a cleaner way, and not having the repository
             initialized means that the TTS won't return the correct available voices
             */
            while (!app.isVoiceRepositoryReady()) {
                delay(50)
            }
        }

//        assetPackManager = AssetPackManagerFactory.getInstance(application)
//        runBlocking{ populateSizes(assetPackManager) }
//        populateSizes()

        // TODO!: check if this is being copied each time
        Synthesizer.initeSpeak(copyDataDir(this, "espeak-ng-data"))
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

        Log.d(LOG_TAG, "onLoadLanguage: $lang, $country, $variant")

//        val langISO2 = if (lang?.length==3) iso3ToIso2Map[lang] else lang

//        val availableLanguages = runBlocking { getAvailableVoices() }

        val currentVoices = getCurrentVoices()

        for (voice in currentVoices) {
//            val status by voice.status
            Log.d(LOG_TAG, "onLoadLanguage checking ${voice.locale.isO3Language}, isDownloaded? ${voice.isDownloaded}")

            if (voice.isDownloaded && voice.locale.isO3Language == lang) {
                Log.d(LOG_TAG, "Known lang $lang called inside onLoadLanguage")

                return TextToSpeech.LANG_AVAILABLE
            }
        }

        return TextToSpeech.LANG_NOT_SUPPORTED
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

        // TODO: implement parameters such as rate, amplitude from SynthesisRequest
        val text = request.charSequenceText.toString()
        val rate = request.speechRate
        val language = request.language
        val langISO2 = if (language.length==3) iso3ToIso2Map[language] else language
        val country = request.country
        val variant = request.variant

        val ret = onIsLanguageAvailable(language, country, variant)

        if (ret == TextToSpeech.LANG_NOT_SUPPORTED) {
            callback.error()
            return
        }

        var h2RVoice: H2RVoice? = null
//        val availableVoices = runBlocking { getAvailableVoices() }
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

        // TODO!: Clean up unused code in next release
//        val assetPackLocation = assetPackManager.getPackLocation(h2RVoice.assetID) ?:  run {
//            Log.e(LOG_TAG, "getPackLocation returned null")
//            return
//        }

//        Log.d(LOG_TAG, "Synthesis Voice found ${h2RVoice.id}")

//        runBlocking {
//            assetPackLocation.assetsPath()?.let{ loadVoiceReturn =
//                Synthesizer.getOrLoadModel(it, foundH2RVoice)}
//
//        }

//        assetPackLocation.assetsPath()?.let{
//            loadVoiceReturn = Synthesizer.getOrLoadModel(foundH2RVoice)
//        }

        val loadVoiceReturn: Int = Synthesizer.getOrLoadModel(h2RVoice)

        if (loadVoiceReturn != TextToSpeech.LANG_AVAILABLE) {
            Log.e(LOG_TAG, "onSynthesizeText: unable to load language")
            return
        }

        if (_curLang?.language != h2RVoice.locale.language) {
            Log.d(LOG_TAG, "Updating _curLang: $_curLang to ${h2RVoice.locale}")
            _curLang = Locale.Builder().setLanguage(h2RVoice.locale.language).build()
        }

        // TODO?: fix sample rate
        callback.start(h2RVoice.sampleRate, AudioFormat.ENCODING_PCM_16BIT, 1)

        if (text.isBlank() || text.isEmpty()) {
            callback.done()
            return
        }

//        val ttsCallback: (ShortArray) -> Int = fun(shortArray: ShortArray): Int {
//            // convert FloatArray to ByteArray
//            val samples = shortArrayToByteArray(shortArray)
//            val maxBufferSize: Int = callback.maxBufferSize
//            var offset = 0
//            while (offset < samples.size) {
//                val bytesToWrite = maxBufferSize.coerceAtMost(samples.size - offset)
//                callback.audioAvailable(samples, offset, bytesToWrite)
//                offset += bytesToWrite
//            }
//
//            // 1 means to continue
//            // 0 means to stop
//            return 1
//        }

        val audioCallback: (ByteArray) -> Int = fun(byteArray: ByteArray): Int {
            // convert FloatArray to ByteArray
//            val samples = shortArrayToByteArray(shortArray)
            val maxBufferSize: Int = callback.maxBufferSize
//            Log.d(LOG_TAG, "maxBufferSize: $maxBufferSize")
            var offset = 0
            while (offset < byteArray.size) {
                val bytesToWrite = maxBufferSize.coerceAtMost(byteArray.size - offset)
                callback.audioAvailable(byteArray, offset, bytesToWrite)
                offset += bytesToWrite
            }

            // 1 means to continue
            // 0 means to stop
            return 1
        }

//        val assetPackPath = assetPackLocation.assetsPath() ?: return

//        runBlocking {
//            Synthesizer.speak(text, foundH2RVoice, 50, 100, assetPackPath, audioCallback)?.join()
//        }

        Synthesizer.speak(text, h2RVoice, rate, 100, audioCallback)

        callback.done()
    }

    override fun onDestroy() {
        // TODO?: provide way to close model and espeak in Synthesizer
        Log.d(LOG_TAG, "onDestroy: Hear2Read TTS Service")
        super.onDestroy()
        ttsServiceScope.cancel()
    }

    override fun onGetVoices(): MutableList<Voice> {
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
        Log.d(LOG_TAG, "onGetVoices returning: $returnVoices")
        return returnVoices.toMutableList()
    }

     override fun onIsLanguageAvailable(lang: String?, country: String?, variant: String?): Int {
        Log.d(LOG_TAG, "onIsLangAvailable: Hear2Read TTS Service: $lang, $country, $variant")

         val availableVoices = getCurrentVoices().filter { it.isDownloaded }

         for (voice in availableVoices) {
             Log.d(LOG_TAG, "onIsLangAvailable: checking: ${voice.locale.isO3Language}, ${voice.id}")
             if (voice.locale.isO3Language == lang){ // onIsLanguageAvailable is guaranteed to have ISO3
                 Log.i(LOG_TAG, "Known language $lang returned: $country, $variant")
                 return TextToSpeech.LANG_AVAILABLE
             }
         }

         return TextToSpeech.LANG_NOT_SUPPORTED
         /*
         return if (h2RVoices.any { h2RVoice ->
         h2RVoice.locale.isO3Language == lang
         }) TextToSpeech.LANG_AVAILABLE else TextToSpeech.LANG_NOT_SUPPORTED
         */
     }

    override fun onGetLanguage(): Array<String> {
        Log.d(LOG_TAG, "onGetLanguage")
        return arrayOf(_curLang?.isO3Language ?: "", "", "")
    }

    override fun onIsValidVoiceName(voiceName: String?): Int {
        Log.d(LOG_TAG, "onIsValidVoiceName: $voiceName")
        val availableVoices = getCurrentVoices().filter { it.isDownloaded }
        return if (availableVoices.any { h2RVoice: H2RVoice -> h2RVoice.id == voiceName }) {
            Log.d(LOG_TAG, "onIsValidVoiceName: $voiceName success")
            TextToSpeech.SUCCESS
        } else {
            TextToSpeech.ERROR
        }
    }

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
                (iso3Map[lang.toString()]?.id ?: "")
        )
        return iso3Map[lang.toString()]?.id ?: ""
    }

    override fun onStop() {
        Log.d(LOG_TAG, "onStop: Hear2Read TTS Service")
//        Synthesizer.termeSpeak()
        // TODO: implement close model feature in Synthesizer
    }

    /**
     * UNUSED!
     *
     * Converts a given short array (parameter 'audio') to a byte array using Little-Endian format.
     * New [ByteArray] is twice the length of the [ShortArray] (since 1 Short requires 2 Bytes)
     *
     * @param audio [ShortArray] of the audio
     * @return Converted [ByteArray] of the audio
     *
     */
//    private fun shortArrayToByteArray(audio: ShortArray): ByteArray {
//        // byteArray is actually a ShortArray
//        val byteArray = ByteArray(audio.size * 2)
//        for (i in audio.indices) {
//            val sample = audio[i].toInt()
//            byteArray[2 * i] = sample.toByte()
//            byteArray[2 * i + 1] = (sample shr 8).toByte()
//        }
//        return byteArray
//    }
}