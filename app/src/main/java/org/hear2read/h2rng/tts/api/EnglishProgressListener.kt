package org.hear2read.h2rng.tts.api

import android.speech.tts.UtteranceProgressListener
import java.util.concurrent.ConcurrentHashMap
import org.hear2read.h2rng.tts.AsyncSynthesisTask

class EnglishProgressListener(
    private val englishTasks: ConcurrentHashMap<String, AsyncSynthesisTask>,
    private val onWarmupSuccess: (sampleRate: Int) -> Unit,
    private val onWarmupFailed: () -> Unit
) : UtteranceProgressListener() {
    override fun onStart(utteranceId: String?) { }

    override fun onBeginSynthesis(
        utteranceId: String?,
        sampleRateInHz: Int,
        audioFormat: Int,
        channelCount: Int
    ) {
        if (utteranceId == "warmup") {
            onWarmupSuccess(sampleRateInHz)
        }
    }

    override fun onAudioAvailable(utteranceId: String?, audio: ByteArray) {
        val task = englishTasks[utteranceId] ?: return
        synchronized(task) {
            task.buffer.write(audio)
        }
    }

    override fun onDone(utteranceId: String?) {
        if (utteranceId == "warmup") return

        val task = englishTasks.remove(utteranceId) ?: return
        val pcmBytes = synchronized(task) { task.buffer.toByteArray() }
        task.deferred.complete(pcmBytes)
    }

    @Deprecated("Deprecated in Java")
    override fun onError(utteranceId: String?) {
        if (utteranceId == "warmup") {
            onWarmupFailed()
            return
        }
        englishTasks.remove(utteranceId)?.deferred?.completeExceptionally(
            Exception("English TTS failed for $utteranceId")
        )
    }

}