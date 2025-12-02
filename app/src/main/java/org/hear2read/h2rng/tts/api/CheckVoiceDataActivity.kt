/*
 * This file handles the logic of checking the available voices in the TTS menu under Android Talkback.
 * CheckVoiceDataActivity is the activity that handles this task.
 * If any "Voice" is present in "voices" and it sis downloaded, it will be shown in the menu.
 * Refer to InstallVoices.kt for more details on "Voice" and "voices".
 */

package org.hear2read.h2rng.tts.api

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import org.hear2read.h2rng.data.H2RVoiceRepository.getCurrentVoices

/**
 * Check voice data activity as required by the Android [TextToSpeech] API. When called and run, it
 * checks the available (downloaded) and unavailable voices and puts them in the result Intent. The
 * voices are checked through the [H2RVoiceRepository]
 *
 */
class CheckVoiceDataActivity : Activity() {
    private val LOG_TAG = "H2RNG_${CheckVoiceDataActivity::class.simpleName}"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val availableLangs: ArrayList<String> = ArrayList()
        val unavailableLangs: ArrayList<String> = ArrayList()

        val currentVoices = getCurrentVoices()

        for (voice in currentVoices) {
            if (voice.isDownloaded) {
                availableLangs.add(voice.locale.isO3Language)
                Log.d(LOG_TAG, "Added voice: ${voice.locale}")
            }
            else {
                unavailableLangs.add(voice.locale.isO3Language)
            }
        }

        val intent = Intent().apply {
            putStringArrayListExtra(TextToSpeech.Engine.EXTRA_AVAILABLE_VOICES, availableLangs)
            putStringArrayListExtra(TextToSpeech.Engine.EXTRA_UNAVAILABLE_VOICES, unavailableLangs)
        }
        setResult(TextToSpeech.Engine.CHECK_VOICE_DATA_PASS, intent)
        finish()
    }
}