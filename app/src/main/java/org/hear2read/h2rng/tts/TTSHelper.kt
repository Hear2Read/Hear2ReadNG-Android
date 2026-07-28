package org.hear2read.h2rng.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import org.hear2read.h2rng.R
import org.hear2read.h2rng.data.H2RVoice
import java.util.*

/**
 * Constant containing the name of the Hear2ReadNG TTS engine in Android. Used to initialize the
 * [TextToSpeech] instance
 */
val TTS_NAME = "org.hear2read.h2rng"

/**
 * Constant string to mark the utterance used to load the language onto the [TextToSpeech] instance
 */
val INIT_UTT_ID = "INIT_UTTERANCE"

// Setting up the Log tag for Logging
private val FILENAME = "TTSHelper"
private val LOG_TAG = "H2RNG_$FILENAME"

// Needs to be updated on new language beyond list

val iso3ToIso2Map = mapOf(
    "asm" to "as", // Assamese
    "ben" to "bn", // Bengali
    "guj" to "gu", // Gujarati
    "hin" to "hi", // Hindi
    "kan" to "kn", // Kannada
    "mal" to "ml", // Malayalam
    "mar" to "mr", // Marathi
    "nep" to "ne", // Nepali
    "ori" to "or", // Odia
    "pan" to "pa", // Punjabi (Gurmukhi)
    "san" to "sa", // Sanskrit
    "sin" to "si", // Sinhala
    "tel" to "te", // Telugu
    "tam" to "ta", // Tamil
    "eng" to "en", // English
)

val iso2ToIso3Map = mapOf(
    "hi" to "hin", // Hindi
    "kn" to "kan", // Kannada
    "te" to "tel", // Telugu
    "ta" to "tam", // Tamil
    "ml" to "mal", // Malayalam
    "gu" to "guj", // Gujarati
    "pa" to "pan", // Punjabi (Gurmukhi)
    "or" to "ori", // Odia
    "bn" to "ben", // Bengali
    "mr" to "mar", // Marathi
    "as" to "asm", // Assamese
    "sa" to "san", // Sanskrit
    "si" to "sin", // Sinhala
    "ne" to "nep", // Nepali
    "en" to "eng"  // English
)

/**
 * TODO: Unused, remove
 * Data class holding parameters relevant to synthesis
 *
 * @property h2RVoice [H2RVoice] object corresponding to relevant voice
 * @property phonemeLength Parameter used to control speech rate. Inverse to rate.
 * @property amplitude Amplitude of output audio. Currently not being modified
 */
data class SynthParams(
    val h2RVoice: H2RVoice,
    val phonemeLength: Float,
    val amplitude: Int = 100
)

/**
 * TODO: Moved to MainViewModel, remove
 * Initialize Hear2ReadNG [TextToSpeech] instance for use in the [TTSDemoScreen]. The instance is
 * almost guaranteed to have already been instantiated when the [MainActivity] starts up, and this
 * function is used to ensure that the voice corresponding to the language is loaded before the
 * demo is active. The initialization is done by setting the parameters sent to the [TextToSpeech]
 * has the Locale of the language and sending an empty string to it to speak.
 *
 * @param context [Context] passed to instantiate the [TextToSpeech] as a fallback in case it is
 * not already instantiated
 * @param langLocale [Locale] of the voice to be loaded
 * @param getTTS Function to fetch the single TTS instance used across the MainActivity
 * @param setTTS Function to set the single TTS instance in case of the fallback case where it has
 * not been instantiated
 * @param onInitialized Function called when the UtteranceProgressListener returns after TTS has
 * finished processing the initialization text, when the voice has been confirmed as loaded
 * @param onError Function called when the UtteranceProgressListener returns an error
 */
