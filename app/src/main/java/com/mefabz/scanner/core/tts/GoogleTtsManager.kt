package com.mefabz.scanner.core.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleTtsManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private var listener: TtsListener? = null
    private var textToSpeech: TextToSpeech? = null
    private val isReady = AtomicBoolean(false)

    init {
        textToSpeech = TextToSpeech(context.applicationContext, { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.language = Locale.US
                textToSpeech?.setSpeechRate(1.0f)
                isReady.set(true)
            } else {
                isReady.set(false)
            }
        }, GOOGLE_ENGINE_PACKAGE).apply {
            setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    utteranceId?.let { listener?.onStart(it) }
                }

                override fun onDone(utteranceId: String?) {
                    utteranceId?.let { listener?.onDone(it) }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    utteranceId?.let { listener?.onError(it) }
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    utteranceId?.let { listener?.onError(it) }
                }

                override fun onRangeStart(
                    utteranceId: String?,
                    start: Int,
                    end: Int,
                    frame: Int
                ) {
                    utteranceId?.let { listener?.onRangeStart(it, start, end) }
                }
            })
        }
    }

    fun setListener(listener: TtsListener?) {
        this.listener = listener
    }

    fun setLanguage(languageCode: String) {
        val locale = if (languageCode == "en-IN") {
            Locale("en", "IN")
        } else {
            Locale.US
        }
        
        try {
            textToSpeech?.language = locale
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun speak(text: String, utteranceId: String = DEFAULT_UTTERANCE_ID) {
        if (!isReady.get()) return
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stop() {
        textToSpeech?.stop()
    }

    fun shutdown() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
        isReady.set(false)
    }

    private companion object {
        const val GOOGLE_ENGINE_PACKAGE = "com.google.android.tts"
        const val DEFAULT_UTTERANCE_ID = "mefabz_invoice_utterance"
    }
}
