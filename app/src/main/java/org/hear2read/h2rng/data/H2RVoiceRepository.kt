package org.hear2read.h2rng.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.hear2read.h2rng.tts.iso3ToIso2Map
import java.util.Locale

/**
 * Thread-safe singleton repository for managing voice data across the app. Uses [StateFlow] for
 * observable state and [Mutex] for thread-safe updates.
 *
 * Note that this is initialized with a hardcoded list and will need to be updated by hand when new
 * voice packs are added to it.
 */

object H2RVoiceRepository {

    // Private mutable state
    private val _voices = MutableStateFlow<List<H2RVoice>>(emptyList())

    // Public read-only state that can be observed
    val voices: StateFlow<List<H2RVoice>> = _voices.asStateFlow()

    // Mutex for thread-safe operations
    private val mutex = Mutex()

    // Track if already initialized
    private var isInitialized = false

    /**
     * Initialize the voice list (unused, just in case)
     */
    suspend fun initializeVoices(initialVoices: List<H2RVoice>) = mutex.withLock {
        _voices.value = initialVoices
    }

    /**
     * Initialize with hardcoded static list
     * PAD packs cannot be populated dynamically
     */
    suspend fun initializeDefault() = mutex.withLock {
        if (isInitialized) return@withLock

        _voices.value = listOf(
                H2RVoice(
                    assetID = "hin",
                    locale = Locale.Builder().setLanguage("hi").build(),
                    id = "hi-tdilv2mono-1",
//        iso3 = "hin",
                    sampleRate = 22050
                ),
        //    Voice("Punjabi", "pa-tdif-1", mutableStateOf(DownloadStatus.NOT_DOWNLOADED), "pan"),
                H2RVoice(
                    assetID = "kan",
                    locale = Locale.Builder().setLanguage("kn").build(),
                    id = "kn-v2-tdilh2radditional-1987val-low",
        //        iso3 = "kan",
                    isMultiSpeaker = true
                ),
                H2RVoice(
                    assetID = "eng",
                    locale = Locale.Builder().setLanguage("en").build(),
                    id = "en-mono-us",
        //        iso3 = "eng"
                ),
        //    Voice("Tamil", "ta-tdif-1", mutableStateOf(DownloadStatus.CORRUPTED), "tam"),
        //    Voice("Marathi", "ma-tdif-1", mutableStateOf(DownloadStatus.DOWNLOADING), "mar"),
        //    Voice("Assamese", "as-tdif-1", mutableStateOf(DownloadStatus.CORRUPTED), "asm"),
        )

        isInitialized = true
    }

    /**
     * Update a specific voice by ID
     */
    suspend fun updateVoice(assetID: String, update: (H2RVoice) -> H2RVoice) = mutex.withLock {
        _voices.update { currentVoices ->
            currentVoices.map { voice ->
                if (voice.assetID == assetID) update(voice) else voice
            }
        }
    }

    /**
     * Update download state for a voice
     */
    suspend fun updateDownloadState(assetID: String, isDownloaded: Boolean) = mutex.withLock {
        _voices.update { currentVoices ->
            currentVoices.map { voice ->
                if (voice.assetID == assetID) voice.copy(
                    isDownloaded = isDownloaded,
                    downloadProgress = if (isDownloaded) 1f else 0f,
                    downloadedBytes = if (isDownloaded) voice.fileSize else 0L
                ) else voice
            }
        }
    }

    /**
     * Update download size for a voice
     */
    suspend fun updateDownloadSize(assetID: String, size: Long) = mutex.withLock {
        _voices.update { currentVoices ->
            currentVoices.map { voice ->
                if (voice.assetID == assetID) voice.copy(fileSize = size) else voice
            }
        }
    }

    /**
     * Update both download state and size atomically
     */
    suspend fun updateDownloadInfo(assetID: String, isDownloaded: Boolean, size: Long) = mutex.withLock {
        _voices.update { currentVoices ->
            currentVoices.map { voice ->
                if (voice.assetID == assetID) {
                    voice.copy(
                        isDownloaded = isDownloaded,
                        fileSize = size,
                        downloadProgress = if (isDownloaded) 1f else 0f,
                        downloadedBytes = if (isDownloaded) size else 0L)
                } else {
                    voice
                }
            }
        }
    }

    /**
     * Update download progress for a voice (called during download)
     */
    suspend fun updateDownloadProgress(
        assetID: String,
        downloadedBytes: Long,
        totalBytes: Long
    ) = mutex.withLock {
        _voices.update { currentVoices ->
            currentVoices.map { voice ->
                if (voice.assetID == assetID) {
                    val progress = if (totalBytes > 0) {
                        (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                    } else {
                        0f
                    }
                    voice.copy(
                        downloadedBytes = downloadedBytes,
                        fileSize = totalBytes,
                        downloadProgress = progress,
                        isDownloaded = false // Still downloading
                    )
                } else {
                    voice
                }
            }
        }
    }

    /**
     * Reset download progress (called when download is canceled/failed)
     */
    suspend fun resetDownloadProgress(assetID: String) = mutex.withLock {
        _voices.update { currentVoices ->
            currentVoices.map { voice ->
                if (voice.assetID == assetID) {
                    voice.copy(
                        downloadedBytes = 0L,
                        downloadProgress = 0f,
                        isDownloaded = false
                    )
                } else {
                    voice
                }
            }
        }
    }

    /**
     * Get a voice by ID (returns current value, not observable)
     */
    fun getVoiceById(voiceId: String): H2RVoice? {
        return _voices.value.find { it.id == voiceId }
    }

    /**
     * Get a voice by ISO (returns current value, not observable)
     */
    fun getVoiceByISO(voiceISO: String): H2RVoice? {
        var iso2 = ""
        if (voiceISO.length == 3)
            iso2 = iso3ToIso2Map[voiceISO].toString()
        return _voices.value.find { it.id.startsWith(iso2) }
    }

    /**
     * Get all voices (returns current value, not observable)
     */
    fun getCurrentVoices(): List<H2RVoice> {
        return _voices.value
    }

    /**
     * Add a new voice
     */
    suspend fun addVoice(voice: H2RVoice) = mutex.withLock {
        _voices.update { currentVoices ->
            if (currentVoices.any { it.id == voice.id }) {
                currentVoices // Don't add duplicates
            } else {
                currentVoices + voice
            }
        }
    }

    /**
     * Remove a voice by ID
     */
    suspend fun removeVoice(assetID: String) = mutex.withLock {
        _voices.update { currentVoices ->
            currentVoices.filter { it.assetID != assetID }
        }
    }

    /**
     * Clear all voices
     */
    suspend fun clearVoices() = mutex.withLock {
        _voices.value = emptyList()
    }
}