package org.hear2read.h2rng.ui

import android.app.Application
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.hear2read.h2rng.data.H2RVoiceRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.suspendCancellableCoroutine
import org.hear2read.h2rng.BuildConfig
import org.hear2read.h2rng.tts.INIT_UTT_ID
import org.hear2read.h2rng.utils.LOG_PREFIX
import java.util.Locale
import kotlin.coroutines.resume

/**
 * Main ViewModel containing the TTS logic to be used with the UI. Initializes an instance of
 * [TextToSpeech] with the Hear2ReadNG engine to be used throughout to demo the TTS. Contains and
 * communicates of various data required for the UI
 *
 */
class MainViewModel (application: Application) : AndroidViewModel(application) {
    val LOG_TAG = LOG_PREFIX + MainViewModel::class.simpleName

    // Single source of truth for UI to know if app ready: voices synced and tts initialized
    private val _isAppReady = MutableStateFlow(false)
    val isAppReady = _isAppReady.asStateFlow()
    private var tts: TextToSpeech? = null
    // Whether voice loaded and ready
    private val _isVoiceReady = MutableStateFlow(false)
    val isVoiceReady = _isVoiceReady.asStateFlow()
    // Default sample text for voice
    var defaultText: String = ""
    private val _rawText = MutableStateFlow("")
    val rawText = _rawText.asStateFlow()
    // Stores index of currently playing text, null if stopped playing
    private val _playingIndex = MutableStateFlow<Int?>(null)
    val playingIndex = _playingIndex.asStateFlow()
    // Stores index of currently playing text, null if not playing
    private val _responseTime = MutableStateFlow<Long?>(null)
    val responseTime = _responseTime.asStateFlow()

