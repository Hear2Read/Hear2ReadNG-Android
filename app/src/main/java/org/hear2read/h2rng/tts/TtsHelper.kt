package org.hear2read.h2rng.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.CompletableDeferred
import org.hear2read.h2rng.R
import org.hear2read.h2rng.data.H2RVoice
import java.io.ByteArrayOutputStream
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

class AsyncSynthesisTask(
    val deferred: CompletableDeferred<ByteArray>,
    val buffer: ByteArrayOutputStream = ByteArrayOutputStream()
)

data class TextChunk(
    val text: String,
    val isEnglish: Boolean
)

/***
 * Split text into native and English chunks, if mixed. Each chunk is also labelled if English or
 * not
 *
 * @param text Text to be split
 * @return List of TextChunk's created from the text
 */
fun splitTextByLanguage(text: String): List<TextChunk> {
    // 1. check if ascii
    // 2. process_mised_text()
    //      1. txts = (regex: re.compile("([a-zA-Z]+)", re.ASCII)).split
    //      2. for txt in txts: isascii? append
    //      3. no ascii: _process_non_native_unicode
    // 3. _process_non_native_unicode:
    //      1. complex per char check for number handling?
    //      2. need script range checking
    // 4. merge consequents
    return listOf()
}

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