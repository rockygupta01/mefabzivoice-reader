package com.mefabz.scanner.core.tts

interface TtsListener {
    fun onStart(utteranceId: String)
    fun onDone(utteranceId: String)
    fun onError(utteranceId: String)
    fun onRangeStart(utteranceId: String, start: Int, end: Int)
}
