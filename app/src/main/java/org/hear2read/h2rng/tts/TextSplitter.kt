package org.hear2read.h2rng.tts

object TextSplitter {

    private val INDIC_RANGE = 0x0900..0x0DFF

    /**
     * Chunks text into parts that are to be read by either the English TTS, or our native TTS
     */
    fun splitText(text: String, scriptRange: IntRange): List<TextChunk> {
        val subsequences = mutableListOf<TextChunk>()

        if (text.all { it.code < 128 }) {
            subsequences.add(TextChunk(isEnglish = true, text = text))
        } else {
            // hack to check if text has ascii AND unicode characters (mixed text)
            if (text.uppercase() != text) {
                subsequences.addAll(splitMixedText(text, scriptRange))
            } else {
                subsequences.add(TextChunk(isEnglish = false, text = text))
            }
        }
        return subsequences
    }

    /***
     * Processes mixed ASCII and Unicode text. Modifies the subsequences to contain only
     * alternating ASCII and Indian language text. Takes care of ASCII numbers and assigns it to
     * either English or Indian depending on context
     *
     * @param text: Mixed text to be split
     * @param script_range: Range of the Unicode characters of the current language
     * @return List of TextChunk containing the split texts. Strictly alternates Indian and English
     * chunks (no consequent chunks of same type)
     */
    private fun splitMixedText(text: String, scriptRange: IntRange): List<TextChunk> {
        val splitTexts = mutableListOf<TextChunk>()
        // contains the current text chunk being built up
        val textBit = StringBuilder()
        // tracks if current text bit is valid native text
        var isNative = false
        // tracks if current text bit is purely non text (whitespace, punct, digits)
        var isNotWord = false

        // We follow a greedy approach in splitting, splitting at the first valid switch from native
        // to English or vice versa
        for (c in text) {
            // Simply append char to empty bit, and update tracking bools
            if (textBit.isEmpty()) {
                isNative = isNativeChar(c, scriptRange)
                isNotWord = isNonTextChar(c)
                textBit.append(c)
                continue
            }

            val isCurrNative = isNativeChar(c, scriptRange)
            val isCurrNontext = isNonTextChar(c)

            // Split only if current text bit is not nontext, and current char is not the same type
            // as the text bit so far
            val isSplit = !(isNotWord || isCurrNontext) && (isNative != isCurrNative)

            if (isSplit) {
                splitTexts.add(TextChunk(isEnglish = isNative, text = textBit.toString()))
                textBit.clear()
            }

            textBit.append(c)
            // Update current text bit type only if it's non word so far
            isNotWord = isNotWord && isCurrNontext
            // Update current text bit type only if not nontext
            if (!isCurrNontext)
                isNative = isCurrNative
        }

        // Add final chunk
        if (textBit.isNotEmpty()) {
            splitTexts.add(TextChunk(isEnglish = isNative, text = textBit.toString()))
        }

        // Merge consequent chunks of same type
        // TODO: this is redundant for the updated splitting logic, remove?
        val splitTextsJoined = mutableListOf<TextChunk>()
        for (chunk in splitTexts) {
            if (splitTextsJoined.isNotEmpty() && splitTextsJoined.last().isEnglish == chunk.isEnglish) {
                splitTextsJoined.last().text += chunk.text
            } else {
                splitTextsJoined.add(chunk)
            }
        }

        return splitTextsJoined
    }

    /***
     * Check whether character is a valid native character
     *
     * @param c: Character to check
     * @param scriptRange: Language character range to check against
     */
    private fun isNativeChar(c: Char, scriptRange: IntRange): Boolean {
        return c.code in scriptRange || c in listOf('।', '॥')
    }

    /***
     * Check whether character is non text character, including Unicode
     *
     * @param c: Character to check
     */
    private fun isNonTextChar(c: Char): Boolean {
        return c.isWhitespace() || c.toString().matches(Regex("\\p{Punct}"))
                || c.isDigit() || !c.isLetterOrDigit()
    }
}