    // List of sentences, watches _rawText. If _rawText changes, recalculates.
    // stateIn caches the result so the UI doesn't force it to recalculate constantly.
    val sentences: StateFlow<List<String>> = _rawText
        .map { text ->
            if (text.isBlank()) emptyList()
            else //listOf(text)
                text.split(Regex("(?<=[.!?])\\s+"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000), // Standard caching behavior
            initialValue = emptyList()
        )
    // Purely needed to grey out next media control button
    val isNextEnabled: StateFlow<Boolean> = combine(
        sentences,
        playingIndex
    ) { currentSentences, currentIndex ->
        // If nothing is playing, or the list is empty, disable the button
        if (currentIndex == null || currentSentences.isEmpty()) {
            return@combine false
        }

        // Enable it as long as the current index is NOT the last item
        currentIndex < currentSentences.size - 1
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )


    /***
     * Updates text in text box. Has an overloaded feature to accomodate clearing and resetting
     * text, which applies only when newText is empty: if current text is also empty, it means we
     * reset the text to the default value, else we clear the current text setting it to empty
     *
     * @param newText [String] to update the text. Overloaded when empty, see fun description
     */
    fun updateText(newText: String) {
        _responseTime.value = null
        // If edited while reading, stop playback
        if (_playingIndex.value != null) {
            stopTts()
        }
        _rawText.value = if (newText.isEmpty() and _rawText.value.isEmpty()) {
            defaultText
        } else {
            newText
        }

    }

    init {
        viewModelScope.launch {
            // Wait for repository to finish voice sync
            H2RVoiceRepository.isReady.first { it == true }

            // Initialize the TTS engine
            initializeTTS()

            // UI app ready
            _isAppReady.value = true
        }
    }


    /**
     * Initialize base [TextToSpeech] instance. The language isn't set in this stage and will only
     * be set when the [TTSDemoScreen] is shown.
     *
     */
    suspend fun initializeTTS(): Boolean = suspendCancellableCoroutine { continuation ->
        Log.d(LOG_TAG, "initializeBasicTTS()")
        tts = TextToSpeech(application, { status ->
            if (status == TextToSpeech.SUCCESS) {
                // TTS initialized successfully
                Log.d(LOG_TAG, "TTS initialized successfully")
                Log.d(LOG_TAG, "Available languages: ${tts?.availableLanguages}")
                continuation.resume(true)
            } else {
                // Handle initialization failure
                Log.e(LOG_TAG, "TTS initialization failure!")
//                isTTSReady = true // TODO show error
                continuation.resume(true)
            }
        }, BuildConfig.APPLICATION_ID)
    }


    /**
     * Initialize Hear2ReadNG [TextToSpeech] instance for use in the [TTSDemoScreen]. The instance is
     * almost guaranteed to have already been instantiated when the [MainActivity] starts up, and this
     * function is used to ensure that the voice corresponding to the language is loaded before the
     * demo is active. The initialization is done by setting the parameters sent to the [TextToSpeech]
     * has the Locale of the language and sending an empty string to it to speak.
     * TODO: Fallback on TTS not found
     *
     * @param langLocale [Locale] of the voice to be loaded
     * @param onInitialized Function called when the UtteranceProgressListener returns after TTS has
     * finished processing the initialization text, when the voice has been confirmed as loaded
     * @param onError Function called when the UtteranceProgressListener returns an error
     * TODO: Ensure it is also called when the TTS fails to synthesize the language initialization
     *      text
     */
    fun initializeVoice(
        langLocale: Locale? = null
    ) {
        _isVoiceReady.value = false
        // If TTS already exists and is for the same language, reuse it
        tts?.let { tts ->
            Log.d(LOG_TAG, "TTS exists, checking if voice needs loading")
            // Run voice loading code only if languageCode is provided
            langLocale?.let { lang ->
                val result = tts.setLanguage(lang)

                if (result == TextToSpeech.LANG_MISSING_DATA ||
                    result == TextToSpeech.LANG_NOT_SUPPORTED
                ) {
                    // TODO have additional flag
//                    onError()
                } else {
                    tts.setSpeechRate(1.0f)
                    tts.setPitch(1.0f)
                    setupInitUtteranceListener(tts)
                    val dummySentence = ""
                    tts.speak(dummySentence, TextToSpeech.QUEUE_FLUSH, null, "INIT_UTTERANCE")
                }
                return
            }
            // Set ready since nothing needs to be done
            _isVoiceReady.value = true
            return
        }
        // TODO make more expressive
//        onError()
    }

    /**
     * Helper to set [UtteranceProgressListener] for the tts which invokes a callback function when an
     * utterance with the ID matching [INIT_UTT_ID] is processed.
     *
     * @param tts The TextToSpeech instance on which the UtteranceProgressListener needs to be set
     */
    fun setupInitUtteranceListener(tts: TextToSpeech) {
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                if (utteranceId == INIT_UTT_ID) {
                    _isVoiceReady.value = true
                }
            }

            override fun onDone(utteranceId: String?) {
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                // TODO: Handle error
            }
        })
    }

    /**
     * Helper function to play a sentence in the [TTSDemoScreen]. This takes the index of the
     * sentence and matches it on return from the [UtteranceProgressListener]. It also updates
     * response time and takes care of enqueueing behviour
     *
     * @param index Index of the sentence in the TTSDemoScreen
     */
    fun playSentence(
        index: Int
    ) {
        Log.d(LOG_TAG, "playSentence: on index $index, playingIndex: ${playingIndex.value}, " +
                "no. sentences: ${sentences.value.size}")
        var i = index

        if ((index < 0 ) or (index >= sentences.value.size))
            i = 0

        // Stop playback
        stopTts()

        val sentence = sentences.value[i]
        Log.d(LOG_TAG, "playSentence: reading sentence: #$i, $sentence")
        val utteranceId = "SENTENCE_$i"
        val startTime = System.currentTimeMillis()
        _responseTime.value = null
        _playingIndex.value = i

        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onAudioAvailable(uttId: String?, audio: ByteArray?) {
                super.onAudioAvailable(utteranceId, audio)
                if (uttId.toString() == utteranceId) {
                    // Update response time, but only on first instance that audio is available
                    if (_responseTime.value == null)
                        _responseTime.value = System.currentTimeMillis() - startTime
                }

            }
            // unused as this is called well in advance of audio being received
            override fun onStart(uttId: String?) {
            }

            override fun onDone(uttId: String?) {
                if (uttId == utteranceId) {
                    // on complete behaviour, enqueue if remaining,
                    if (i < sentences.value.size - 1)
                        playSentence(i + 1)
                    // else stop
                    else
                        stopTts()
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(uttId: String?) {
                // Try playing the next if remaining,
                if (i < sentences.value.size - 1)
                    playSentence(i + 1)
                // else stop
                else
                    stopTts()
            }
        })

        tts?.speak(sentence, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stopTts() {
        tts?.stop()
        _playingIndex.value = null
    }

    fun getAvailableLanguages(): List<Locale> {
        val availableLangs = tts?.availableLanguages?.toList()?.sortedBy { it.displayLanguage }
            ?: emptyList()
        return availableLangs
    }

    override fun onCleared() {
        super.onCleared()
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}