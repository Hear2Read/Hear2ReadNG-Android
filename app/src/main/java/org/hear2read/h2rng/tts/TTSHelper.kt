package org.hear2read.h2rng.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
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
    "hin" to "hi", // Hindi
    "kan" to "kn", // Kannada
    "tel" to "te", // Telugu
    "tam" to "ta", // Tamil
    "mal" to "ml", // Malayalam
    "guj" to "gu", // Gujarati
    "pan" to "pa", // Punjabi (Gurmukhi)
    "ori" to "or", // Odia
    "ben" to "bn", // Bengali
    "mar" to "mr", // Marathi
    "asm" to "as", // Assamese
    "sin" to "si", // Sinhala
    "nep" to "ne", // Nepali
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
    "si" to "sin", // Sinhala
    "ne" to "nep", // Nepali
    "en" to "eng"  // English
)

/**
 * Initialize Hear2ReadNG [TextToSpeech] instance for use in the [TTSDemoScreen]. The instance is
 * almost guaranteed to have already been instantiated when the [MainActivity] starts up, and this
 * function is used to ensure that the voice corresponding to the language is loaded before the
 * demo is active. The initialization is done by setting the parameters sent to the [TextToSpeech]
 * has the Locale of the language and sending an empty string to it to speak.
 * TODO: Remove the instantiation fallback after ensuring it is unnecessary
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
 * TODO: currently only called when language not found in the setLanguage() method of the TTS
 */
fun initializeTTS(
    context: Context,
    langLocale: Locale? = null,
    getTTS: () -> TextToSpeech?,
    setTTS: (TextToSpeech?) -> Unit,
    onInitialized: () -> Unit,
    onError: () -> Unit
) {
    // If TTS already exists and is for the same language, reuse it
    val existingTTS = getTTS()
    existingTTS?.let { tts ->
        Log.d(LOG_TAG, "TTS exists, checking if voice needs loading")
        // Run voice loading code only if languageCode is provided
        langLocale?.let { lang ->
//            val locale = getLocaleForLanguage(lang)
            val result = tts.setLanguage(lang)

            if (result == TextToSpeech.LANG_MISSING_DATA ||
                result == TextToSpeech.LANG_NOT_SUPPORTED
            ) {
                // TODO return onInitialized in this case as well, modify with taking a bool arg
                onError()
            } else {
                setupUtteranceListener(tts, onInitialized)
//                val dummySentence = getDummySentence(lang)
                val dummySentence = ""
                tts.speak(dummySentence, TextToSpeech.QUEUE_FLUSH, null, "INIT_UTTERANCE")
            }
            return
        }
        // Callback since nothing needs to be done
        onInitialized()
        return
    }

    // TODO: the tts should already be initialized here and this code should never run
    // Use var to allow assignment before the lambda completes
    var ttsInstance: TextToSpeech? = null

    ttsInstance = TextToSpeech(context, { status ->
        if (status == TextToSpeech.SUCCESS) {
            ttsInstance?.let tts@{ tts ->
                // Run voice loading code only if languageCode is provided
                if (langLocale == null) {
                    setTTS(tts)
                    onInitialized()
                    return@tts
                }
                else {
//                    val locale = getLocaleForLanguage(langLocale)
                    val result = tts.setLanguage(langLocale)

                    if (result == TextToSpeech.LANG_MISSING_DATA ||
                        result == TextToSpeech.LANG_NOT_SUPPORTED
                    ) {
                        onError()
                    } else {
                        setTTS(tts)
                        setupUtteranceListener(tts, onInitialized)

                        // Send dummy sentence to load the voice
//                        val dummySentence = getDummySentence(lang)
                        val dummySentence = ""
                        tts.speak(dummySentence, TextToSpeech.QUEUE_FLUSH, null, "INIT_UTTERANCE")
                    }
                    return@tts
                }
            }
        } else {
            onError()
        }
    }, TTS_NAME)
}

/**
 * Helper to set [UtteranceProgressListener] for the tts which invokes a callback function when an
 * utterance with the ID matching [INIT_UTT_ID] is processed.
 *
 * @param tts The TextToSpeech instance on which the UtteranceProgressListener needs to be set
 * @param onInitComplete The function to be called when the utterance with the ID matching
 * [INIT_UTT_ID] is processed
 */
fun setupUtteranceListener(tts: TextToSpeech, onInitComplete: (() -> Unit)? = null) {
    tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) {
            if (utteranceId == INIT_UTT_ID) {
                onInitComplete?.invoke()
            }
        }

        override fun onDone(utteranceId: String?) {
            // Handle sentence completion
        }

        override fun onError(utteranceId: String?) {
            // TODO: Handle error
        }
    })
}


/**
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
fun playSentence(
    tts: TextToSpeech?,
    sentence: String,
    index: Int,
    onStart: () -> Unit,
    onComplete: () -> Unit
) {
    val utteranceId = "SENTENCE_$index"

    tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
        override fun onAudioAvailable(uttId: String?, audio: ByteArray?) {
            super.onAudioAvailable(utteranceId, audio)
            if (uttId.toString() == utteranceId) {
                onStart()
            }

        }
        // unused as this is called well in advance of audio being received
        override fun onStart(uttId: String?) {
            if (uttId == utteranceId) {
                //onStart()
            }
        }

        override fun onDone(uttId: String?) {
            if (uttId == utteranceId) {
                onComplete()
            }
        }

        override fun onError(uttId: String?) {
            onComplete()
        }
    })

    tts?.speak(sentence, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
}


/**
 * Get sample text. This is a single sentence which contains text in the specified language. The
 * content of the text is roughly "This is an example of speech in <language>", where language is
 * the language name. It currently uses reflection based deprecated code to access the correct text
 * from the resources. This needs to be replaced with hardcoded conditionals
 * TODO: Correct deprecated code
 *
 * @param context [Context] passed to access the app resources
 * @param languageCode ISO 3 letter code of the language
 * @return String of the sample text
 */
fun getSampleText(context: Context, languageCode: String): String {
    val resourceName = "${languageCode}_sample"
    Log.d(LOG_TAG, "Resource name: $resourceName")
    val resId = context.resources.getIdentifier(resourceName, "string", context.packageName)
    val sampleText = if (resId != 0) context.getString(resId) else "Some random sample text."
    return sampleText
}

/**
 * Get demo text. This is a short paragraph which contains text in the specified language. The
 * content of the text is usually the first paragraph of the language's article in the Wikipedia of
 * the language. It currently uses reflection based deprecated code to access the correct text
 * from the resources. This needs to be replaced with hardcoded conditionals
 * TODO: Correct deprecated code
 * TODO: Currently unused, may be used in the future
 *
 * @param context [Context] passed to access the app resources
 * @param languageCode ISO 3 letter code of the language
 * @return String of the demo text
 */
fun getDemoText(context: Context, languageCode: String): String {
    val resourceName = "${languageCode}_demo"
    Log.d(LOG_TAG, "Resource name: $resourceName")
    val resId = context.resources.getIdentifier(resourceName, "string", context.packageName)
    val sampleText = if (resId != 0) context.getString(resId) else "Some random sample text."
    return sampleText
}