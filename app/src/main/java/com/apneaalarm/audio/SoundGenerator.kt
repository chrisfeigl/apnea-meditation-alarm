package com.apneaalarm.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin

class SoundGenerator {

    companion object {
        private const val SAMPLE_RATE = 44100
    }

    enum class SoundType {
        BOWL,
        LOW_GONG,
        HIGH_GONG
    }

    suspend fun playSound(type: SoundType, volumeMultiplier: Float = 1.0f) = withContext(Dispatchers.IO) {
        val samples = when (type) {
            SoundType.BOWL -> generateBowlSound(durationSeconds = 4.0)
            SoundType.LOW_GONG -> generateGongSound(fundamentalHz = 110.0, durationSeconds = 2.0)
            SoundType.HIGH_GONG -> generateGongSound(fundamentalHz = 330.0, durationSeconds = 1.8)
        }

        val adjustedSamples = samples.map { (it * volumeMultiplier).toInt().toShort() }.toShortArray()
        playPcmData(adjustedSamples)
    }

    /**
     * Play the intro bowl sequence:
     * - 0-48s: Fade in from 0% to maxVolume
     * - 48-51s: Hold at maxVolume
     * - 51-54s: Fade out to 0%
     */
    suspend fun playIntroBowlSequence(
        maxVolume: Float,
        onProgress: (elapsedSeconds: Int) -> Unit = {}
    ) = withContext(Dispatchers.IO) {
        // Generate a longer bowl sound that we'll loop
        val bowlDuration = 6.0 // 6 second loop
        val bowlSamples = generateResonantBowl(bowlDuration)

        val bufferSize = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(maxOf(bufferSize, bowlSamples.size * 2))
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        audioTrack.play()

        val samplesPerSecond = SAMPLE_RATE
        var totalElapsed = 0

        try {
            // Phase 1: Fade in (0-48 seconds)
            for (second in 0 until 48) {
                if (!coroutineContext.isActive) break

                val fadeProgress = second / 48.0f
                val volume = fadeProgress.pow(2) * maxVolume // Quadratic fade for gentle start

                val scaledSamples = bowlSamples.map { (it * volume).toInt().toShort() }.toShortArray()
                audioTrack.write(scaledSamples, 0, samplesPerSecond.coerceAtMost(scaledSamples.size))

                onProgress(second)
                totalElapsed = second
            }

            // Phase 2: Hold at max (48-51 seconds)
            for (second in 48 until 51) {
                if (!coroutineContext.isActive) break

                val scaledSamples = bowlSamples.map { (it * maxVolume).toInt().toShort() }.toShortArray()
                audioTrack.write(scaledSamples, 0, samplesPerSecond.coerceAtMost(scaledSamples.size))

                onProgress(second)
                totalElapsed = second
            }

            // Phase 3: Fade out (51-54 seconds)
            for (second in 51 until 54) {
                if (!coroutineContext.isActive) break

                val fadeProgress = (second - 51) / 3.0f
                val volume = maxVolume * (1.0f - fadeProgress)

                val scaledSamples = bowlSamples.map { (it * volume).toInt().toShort() }.toShortArray()
                audioTrack.write(scaledSamples, 0, samplesPerSecond.coerceAtMost(scaledSamples.size))

                onProgress(second)
                totalElapsed = second
            }
        } finally {
            audioTrack.stop()
            audioTrack.release()
        }
    }

