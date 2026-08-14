package org.hear2read.h2rng.tts

import android.util.Log
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.runBlocking
import org.hear2read.h2rng.utils.LOG_PREFIX
import org.hear2read.h2rng.utils.TextSplitter

/**
 * Pipeline handling mixed text synthesis. Splits text and synchronizes the native and English
 * synthesis outputs
 *
 * @constructor Creates a new TtsPipeline
 * @property englishEngine the English engine
 * @property nativeEngine the native engine
 * @property textSplitter the text splitter to chunk mixed text
 */
class TtsPipeline(
    private val englishEngine: TtsEngine,
    private val nativeEngine: TtsEngine,
    private val textSplitter: TextSplitter
) {
    private val LOG_TAG = LOG_PREFIX + TtsPipeline::class.simpleName

    /**
     * Handles the request, splitting the text and queueing the synthesis and audio in order
     *
     * @param text Text to be synthesized
     * @param output [AudioOutput] that will receive the generated audio
     * @param nativeRange The range of the Unicode characters of the native text
     * @param params [SpeechParams] containing parameters of the speech to be generated
     */
    fun processRequest(
        text: String,
        output: AudioOutput,
        nativeRange: IntRange,
        params: SpeechParams
    ) {
        output.start(params.sampleRate)

        val chunks = if (nativeRange == 0x0000..0x007F) {
            listOf(TextChunk(text, true))
        } else {
            textSplitter.splitText(text, nativeRange)
        }

        if (chunks.isEmpty()) {
            output.done()
            return
        }

        Log.d(LOG_TAG, "Chunked text: ${chunks.map { 
            "${it.text}, english: ${it.isEnglish}" 
        }}")

        // Ensure we're blocking the Android synthesis thread, as is required
        runBlocking {
            Log.d(LOG_TAG, "starting first chunk")
            // Start the first stream immediately
            var currentStream = triggerEngine(chunks[0], params)

            for (i in chunks.indices) {
                // Background lookahead pipeline
                // Start next chunk synthesizing in the background
                // TODO: modify for lookahead only if next is native, needs to change the switch of
                // current to next logic as well
                val nextStream: ReceiveChannel<ByteArray>? = if (i + 1 < chunks.size) {
                    triggerEngine(chunks[i + 1], params)
                } else null

                try {
                    // Consume the current stream chunk-by-chunk as they arrive
                    for (audioBytes in currentStream) {
                        val wasSuccessful = output.write(audioBytes)
                        if (!wasSuccessful) {
                            // Android OS cancelled playback, kill all streams
                            currentStream.cancel()
                            nextStream?.cancel()
                            return@runBlocking
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // Advance the pipeline
                if (nextStream != null) {
                    currentStream = nextStream
                }

                Log.d(LOG_TAG, "ended chunk $i")
            }
        }

        output.done()
    }

    /**
     * Helper function to start synthesis on the appropriate engine
     *
     * @param chunk The [TextChunk] to be processed
     * @param params [SpeechParams] containing parameters of the speech to be generated
     * @return a [ReceiveChannel] of [ByteArray] of generated audio
     */
    private fun triggerEngine(
        chunk: TextChunk,
        params: SpeechParams
    ): ReceiveChannel<ByteArray> {
        Log.d(LOG_TAG, "trigger engine: ${chunk.text}")
        return if (chunk.isEnglish) {
            englishEngine.synthesize(chunk.text, params)
        } else {
            nativeEngine.synthesize(chunk.text, params)
        }
    }
}