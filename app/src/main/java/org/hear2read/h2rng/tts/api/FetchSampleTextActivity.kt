package org.hear2read.h2rng.tts.api

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

/**
 * Fetch sample text activity used by Android [TextToSpeech] API to retrieve the sample text used
 * in the Android Text to Speech setting page. Sends the corresponding sample text keyed to
 * [TextToSpeech.Engine.EXTRA_SAMPLE_TEXT] in the result [Intent]
 *
 */
class FetchSampleTextActivity : Activity() {
    private val LOG_TAG = "H2RNG_" + FetchSampleTextActivity::class.simpleName

    /**
     * Helper to retrieve the sample text from the resources. Uses deprecated reflection to access
     * the sample corresponding to the language instead of hardcoded conditionals
     * TODO: Fix this
     *
     * @param lang The 2 letter ISO code of the language
     * @return The sample text corresponding the language
     */
    private fun getSampleTextData(lang: String) : String {
        val iso3lang = Locale(lang).isO3Language
        val resourceName = "${iso3lang}_sample"
        Log.d(LOG_TAG, "Resource name: $resourceName")
        // TODO: replace with hardcoded conditionals
        val resId = this.resources.getIdentifier(resourceName, "string", this.packageName)

        val sampleText = if (resId != 0) this.getString(resId) else "Some random sample text."
        Log.d(LOG_TAG, sampleText)
        return sampleText
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(LOG_TAG, "onCreate")
        super.onCreate(savedInstanceState)
        var result = TextToSpeech.LANG_AVAILABLE

        val request = intent
        val lang = request.getStringExtra("language")

        Log.d(LOG_TAG, "Language: $lang")
//        val country = request.getStringExtra("country")
//        val variant = request.getStringExtra("variant")

        val text: String = getSampleTextData(lang ?: "")
        if (text.isEmpty()) {
            result = TextToSpeech.LANG_NOT_SUPPORTED
        }

        val intent = Intent().apply {
            if (result == TextToSpeech.LANG_AVAILABLE) {
                putExtra(TextToSpeech.Engine.EXTRA_SAMPLE_TEXT, text)
            } else {
                putExtra("sampleText", text)
            }
        }

        setResult(result, intent)
        finish()
    }
}