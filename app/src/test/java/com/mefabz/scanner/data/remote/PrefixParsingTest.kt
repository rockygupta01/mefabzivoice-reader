package com.mefabz.scanner.data.remote

import org.junit.Assert.assertEquals
import org.junit.Test

class PrefixParsingTest {

    private fun parsePrefixes(raw: String): List<String> {
        return raw.split(",")
            .map { it.trim().uppercase() }
            .filter { it.isNotBlank() }
            .ifEmpty { listOf("MEFABZ") }
    }

    private fun buildSuffixRegex(raw: String): Regex {
        val suffixes = raw.split(",")
            .map { it.trim().lowercase() }
            .filter { it.isNotBlank() }
            
        if (suffixes.isEmpty()) {
            return Regex("(?!)") // Matches nothing if completely empty
        }
        
        return Regex(
            suffixes.joinToString("|", prefix = "(?i)\\b(", postfix = ")\\b\\s*[)\\]]?")
        )
    }

    @Test
    fun testParsePrefixes() {
        assertEquals(listOf("MEFABZ"), parsePrefixes("MEFABZ"))
        assertEquals(listOf("MEFABZ", "INVOICE"), parsePrefixes("MEFABZ, INVOICE"))
        assertEquals(listOf("TEST"), parsePrefixes("test"))
        assertEquals(listOf("MEFABZ"), parsePrefixes(""))
        assertEquals(listOf("MEFABZ"), parsePrefixes("  ,  "))
    }

    @Test
    fun testBuildSuffixRegex() {
        // Test empty gives match nothing
        val emptyRegex = buildSuffixRegex("")
        assertEquals(false, emptyRegex.containsMatchIn("black"))
        
        // Test single suffix
        val singleRegex = buildSuffixRegex("black")
        assertEquals(true, singleRegex.containsMatchIn("T-Shirt Black"))
        assertEquals(true, singleRegex.containsMatchIn("t-shirt black)"))
        
        // Test multiple suffixes
        val multiRegex = buildSuffixRegex("black, blue, red")
        assertEquals(true, multiRegex.containsMatchIn("Shirt Blue "))
        assertEquals(true, multiRegex.containsMatchIn("Pants RED"))
        assertEquals(false, multiRegex.containsMatchIn("Shirt Green"))
    }
}
