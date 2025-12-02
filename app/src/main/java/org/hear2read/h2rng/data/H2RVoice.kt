package org.hear2read.h2rng.data

import java.util.Locale

/**
 * Data class for the H2R voice object. This object encapsulates all the relevant data
 * corresponding to the voice as required by the various parts of the app. Given the nature of some
 * of the properties ([assetID], [isMultiSpeaker]), the objects are all initialized hardcoded.
 * Hence, when adding a new voice, the relevant initialization also needs to be hardcoded.
 *
 * @property assetID The asset package nme as used by Android to locate the relevant asset
 * @property locale The Locale associated with the language initialized using the 2 letter ISO code
 * @property id The voice ID as seen by the [TextToSpeech] API. Is also the name of the voice file
 * @property isDownloaded True when voice is fully downloaded and transferred
 * @property isMultiSpeaker True for voice models with more than one speaker
 * @property sampleRate Sample rate of the voice in Hz
 * @property fileSize Size of the asset pack download in bytes
 * @property fileLocation Location of the downloaded voice file asset TODO: not implemented
 * @property downloadedBytes Number of bytes downloaded. This is updated when the asset is
 * downloading
 * @property downloadProgress Download progress (0. to 1.) as obtained from [downloadedBytes] and
 * [fileSize]
 */
data class H2RVoice(
    //    val name: String, // e.g. Hindi
    val assetID: String,
    val locale: Locale,
    val id: String, // e.g. hi-tdilv2mono-... TODO: is this required?
    val isDownloaded: Boolean = false,
    //    val iso3: String,
    val isMultiSpeaker: Boolean = false, //TODO: get this from json instead of hardcoding?
    val sampleRate: Int = 16000,
    val fileSize: Long = 0, // in bytes
    // TODO implement this
    val fileLocation: String? = null,
    val downloadedBytes: Long = 0L, // bytes downloaded so far
    val downloadProgress: Float = 0f // 0.0 to 1.0
){
    /**
     * Get progress percentage
     *
     * TODO: Unused
     *
     * @return [downloadProgress] as a percentage
     */
    fun getProgressPercentage(): Int = (downloadProgress * 100).toInt()

    /**
     * Is downloading
     *
     * @return True when voice asset pack is being downloaded
     */
    fun isDownloading(): Boolean = !isDownloaded && (downloadProgress > 0f)
}