    private fun generateResonantBowl(durationSeconds: Double): ShortArray {
        val numSamples = (SAMPLE_RATE * durationSeconds).toInt()
        val samples = ShortArray(numSamples)

        // Large resonant Tibetan singing bowl - deep tone
        val fundamental = 156.0

        // Rich harmonic structure with beating frequencies
        val partials = listOf(
            Triple(fundamental, 1.0, 0.0),
            Triple(fundamental * 1.003, 0.7, 0.2),
            Triple(fundamental * 2.0, 0.4, 0.0),
            Triple(fundamental * 2.004, 0.3, 0.3),
            Triple(fundamental * 2.89, 0.2, 0.0),
            Triple(fundamental * 3.82, 0.12, 0.0),
            Triple(fundamental * 4.68, 0.06, 0.0),
            Triple(fundamental * 5.55, 0.03, 0.0),
        )

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE

            // Subtle amplitude modulation for "singing" quality
            val singingRate = 0.15
            val singingDepth = 0.08
            val singing = 1.0 + singingDepth * sin(2.0 * PI * singingRate * t)

            var sample = 0.0
            for ((freq, amp, phaseOffset) in partials) {
                val pitchDrift = 1.0 + 0.0003 * sin(2.0 * PI * 0.1 * t + phaseOffset)
                sample += amp * sin(2.0 * PI * freq * pitchDrift * t + phaseOffset)
            }

            val totalAmp = partials.sumOf { it.second }
            sample = sample / totalAmp * singing

            samples[i] = (sample * Short.MAX_VALUE * 0.7).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }

        return samples
    }

    private fun generateBowlSound(durationSeconds: Double): ShortArray {
        val numSamples = (SAMPLE_RATE * durationSeconds).toInt()
        val samples = ShortArray(numSamples)

        val fundamental = 170.0

        val partials = listOf(
            Triple(fundamental, 1.0, 0.0),
            Triple(fundamental * 1.002, 0.8, 0.1),
            Triple(fundamental * 2.0, 0.45, 0.0),
            Triple(fundamental * 2.005, 0.35, 0.15),
            Triple(fundamental * 2.92, 0.25, 0.0),
            Triple(fundamental * 3.86, 0.15, 0.0),
            Triple(fundamental * 4.72, 0.08, 0.0),
        )

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE

            val attackTime = 0.8
            val envelope = when {
                t < attackTime -> (t / attackTime).pow(0.5)
                else -> exp(-0.8 * (t - attackTime) / durationSeconds)
            }

            var sample = 0.0
            for ((freq, amp, phaseOffset) in partials) {
                val partialDecay = exp(-0.3 * (freq / fundamental - 1.0) * t)
                sample += amp * partialDecay * sin(2.0 * PI * freq * t + phaseOffset)
            }

            val totalAmp = partials.sumOf { it.second }
            sample = sample / totalAmp * envelope

            samples[i] = (sample * Short.MAX_VALUE * 0.5).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }

        return samples
    }

    private fun generateGongSound(fundamentalHz: Double, durationSeconds: Double): ShortArray {
        val numSamples = (SAMPLE_RATE * durationSeconds).toInt()
        val samples = ShortArray(numSamples)

        val partials = listOf(
            Pair(fundamentalHz, 1.0),
            Pair(fundamentalHz * 1.52, 0.55),
            Pair(fundamentalHz * 2.0, 0.4),
            Pair(fundamentalHz * 2.48, 0.3),
            Pair(fundamentalHz * 3.15, 0.2),
            Pair(fundamentalHz * 4.2, 0.1),
        )

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE

            val attackTime = 0.015
            val envelope = if (t < attackTime) {
                (t / attackTime).pow(0.3)
            } else {
                exp(-2.5 * (t - attackTime) / durationSeconds)
            }

            var sample = 0.0
            for ((index, partial) in partials.withIndex()) {
                val (freq, amp) = partial
                val partialDecay = exp(-((index + 1) * 1.5) * t / durationSeconds)
                sample += amp * partialDecay * sin(2.0 * PI * freq * t)
            }

            sample = sample / partials.sumOf { it.second } * envelope

            samples[i] = (sample * Short.MAX_VALUE * 0.6).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }

        return samples
    }

    private fun playPcmData(samples: ShortArray) {
        val bufferSize = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(maxOf(bufferSize, samples.size * 2))
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        audioTrack.write(samples, 0, samples.size)
        audioTrack.play()

        val durationMs = (samples.size.toLong() * 1000) / SAMPLE_RATE
        Thread.sleep(durationMs)

        audioTrack.stop()
        audioTrack.release()
    }
}
