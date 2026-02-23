package org.hear2read.h2rng.data

import java.util.Locale

/**
 * Data class for the H2R voice object. This object encapsulates all the relevant data
 * corresponding to the voice as required by the various parts of the app. Given the nature of some
 * of the properties ([id], [isMultiSpeaker]), the objects are all initialized hardcoded.
 * Hence, when adding a new voice, the relevant initialization also needs to be hardcoded.
 *
 * @property id This is the most crucial parameter and is used in multiple contexts. The voice
 * object is typically built using just this string. It is a hyphen separated string with the
 * following properties:
 * - It is voice ID as seen by the [TextToSpeech] API.
 * - It is also the name of the voice file
 * - The first element is the ISO 2 letter code of the language
 * - The asset pack ID is the first element in the hyphen separated list as well
 * @property assetID The asset ID associated with the asset pack of the voice. Note that the way
 * the split is done means we can't use the underscore separated ISO code for the country in the
 * voice name (en_US will not work)
 * @property locale The Locale associated with the language initialized using the 2 letter ISO code
 * obtained from the [id]
 * @property isDownloaded True when voice is fully downloaded and transferred
 * @property isMultiSpeaker True for voice models with more than one speaker
 * @property sampleRate Sample rate of the voice in Hz
 * @property fileSize Size of the asset pack download in bytes
 * @property fileLocation Location of the downloaded voice file asset
 * @property downloadedBytes Number of bytes downloaded. This is updated when the asset is
 * downloading
 * @property downloadProgress Download progress (0. to 1.) as obtained from [downloadedBytes] and
 * [fileSize]
 */
data class H2RVoice(
    val id: String,
    val isDownloaded: Boolean = false,
    val isMultiSpeaker: Boolean = false, //TODO: get this from json instead of hardcoding?
    val sampleRate: Int = 16000,
    val fileSize: Long = 0, // in bytes
    val fileLocation: String? = null,
    val downloadedBytes: Long = 0L, // bytes downloaded so far
    val downloadProgress: Float = 0f, // 0.0 to 1.0){}
    val assetAvailable: Boolean = true  // used for testing builds where all assets may not be
                                        // available
){
    val assetID = id.split('-')[0]
    val locale = Locale.Builder().setLanguage(assetID).build()
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