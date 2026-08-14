package org.hear2read.h2rng.tts

import android.util.Log
import androidx.annotation.Keep
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.launch
import org.hear2read.h2rng.utils.LOG_PREFIX

/**
 * [TtsEngine] extension for Hearr2ReadNG native text synthesis. Also extends [H2RNGCallback] to
 * receive audio from the engine
 *
 * @constructor Creates a new H2rngTtsAdapter
 * @property scope The [CoroutineScope] used by the [android.speech.tts.TextToSpeechService] to
 * block calls
 * @property filesDirPath the files dir path used to initialize the Hear2ReadNG engine. It should
 * contain the files necessary for the engine functioning.
 */
class H2rngTtsAdapter(
    private val scope: CoroutineScope, // Provided by TTSService to launch blocking JNI call
    private val filesDirPath: String
) : TtsEngine, H2RNGCallback {

    private val LOG_TAG = "${LOG_PREFIX}${H2rngTtsAdapter::class.simpleName}"

    /**
     * Active channel to which audio is written
     */
    @Volatile
    private var activeChannel: Channel<ByteArray>? = null

    init {
        Synthesizer.init(filesDirPath, this)
    }

    /**
     * Synthesizes native text
     *
     * @param text The text to be synthesized
     * @param params [SpeechParams] containing parameters of the speech to be generated
     * @return a [ReceiveChannel] of [ByteArray] where the generated audio is fed
     */
    override fun synthesize(text: String, params: SpeechParams): ReceiveChannel<ByteArray> {
        Log.d(LOG_TAG, "h2r synthesize: $text")
        val channel = Channel<ByteArray>(Channel.UNLIMITED)
        activeChannel = channel

        // Launch in background: blocking is done in the pipeline
        scope.launch(Dispatchers.Default) {
            try {
                Synthesizer.synthesize(
                    text,
                    speed = params.speed,
                    pitch = params.pitch,
                    volume = params.volume,
                    charMode = params.charMode,
                    sampleRate = params.sampleRate
                )
            } finally {
                channel.close()
                if (activeChannel == channel) activeChannel = null
            }
        }

        return channel
    }

    /**
     * Stops current synthesis
     *
     */
    override fun stop() {
        activeChannel?.cancel()
        activeChannel = null
        Synthesizer.stop()
    }

    /**
     * Shuts down the engine
     *
     */
    override fun shutdown() {
        Synthesizer.terminate()
    }

    /**
     * Override function defining the audio callback behaviour. Is called by the engine to send
     * synthsized audio
     *
     * @param samples The audio samples that have been synthesized by the engine
     */
    @Keep
    override fun onAudio(samples: ShortArray) {
        Log.d(LOG_TAG, "onAudio length ${samples.size}")

        val channel = activeChannel ?: return

        val byteArray = ByteArray(samples.size * Short.SIZE_BYTES)
        for (i in 0 until samples.size) {
            val intVal = samples[i].toInt()

            byteArray[2 * i] = (intVal).toByte()
            byteArray[2 * i + 1] = (intVal shr 8).toByte()
        }
//        feedAudioToCallback(byteArray)
        // Push audio chunk to pipe
        channel.trySend(byteArray)
    }

    /**
     * Override function defining the index reached behaviour. Unused in Android
     *     *
     * @param index
     */
    @Keep
    override fun onIndexReached(index: Int) {
        // Unused
    }

    /**
     * Override function defining the synthesis completed behaviour.
     *
     */
    @Keep
    override fun onDone() {
        // Signal the pipeline that native engine is finished with synthesis
        activeChannel?.close()
        activeChannel = null
    }
}