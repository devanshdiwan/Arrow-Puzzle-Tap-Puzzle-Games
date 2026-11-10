package com.example.utils

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sin

class SoundEffectManager {

    var isSoundEnabled: Boolean = true
    var isMusicEnabled: Boolean = true

    private val sampleRate = 22050
    private val scope = CoroutineScope(Dispatchers.Default)
    private var musicJob: Job? = null

    /**
     * Plays a crisp high-frequency UI tap click
     */
    fun playTap() {
        if (!isSoundEnabled) return
        scope.launch {
            generateTone(frequency = 750.0, durationMs = 20, volume = 0.35f)
        }
    }

    /**
     * Plays a satisfying smooth ascending whoosh tone when an arrow escapes
     */
    fun playEscape() {
        if (!isSoundEnabled) return
        scope.launch {
            generateGlissando(startFreq = 480.0, endFreq = 960.0, durationMs = 90, volume = 0.5f)
        }
    }

    /**
     * Plays a subtle low-frequency thud buzz when an arrow is blocked
     */
    fun playBlocked() {
        if (!isSoundEnabled) return
        scope.launch {
            generateTone(frequency = 140.0, durationMs = 70, volume = 0.6f)
        }
    }

    /**
     * Plays a cheerful celebratory fanfare when a level is completed
     */
    fun playVictory() {
        if (!isSoundEnabled) return
        scope.launch {
            // Arpeggio: C5 (523Hz), E5 (659Hz), G5 (784Hz), C6 (1046Hz)
            generateTone(frequency = 523.25, durationMs = 80, volume = 0.45f)
            delay(85)
            generateTone(frequency = 659.25, durationMs = 80, volume = 0.45f)
            delay(85)
            generateTone(frequency = 783.99, durationMs = 90, volume = 0.5f)
            delay(95)
            generateTone(frequency = 1046.50, durationMs = 220, volume = 0.55f)
        }
    }

    fun startBackgroundAmbient() {
        if (!isMusicEnabled || musicJob != null) return
        musicJob = scope.launch {
            val chords = listOf(
                doubleArrayOf(261.63, 329.63, 392.00), // C major
                doubleArrayOf(220.00, 261.63, 329.63), // A minor
                doubleArrayOf(174.61, 220.00, 261.63), // F major
                doubleArrayOf(196.00, 246.94, 293.66)  // G major
            )
            var index = 0
            while (isActive && isMusicEnabled) {
                val chord = chords[index % chords.size]
                // Play soft chime note
                for (freq in chord) {
                    if (!isActive || !isMusicEnabled) break
                    generateTone(frequency = freq, durationMs = 280, volume = 0.08f)
                    delay(380)
                }
                delay(1200)
                index++
            }
        }
    }

    fun stopBackgroundAmbient() {
        musicJob?.cancel()
        musicJob = null
    }

    private fun generateTone(frequency: Double, durationMs: Int, volume: Float) {
        try {
            val numSamples = (durationMs * sampleRate) / 1000
            val buffer = ShortArray(numSamples)
            val angularFreq = 2.0 * Math.PI * frequency / sampleRate

            for (i in 0 until numSamples) {
                // Envelope fade-in / fade-out to prevent clicks
                val envelope = when {
                    i < numSamples * 0.1f -> i / (numSamples * 0.1f)
                    i > numSamples * 0.7f -> (numSamples - i) / (numSamples * 0.3f)
                    else -> 1.0f
                }
                val sample = (sin(angularFreq * i) * 32767 * volume * envelope).toInt()
                buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }

            playAudioTrack(buffer)
        } catch (_: Exception) {
            // Graceful fallback on devices without active audio outputs
        }
    }

    private fun generateGlissando(startFreq: Double, endFreq: Double, durationMs: Int, volume: Float) {
        try {
            val numSamples = (durationMs * sampleRate) / 1000
            val buffer = ShortArray(numSamples)

            var phase = 0.0
            for (i in 0 until numSamples) {
                val progress = i.toDouble() / numSamples
                val currentFreq = startFreq + (endFreq - startFreq) * progress
                phase += 2.0 * Math.PI * currentFreq / sampleRate
                val envelope = when {
                    i < numSamples * 0.1f -> i / (numSamples * 0.1f)
                    i > numSamples * 0.7f -> (numSamples - i) / (numSamples * 0.3f)
                    else -> 1.0f
                }
                val sample = (sin(phase) * 32767 * volume * envelope).toInt()
                buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }

            playAudioTrack(buffer)
        } catch (_: Exception) {
            // Graceful fallback
        }
    }

    private fun playAudioTrack(buffer: ShortArray) {
        var track: AudioTrack? = null
        try {
            track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(buffer.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(buffer, 0, buffer.size)
            track.play()
            Thread.sleep((buffer.size * 1000L / sampleRate) + 20)
            track.stop()
            track.release()
        } catch (_: Exception) {
            track?.release()
        }
    }
}
