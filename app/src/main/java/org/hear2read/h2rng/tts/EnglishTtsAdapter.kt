package org.hear2read.h2rng.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import org.hear2read.h2rng.tts.api.EnglishProgressListener
import org.hear2read.h2rng.utils.LOG_PREFIX

/**
 * TtsEngine extension to be used for the English TTS engine
 *
 * @constructor
 * @property context Context used to instantiate the [TextToSpeech] object. Currently uses the
 * Service context
 * @param enginePackageName The package name of the English TTS engine to be used
 */
class EnglishTtsAdapter(
    private val context: Context,
    enginePackageName: String
) : TtsEngine {
    private val LOG_TAG = LOG_PREFIX + EnglishTtsAdapter::class.simpleName

    private var tts: TextToSpeech
    private var engSamplerate: Int = 24000

    /**
     * Tracks whether the engine has been initialized
     */
    private val engReadyDeferred = CompletableDeferred<Unit>()

    /**
     * Maps utterance IDs to respective streaming pipes
     */
    private val channels = ConcurrentHashMap<String, Channel<ByteArray>>()

    init {
        tts = TextToSpeech(context, { status ->
            if (status == TextToSpeech.SUCCESS) {
                setupListener()
                warmup()
            } else {
                engReadyDeferred.complete(Unit)
            }
        }, enginePackageName)
    }

    /**
     * Initialises the listener and attaches to the TTS instance
     *
     */
    private fun setupListener() {
        val listener = EnglishProgressListener(
            channels = channels,
            onWarmupSuccess = { sampleRate ->
                Log.d(LOG_TAG, "English sample rate: $sampleRate")
                engSamplerate = sampleRate
                engReadyDeferred.complete(Unit)
            },
            onWarmupFailed = {
                // Fallback to default if warmup fails
                // TODO: change behaviour to have a real fallback
                engReadyDeferred.complete(Unit)
            }
        )
        tts.setOnUtteranceProgressListener(listener)
    }

    /**
     * Starts the warmup process for the English TTS to complete initialization
     *
     */
    private fun warmup() {
        val dummyFile = File(context.cacheDir, "warmup.wav")
        tts.synthesizeToFile("a", null, dummyFile, "warmup")
    }

    /**
     * Waits for warmup
     * Suspending function — must be called from a coroutine or another suspending function.
     *
     */
    suspend fun waitForWarmup() {
        engReadyDeferred.await()
    }

    fun getSampleRate(): Int = engSamplerate

    /**
     * Synthesizes English text
     *
     * @param text The text to be synthesized
     * @param params [SpeechParams] containing parameters of the speech to be generated
     * @return a [ReceiveChannel] of [ByteArray] where the generated audio is fed
     */
    override fun synthesize(text: String, params: SpeechParams): ReceiveChannel<ByteArray> {
        val utteranceId = "eng_${System.nanoTime()}"
        Log.d(LOG_TAG, "eng synthesize: $text")

        // Channel.UNLIMITED prevents Eng TTS from blocking if pipeline is busy
        val channel = Channel<ByteArray>(Channel.UNLIMITED)
        channels[utteranceId] = channel

        val dummyFile = File(context.cacheDir, "$utteranceId.wav")

        tts.setSpeechRate(params.speed)
        tts.setPitch(params.pitch)

        // This fires asynchronously. It returns instantly, and bytes flow into the channel later.
        tts.synthesizeToFile(text, null, dummyFile, utteranceId)

        return channel
    }

    /**
     * Stops current synthesis
     *
     */
    override fun stop() {
        tts.stop()
        channels.values.forEach { it.cancel() }
        channels.clear()
    }

    /**
     * Shuts down the engine
     *
     */
    override fun shutdown() {
        tts.shutdown()
    }
}