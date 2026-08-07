package org.hear2read.h2rng.utils

import org.hear2read.h2rng.tts.iso3ToIso2Map

//import android.util.Log
//

val LOG_PREFIX = "H2RNG_"

//private val FILENAME = "TextUtils"
//private val LOG_TAG = "${LOG_PREFIX}${FILENAME}"

/**
 * Get script character from ISO code. Used for the icon in the [LanguageListItem]
 *
 * @param languageCode ISO language code. Can be the 2 letter or 3 letter code
 * @return The letter "ka" in the script of the language
 */
fun getScriptCharacter(languageCode: String): String {
//    Log.d(LOG_TAG, "getScriptCharacter on $languageCode")
    var iso2Code = languageCode
    if (iso2Code.length > 2)
        iso2Code = iso3ToIso2Map[languageCode].toString()
    return when (iso2Code.lowercase()) {
        "hi" -> "क"      // Hindi (Devanagari)
        "mr" -> "क"      // Marathi (Devanagari)
        "ne" -> "क"      // Nepali (Devanagari)
        "sa" -> "क"      // Sanskrit (Devanagari)
        "bn" -> "ক"      // Bengali
        "gu" -> "ક"      // Gujarati
        "pa" -> "ਕ"      // Punjabi (Gurmukhi)
        "ta" -> "க"      // Tamil
        "te" -> "క"      // Telugu
        "kn" -> "ಕ"      // Kannada
        "ml" -> "ക"      // Malayalam
        "or" -> "କ"      // Odia
        "as" -> "ক"      // Assamese
        "ur" -> "ک"      // Urdu (Arabic script)
        "si" -> "ක"      // Sinhala
        else -> "K"      // Default to Latin K
    }
}