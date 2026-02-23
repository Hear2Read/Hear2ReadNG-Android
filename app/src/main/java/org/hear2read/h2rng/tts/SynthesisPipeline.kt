package org.hear2read.h2rng.tts

import android.speech.tts.SynthesisCallback
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.hear2read.h2rng.Synthesizer
import java.util.concurrent.CancellationException
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.coroutineContext

/**
 * Synthesis pipeline class to handle asynchronous synthesis of text from the TTS synthesis
 * function. Implements a single sized buffer so that synthesis of one sentence is started in
 * advance once the audio of the previous sentence has started being fed to [SynthesisCallback].
 * Note that this has been implemented such that [SynthesisCallback.audioAvailable] is only being
 * called in a blocking fashion from the Synthesis Thread, as required by Android
 *
 * @property synthesisCallback [SynthesisCallback] object to feed generated audio
 * @property synthParams [SynthParams] object having relevant parameters for synthesis
 * @property timeoutMillis Timeout in milliseconds to prevent hanging. Default value 10s
 */
class SynthesisPipeline(
    private val synthesisCallback: SynthesisCallback,
    private val synthParams: SynthParams,
    private val timeoutMillis: Long = 10000
) {
    private val LOG_TAG = "H2R_SynthesisPipeline"

    /**
     * Check for if the pipeline has started and has not been stopped
     */
    private val isActive = AtomicBoolean(false)

    /**
     * Check for if pipeline in the process of stopping
     */
    private val isStopping = AtomicBoolean(false)

    /**
     * Job to implement cancellable synthesis
     */
    private var synthJob: Job? = null


    /**
     * Sealed class used as return from the flow when synthesis is done. Either holds the audio
     * data, if synthesis completed successfully, the exception raised in case of interrupted
     * synthesis, or an object indicating the timeout was triggered
     *
     */
    sealed class SynthesisResult {
        data class Success(val audioData: ByteArray) : SynthesisResult()
        data class Error(val exception: Exception) : SynthesisResult()
        object Timeout: SynthesisResult()
    }


    /**
     * Starts the blocking synthesis pipeline. Runs on the parent Synthesis Thread provided by
     * Android, while running the synthesis on [Dispatchers.Default].
     *
     * @param sentences List<LongArray> of phonemeIDs required to be synthesised
     */
    fun start(sentences: List<LongArray>) = runBlocking {
        if (!isActive.compareAndSet(false, true)) {
            Log.w(LOG_TAG, "Pipeline already active")
            return@runBlocking
        }

//        Log.d(LOG_TAG, "Setting isStopping false")
        isStopping.set(false)
        var hasError = false

        Synthesizer.initSynthesis()

        try {
//            var j = 0
            synthJob = launch {
//                printCurrentDispatcher()
//                var i = 0
                sentences.asFlow()
                    .map { sentence ->
                        // Check cancellation
                        if (!isActive.get() || isStopping.get()) {
                            throw CancellationException("Stopped")
                        }

                        try {
                            val audioData = withTimeout(timeoutMillis) {
//                                Log.d(LOG_TAG ,"Started synthesis of sentence $i")
                                Synthesizer.inferFromPhonemeIDs(sentence, synthParams)
                            }
//                            Log.d(LOG_TAG, "Finished synthesis of sentence $i, " +
//                                    "audio length: ${audioData.size}")
//                            i += 1
                            SynthesisResult.Success(audioData)

                        } catch (e: TimeoutCancellationException) {
                            Log.w(LOG_TAG, "Synthesis timeout!")
                            SynthesisResult.Timeout
//                            throw e   // don't throw exception to cancel entire pipeline, process
                                        // next sentence instead

                        } catch (e: CancellationException) {
                            throw e // throw to cancel job

                        } catch (e: Exception) {
                            Log.e(LOG_TAG, "Synthesis error! ", e)
                            SynthesisResult.Error(e)
                        }
                    }
//                    .catch { e ->
//                        when(e) {
//                            is TimeoutCancellationException -> {
//                                Log.w(LOG_TAG, "Synthesis timeout!")
//                                SynthesisResult.Timeout
//                            }
//                            is CancellationException -> {}
//                            else -> {
//                                Log.e(LOG_TAG, "Synthesis error! ", e)
//                                SynthesisResult.Error(e)
//                            }
//                        }
//                    }
                    .flowOn(Dispatchers.Default)
                    .buffer(capacity = 1)
                    .collect { result ->
                        // Check if stopped
                        if (isStopping.get()) return@collect

                        when (result) {
                            is SynthesisResult.Success -> {
//                                Log.d(LOG_TAG ,"Feeding audio of sentence $j, length " +
//                                        "${result.audioData.size}")
                                feedAudioToCallback(result.audioData)
//                                Log.d(LOG_TAG, "Done feeding audio of sentence $j")
//                                j += 1
                            }

                            is SynthesisResult.Error -> {
                                Log.w(LOG_TAG, "Skipping error")
                                hasError = true
                            }

                            is SynthesisResult.Timeout -> {
                                Log.w(LOG_TAG, "Skipping timeout")
                                hasError = true
                            }
                        }
                    }
            }

            // Wait for flow to complete
            synthJob?.join()

            // Call done if not stopped
            if (isActive.get() && !isStopping.get()) {
                if (hasError) {
                    Log.w(LOG_TAG, "Synthesis completed with some errors")
                }
//                Log.d(LOG_TAG, "Synthesis completed")
                synthesisCallback.done()
            }

        } catch (e: CancellationException) {
//            Log.d(LOG_TAG, "Synthesis cancelled")
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Unexpected error in synthesis pipeline", e)
            if (!isStopping.get()) {
                synthesisCallback.error()
            }
        } finally {
//            Log.d(LOG_TAG, "Closing Synthesis pipeline job")
            isActive.set(false)
            synthJob = null
        }
    }


    /**
     * Logging function to pring current dispatcher and thread, to check where the audio callback
     * is being called
     *
     */
    suspend fun printCurrentDispatcher() {
        val dispatcher = coroutineContext[ContinuationInterceptor]
        Log.d(LOG_TAG,"Current Dispatcher is: $dispatcher, current thread: ${Thread.currentThread().name}")
    }


    /**
     * Thread safe function to stop synthesis, as required by TextToSpeechService.onStop. Can be
     * called from any thread multiple times, and is safe to call when pipeline not running.
     */
    fun stop() {
        if (!isActive.get()) {
//            Log.d(LOG_TAG, "Pipeline not active, stop is no-op")
            return
        }

        if (!isStopping.compareAndSet(false, true)) {
//            Log.d(LOG_TAG, "Already stopping")
            return
        }

//        Log.d(LOG_TAG, "Stopping synthesis pipeline")

        synthJob?.cancel()
    }


    /**
     * Function to feed audio to the [SynthesisCallback] completely.
     *
     * @param audioData The raw audio data in bytes to be fed to the [SynthesisCallback]
     * @return 1 if completed, 0 if interrupted
     */
    private fun feedAudioToCallback(audioData: ByteArray): Int {
//        Log.d(LOG_TAG, "Feeding audio length ${audioData.size}")
        val maxBufferSize = synthesisCallback.maxBufferSize
        var offset = 0

        while (offset < audioData.size && isActive.get() && !isStopping.get()) {
            val chunkSize = minOf(maxBufferSize, audioData.size - offset)
//            Log.d(LOG_TAG, "Feeding chunk at $offset, chunk $chunkSize")
            val written = synthesisCallback.audioAvailable(
                audioData,
                offset,
                chunkSize
            )
            if (written < 0) return 0
            offset += chunkSize
        }
//        Log.d(LOG_TAG, "Exiting feedAudioCallback, offset: $offset, ")

        return 1
    }

}