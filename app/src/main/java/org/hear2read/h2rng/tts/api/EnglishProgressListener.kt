package org.hear2read.h2rng.tts.api

import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.channels.Channel
import org.hear2read.h2rng.tts.EnglishTtsAdapter
import org.hear2read.h2rng.utils.LOG_PREFIX
import java.util.concurrent.ConcurrentHashMap

/**
 * UtteranceProgressListener to be used with the English TTS
 *
 * @constructor Creates a new EnglishProgressListener
 * @property channels The hashmap to be used to create channels for the output
 * @property onWarmupSuccess Callback when warmup succeeds, currently used to set samplerate
 * @property onWarmupFailed Callback on warmup fail, currently just signals engine ready and has no
 * fallback
 */
class EnglishProgressListener(
    private val channels: ConcurrentHashMap<String, Channel<ByteArray>>,
    private val onWarmupSuccess: (sampleRate: Int) -> Unit,
    private val onWarmupFailed: () -> Unit
) : UtteranceProgressListener() {
    private val LOG_TAG = LOG_PREFIX + EnglishProgressListener::class.simpleName

    override fun onStart(utteranceId: String?) { }

    override fun onBeginSynthesis(
        utteranceId: String?,
        sampleRateInHz: Int,
        audioFormat: Int,
        channelCount: Int
    ) {
        Log.d(LOG_TAG, "eng onBeginSynthesis: $utteranceId")
        if (utteranceId == "warmup") {
            onWarmupSuccess(sampleRateInHz)
        }
    }

    override fun onAudioAvailable(utteranceId: String?, audio: ByteArray) {
        val channel = channels[utteranceId] ?: return
        // CRITICAL: We MUST copy the array because Android reuses the memory!
        channel.trySend(audio.copyOf())
    }

    override fun onDone(utteranceId: String?) {
        Log.d(LOG_TAG, "eng onDone: $utteranceId")
        if (utteranceId == "warmup") return

        // Close stream, telling pipeline this chunk is finished
        channels.remove(utteranceId)?.close()
    }

    @Deprecated("Deprecated in Java")
    override fun onError(utteranceId: String?) {
        if (utteranceId == "warmup") {
            onWarmupFailed()
            return
        }

        channels.remove(utteranceId)?.close(
            Exception("English TTS failed for $utteranceId")
        )
    }

}