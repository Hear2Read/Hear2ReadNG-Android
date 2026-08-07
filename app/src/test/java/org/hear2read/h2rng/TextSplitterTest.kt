package org.hear2read.h2rng

import org.hear2read.h2rng.tts.TextSplitter
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.InputStreamReader

class TextSplitterTest {

    // Punjabi Gurmukhi Unicode Range
    private val PUNJABI_RANGE = 0x0A00..0x0A7F

    @Test
    fun testSplittingLogicWithSampleFile() {
        // 1. Read the file from the resources folder
        val inputStream = javaClass.classLoader?.getResourceAsStream("sample_splits.txt")
        requireNotNull(inputStream) { "Sample file not found in resources folder!" }

        val reader = InputStreamReader(inputStream)
        val lines = reader.readLines()

        // Skip the header row
        for (i in 1 until lines.size) {
            val line = lines[i]
            if (line.isBlank()) continue

            // 2. Parse the Tab-Separated columns
            val columns = line.split("\t")
            val originalText = columns[0].trim()
            val expectedChunksRaw = columns[1].trim()

            // 3. Parse the expected chunks string (e.g. "ਨਗਰ ਕੌਂਸਲ", "work around CI 3")
            // We use Regex to extract everything inside the quotes
            val expectedTextList = Regex("\"([^\"]*)\"")
                .findAll(expectedChunksRaw)
                .map { it.groupValues[1] }
                .toList()

            // 4. Run your Kotlin code
            val actualChunks = TextSplitter.splitText(originalText, PUNJABI_RANGE)
            val actualTextList = actualChunks.map { it.text.trim() }

            // 5. Compare the output!
            assertEquals(
                "Failed on input: $originalText",
                expectedTextList,
                actualTextList
            )
        }
    }
}