//fun initializeTTS(
//    context: Context,
//    langLocale: Locale? = null,
//    getTTS: () -> TextToSpeech?,
//    setTTS: (TextToSpeech?) -> Unit,
//    onInitialized: () -> Unit,
//    onError: () -> Unit
//) {
//    // If TTS already exists and is for the same language, reuse it
//    val existingTTS = getTTS()
//    existingTTS?.let { tts ->
//        Log.d(LOG_TAG, "TTS exists, checking if voice needs loading")
//        // Run voice loading code only if languageCode is provided
//        langLocale?.let { lang ->
////            val locale = getLocaleForLanguage(lang)
//            val result = tts.setLanguage(lang)
//
//            if (result == TextToSpeech.LANG_MISSING_DATA ||
//                result == TextToSpeech.LANG_NOT_SUPPORTED
//            ) {
//                // TODO return onInitialized in this case as well, modify with taking a bool arg
//                onError()
//            } else {
//                tts.setSpeechRate(1.0f)
//                tts.setPitch(1.0f)
//                setupUtteranceListener(tts, onInitialized)
////                val dummySentence = getDummySentence(lang)
//                val dummySentence = ""
//                tts.speak(dummySentence, TextToSpeech.QUEUE_FLUSH, null, "INIT_UTTERANCE")
//            }
//            return
//        }
//        // Callback since nothing needs to be done
//        onInitialized()
//        return
//    }
//
//    // TODO: the tts should already be initialized here and this code should never run
//    // Use var to allow assignment before the lambda completes
//    var ttsInstance: TextToSpeech? = null
//
//    ttsInstance = TextToSpeech(context, { status ->
//        if (status == TextToSpeech.SUCCESS) {
//            ttsInstance?.let tts@{ tts ->
//                // Run voice loading code only if languageCode is provided
//                if (langLocale == null) {
//                    setTTS(tts)
//                    onInitialized()
//                    return@tts
//                }
//                else {
////                    val locale = getLocaleForLanguage(langLocale)
//                    val result = tts.setLanguage(langLocale)
//
//                    if (result == TextToSpeech.LANG_MISSING_DATA ||
//                        result == TextToSpeech.LANG_NOT_SUPPORTED
//                    ) {
//                        onError()
//                    } else {
//                        setTTS(tts)
//                        setupUtteranceListener(tts, onInitialized)
//
//                        // Send dummy sentence to load the voice
////                        val dummySentence = getDummySentence(lang)
//                        val dummySentence = ""
//                        tts.speak(dummySentence, TextToSpeech.QUEUE_FLUSH, null, "INIT_UTTERANCE")
//                    }
//                    return@tts
//                }
//            }
//        } else {
//            onError()
//        }
//    }, TTS_NAME)
//}

/**
 * TODO: Moved, remove
 * Helper to set [UtteranceProgressListener] for the tts which invokes a callback function when an
 * utterance with the ID matching [INIT_UTT_ID] is processed.
 *
 * @param tts The TextToSpeech instance on which the UtteranceProgressListener needs to be set
 * @param onInitComplete The function to be called when the utterance with the ID matching
 * [INIT_UTT_ID] is processed
 */
//fun setupUtteranceListener(tts: TextToSpeech, onInitComplete: (() -> Unit)? = null) {
//    tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
//        override fun onStart(utteranceId: String?) {
//            if (utteranceId == INIT_UTT_ID) {
//                onInitComplete?.invoke()
//            }
//        }
//
//        override fun onDone(utteranceId: String?) {
//            // Handle sentence completion
//        }
//
//        @Deprecated("Deprecated in Java")
//        override fun onError(utteranceId: String?) {
//            // TODO: Handle error
//        }
//    })
//}


/**
 * TODO: Deprecated, remove
 * Helper function to play a sentence in the [TTSDemoScreen]. This takes the index of the sentence
 * and matches it on return from the [UtteranceProgressListener]. It also takes callback functions
 * which are called when the synthesised audio becomes available, as well as when the playback of
 * the audio is completed. This will be used to get the response time and to queue the next
 * sentence respectively.
 *
 * @param tts The [TextToSpeech] instance which will be used to play the sentence
 * @param sentence The sentence to be played back
 * @param index Index of the sentence in the TTSDemoScreen
 * @param onStart Function to be called once the TTS engine returns synthesised audio
 * @param onComplete Function to be called once audio playback is completed
 */
//fun playSentence(
//    tts: TextToSpeech?,
//    sentence: String,
//    index: Int,
//    onStart: () -> Unit,
//    onComplete: () -> Unit
//) {
//    val utteranceId = "SENTENCE_$index"
//
//    tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
//        override fun onAudioAvailable(uttId: String?, audio: ByteArray?) {
//            super.onAudioAvailable(utteranceId, audio)
//            if (uttId.toString() == utteranceId) {
//                onStart()
//            }
//
//        }
//        // unused as this is called well in advance of audio being received
//        override fun onStart(uttId: String?) {
//            if (uttId == utteranceId) {
//                //onStart()
//            }
//        }
//
//        override fun onDone(uttId: String?) {
//            if (uttId == utteranceId) {
//                onComplete()
//            }
//        }
//
//        @Deprecated("Deprecated in Java")
//        override fun onError(uttId: String?) {
//            onComplete()
//        }
//    })
//
//    tts?.speak(sentence, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
//}


/**
 * TODO: deprecated, remove
 * Get sample text. This is a single sentence which contains text in the specified language. The
 * content of the text is roughly "This is an example of speech in <language>", where language is
 * the language name. It currently uses reflection based deprecated code to access the correct text
 * from the resources. This needs to be replaced with hardcoded conditionals
 *
 * @param context [Context] passed to access the app resources
 * @param languageCode ISO 3 letter code of the language
 * @return String of the sample text
 */
