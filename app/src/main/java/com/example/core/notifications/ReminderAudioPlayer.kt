package com.example.core.notifications

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

/**
 * Plays the user's own imported Adhan audio (no bundled recordings) and optional local TTS.
 * Any failure is reported so the caller can fall back to the system notification tone.
 */
class ReminderAudioPlayer(private val context: Context) {

    private var player: MediaPlayer? = null
    private var textToSpeech: TextToSpeech? = null

    fun playCustomAudio(uriString: String?): Boolean {
        if (uriString.isNullOrBlank()) return false
        return try {
            stop()
            player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                setDataSource(context, Uri.parse(uriString))
                setOnCompletionListener { stop() }
                setOnErrorListener { _, _, _ ->
                    stop()
                    true
                }
                prepare()
                start()
            }
            true
        } catch (error: Exception) {
            Log.w(TAG, "Imported Adhan audio could not be played; falling back to the system tone.", error)
            stop()
            false
        }
    }

    fun playShortTone() {
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(context, uri)
            ringtone?.play()
        } catch (error: Exception) {
            Log.w(TAG, "Default notification tone unavailable.", error)
        }
    }

    fun speak(announcement: String) {
        try {
            if (textToSpeech == null) {
                textToSpeech = TextToSpeech(context) { status ->
                    if (status == TextToSpeech.SUCCESS) {
                        textToSpeech?.language = Locale.getDefault()
                        textToSpeech?.speak(announcement, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
                    }
                }
                textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) = Unit
                    override fun onDone(utteranceId: String?) = shutdown()
                    @Deprecated("Deprecated by Android")
                    override fun onError(utteranceId: String?) = shutdown()
                })
            } else {
                textToSpeech?.speak(announcement, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
            }
        } catch (error: Exception) {
            Log.w(TAG, "Text-to-speech unavailable; skipping spoken announcement.", error)
        }
    }

    fun stop() {
        try {
            player?.stop()
            player?.release()
        } catch (error: IllegalStateException) {
            Log.w(TAG, "Media player was already stopped.", error)
        } finally {
            player = null
        }
    }

    private fun shutdown() {
        try {
            textToSpeech?.stop()
            textToSpeech?.shutdown()
        } catch (error: Exception) {
            Log.w(TAG, "TTS shutdown failed.", error)
        } finally {
            textToSpeech = null
        }
    }

    companion object {
        private const val TAG = "ReminderAudioPlayer"
        private const val UTTERANCE_ID = "namaz-prayer-announcement"
    }
}
