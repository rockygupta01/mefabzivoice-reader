package com.mefabz.scanner.feature.scanner.presentation.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle

private val colorStyleMap = mapOf(
    "black" to SpanStyle(color = Color.Black, background = Color.White.copy(alpha = 0.7f)),
    "white" to SpanStyle(color = Color.White),
    "green" to SpanStyle(color = Color(0xFF4ADE80)),
    "grey" to SpanStyle(color = Color(0xFF9CA3AF)),
    "gray" to SpanStyle(color = Color(0xFF9CA3AF)),
    "brown" to SpanStyle(color = Color(0xFFD97706)),
    "red" to SpanStyle(color = Color(0xFFF87171)),
    "cream" to SpanStyle(color = Color(0xFFFEF3C7)),
    "blue" to SpanStyle(color = Color(0xFF60A5FA)),
    "yellow" to SpanStyle(color = Color(0xFFFBBF24)),
    "pink" to SpanStyle(color = Color(0xFFF472B6)),
    "purple" to SpanStyle(color = Color(0xFFA855F7)),
    "orange" to SpanStyle(color = Color(0xFFFB923C))
)

fun buildColorHighlightedString(text: String, baseColor: Color): AnnotatedString {
    return buildAnnotatedString {
        val pattern = colorStyleMap.keys.joinToString("|", prefix = "\\b(", postfix = ")\\b")
        val regex = Regex(pattern, RegexOption.IGNORE_CASE)
        
        var currentIndex = 0
        val matches = regex.findAll(text)
        
        for (match in matches) {
            val colorName = match.value.lowercase()
            val style = colorStyleMap[colorName]
            
            // Append text before match with base style
            if (match.range.first > currentIndex) {
                withStyle(SpanStyle(color = baseColor)) {
                    append(text.substring(currentIndex, match.range.first))
                }
            }
            
            // Append colorized text
            if (style != null) {
                withStyle(style) {
                    append(match.value)
                }
            } else {
                withStyle(SpanStyle(color = baseColor)) {
                    append(match.value)
                }
            }
            currentIndex = match.range.last + 1
        }
        
        // Append remaining text
        if (currentIndex < text.length) {
            withStyle(SpanStyle(color = baseColor)) {
                append(text.substring(currentIndex))
            }
        }
    }
}