//fun getSampleText(context: Context, languageCode: String): String {
////    val resourceName = "${languageCode}_sample"
////    Log.d(LOG_TAG, "Resource name: $resourceName")
//
//    val sampleText = when (languageCode) {
//        "asm" -> context.resources.getString(R.string.asm_sample)
//        "ben" -> context.resources.getString(R.string.ben_sample)
//        "guj" -> context.resources.getString(R.string.guj_sample)
//        "hin" -> context.resources.getString(R.string.hin_sample)
//        "kan" -> context.resources.getString(R.string.kan_sample)
//        "mal" -> context.resources.getString(R.string.mal_sample)
//        "mar" -> context.resources.getString(R.string.mar_sample)
//        "nep" -> context.resources.getString(R.string.nep_sample)
//        "ori" -> context.resources.getString(R.string.ori_sample)
//        "pan" -> context.resources.getString(R.string.pan_sample)
//        "san" -> context.resources.getString(R.string.san_sample)
//        "sin" -> context.resources.getString(R.string.sin_sample)
//        "tam" -> context.resources.getString(R.string.tam_sample)
//        "tel" -> context.resources.getString(R.string.tel_sample)
//        "eng" -> context.resources.getString(R.string.eng_sample)
//        else -> "Unknown language"
//    }
////    val resId = context.resources.getIdentifier(resourceName, "string", context.packageName)
////    val sampleText = if (resId != 0) context.getString(resId) else "Some random sample text."
//    return sampleText
//}
/**
 * Get sample text ID. This is a single sentence which contains text in the specified language. The
 * content of the text is roughly "This is an example of speech in <language>", where language is
 * the language name. It only returns the resource ID, the caller will use the context available to
 * resolve it
 *
 * @param languageCode ISO 3 letter code of the language
 * @return ID of the sample text if valid, else null
 */
fun getSampleTextID(languageCode: String): Int {
//    val resourceName = "${languageCode}_sample"
//    Log.d(LOG_TAG, "Resource name: $resourceName")

    val sampleText = when (languageCode) {
        "asm" -> R.string.asm_sample
        "ben" -> R.string.ben_sample
        "guj" -> R.string.guj_sample
        "hin" -> R.string.hin_sample
        "kan" -> R.string.kan_sample
        "mal" -> R.string.mal_sample
        "mar" -> R.string.mar_sample
        "nep" -> R.string.nep_sample
        "ori" -> R.string.ori_sample
        "pan" -> R.string.pan_sample
        "san" -> R.string.san_sample
        "sin" -> R.string.sin_sample
        "tam" -> R.string.tam_sample
        "tel" -> R.string.tel_sample
        "eng" -> R.string.eng_sample
        else -> R.string.unk_sample
    }
//    val resId = context.resources.getIdentifier(resourceName, "string", context.packageName)
//    val sampleText = if (resId != 0) context.getString(resId) else "Some random sample text."
    return sampleText
}

/**
 * Get demo text. This is a short paragraph which contains text in the specified language. The
 * content of the text is usually the first paragraph of the language's article in the Wikipedia of
 * the language. It currently uses reflection based deprecated code to access the correct text
 * from the resources. This needs to be replaced with hardcoded conditionals
 * TODO: Currently unused, may be used in the future
 *
 * @param context [Context] passed to access the app resources
 * @param languageCode ISO 3 letter code of the language
 * @return String of the demo text
 */
//fun getDemoText(context: Context, languageCode: String): String {
////    val resourceName = "${languageCode}_demo"
////    Log.d(LOG_TAG, "Resource name: $resourceName")
//
//    val sampleText = when (languageCode) {
//        "asm" -> context.resources.getString(R.string.asm_demo)
//        "ben" -> context.resources.getString(R.string.ben_demo)
//        "guj" -> context.resources.getString(R.string.guj_demo)
//        "hin" -> context.resources.getString(R.string.hin_demo)
//        "kan" -> context.resources.getString(R.string.kan_demo)
//        "mal" -> context.resources.getString(R.string.mal_demo)
//        "mar" -> context.resources.getString(R.string.mar_demo)
//        "nep" -> context.resources.getString(R.string.nep_demo)
//        "ori" -> context.resources.getString(R.string.ori_demo)
//        "pan" -> context.resources.getString(R.string.pan_demo)
//        "san" -> context.resources.getString(R.string.san_demo)
////        "sin" -> context.resources.getString(R.string.sin_demo)
//        "tel" -> context.resources.getString(R.string.tel_demo)
//        "tam" -> context.resources.getString(R.string.tam_demo)
////        "eng" -> context.resources.getString(R.string.eng_demo)
//        else -> "Unknown language"
//    }
////    val resId = context.resources.getIdentifier(resourceName, "string", context.packageName)
////    val sampleText = if (resId != 0) context.getString(resId) else "Some random sample text."
//    return sampleText
//}