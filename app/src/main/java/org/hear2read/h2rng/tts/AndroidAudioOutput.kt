package org.hear2read.h2rng.tts

import android.media.AudioFormat
import android.speech.tts.SynthesisCallback
import android.speech.tts.TextToSpeech

/**
 * Implements [AudioOutput] interface for consuming TTS generated audio. In this case, acts as a
 * wrapper for the [SynthesisCallback] and writes to it
 *
 * @constructor Creates a new AndroidAudioOutput
 * @property callback [SynthesisCallback] to write the output to
 */
class AndroidAudioOutput(private val callback: SynthesisCallback) : AudioOutput {
    override fun start(sampleRate: Int) {
        callback.start(sampleRate, AudioFormat.ENCODING_PCM_16BIT, 1)
    }

    override fun write(audio: ByteArray): Boolean {
        val maxBufferSize = callback.maxBufferSize
        var offset = 0
        while (offset < audio.size) {
            val bytesToWrite = minOf(maxBufferSize, audio.size - offset)
            val result = callback.audioAvailable(audio, offset, bytesToWrite)
            if (result != TextToSpeech.SUCCESS) {
                return false
            }
            offset += bytesToWrite
        }
        return true
    }

    override fun done() {
        callback.done()    }
}