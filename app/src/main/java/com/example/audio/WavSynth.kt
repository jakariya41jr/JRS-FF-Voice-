package com.example.audio

import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

object WavSynth {
    private const val SAMPLE_RATE = 22050

    fun writeChimePad(file: File): Long {
        val durationSec = 0.75
        val numSamples = (SAMPLE_RATE * durationSec).toInt()
        val samples = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val env = exp(-4.2 * t)
            val s = 0.45 * sin(2 * PI * 523.25 * t) +
                0.35 * sin(2 * PI * 659.25 * t) +
                0.25 * sin(2 * PI * 783.99 * t)
            samples[i] = (s * env * 28000).toInt().coerceIn(-32767, 32767).toShort()
        }
        writePcmWav(file, samples)
        return (durationSec * 1000).toLong()
    }

    fun writeBassHitPad(file: File): Long {
        val durationSec = 0.55
        val numSamples = (SAMPLE_RATE * durationSec).toInt()
        val samples = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val freq = 140.0 * exp(-5.0 * t) + 46.0
            val env = exp(-5.5 * t)
            val s = sin(2 * PI * freq * t)
            samples[i] = (s * env * 30000).toInt().coerceIn(-32767, 32767).toShort()
        }
        writePcmWav(file, samples)
        return (durationSec * 1000).toLong()
    }

    fun writePingPad(file: File): Long {
        val durationSec = 0.45
        val numSamples = (SAMPLE_RATE * durationSec).toInt()
        val samples = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val env = exp(-7.5 * t)
            val s = 0.65 * sin(2 * PI * 1046.5 * t) + 0.35 * sin(2 * PI * 1567.98 * t)
            samples[i] = (s * env * 26000).toInt().coerceIn(-32767, 32767).toShort()
        }
        writePcmWav(file, samples)
        return (durationSec * 1000).toLong()
    }

    private fun writePcmWav(file: File, samples: ShortArray) {
        file.parentFile?.mkdirs()
        val dataSize = samples.size * 2
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray(Charsets.US_ASCII))
            putInt(36 + dataSize)
            put("WAVE".toByteArray(Charsets.US_ASCII))
            put("fmt ".toByteArray(Charsets.US_ASCII))
            putInt(16)
            putShort(1) // PCM
            putShort(1) // Mono
            putInt(SAMPLE_RATE)
            putInt(SAMPLE_RATE * 2)
            putShort(2)
            putShort(16)
            put("data".toByteArray(Charsets.US_ASCII))
            putInt(dataSize)
        }
        FileOutputStream(file).use { out ->
            out.write(header.array())
            val buf = ByteBuffer.allocate(dataSize).order(ByteOrder.LITTLE_ENDIAN)
            for (s in samples) buf.putShort(s)
            out.write(buf.array())
        }
    }
}
