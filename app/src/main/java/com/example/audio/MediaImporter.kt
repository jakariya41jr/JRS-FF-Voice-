package com.example.audio

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer

data class ImportedAudioResult(
    val title: String,
    val filePath: String,
    val durationMs: Long
)

class MediaImporter(private val context: Context) {
    private val soundsDir: File
        get() = File(context.filesDir, "sounds").apply { mkdirs() }

    suspend fun importFromUri(uri: Uri): ImportedAudioResult? = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val mimeType = resolver.getType(uri).orEmpty()
        val displayName = queryDisplayName(uri) ?: "sound_${System.currentTimeMillis()}"
        val baseTitle = displayName.substringBeforeLast('.').ifBlank { "Imported Sound" }
        val stamp = System.currentTimeMillis()

        val resultFile = if (mimeType.startsWith("video/")) {
            val outFile = File(soundsDir, "imp_$stamp.m4a")
            val extracted = extractAudioFromVideoUri(uri, outFile)
            if (extracted) outFile else {
                // Fallback direct copy if container audio track couldn't be remuxed
                val fallback = File(soundsDir, "imp_$stamp.mp4")
                if (copyUriToFile(uri, fallback)) fallback else null
            }
        } else {
            val ext = displayName.substringAfterLast('.', "mp3").lowercase().take(5)
            val safeExt = if (ext.all { it.isLetterOrDigit() }) ext else "mp3"
            val outFile = File(soundsDir, "imp_$stamp.$safeExt")
            if (copyUriToFile(uri, outFile)) outFile else null
        }

        if (resultFile == null || !resultFile.exists() || resultFile.length() == 0L) {
            return@withContext null
        }

        val durationMs = readDurationMs(resultFile)
        ImportedAudioResult(
            title = baseTitle.take(40),
            filePath = resultFile.absolutePath,
            durationMs = durationMs
        )
    }

    private fun extractAudioFromVideoUri(uri: Uri, outFile: File): Boolean {
        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null
        return try {
            extractor.setDataSource(context, uri, null)
            var audioTrackIndex = -1
            var audioFormat: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME).orEmpty()
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    audioFormat = format
                    break
                }
            }
            if (audioTrackIndex == -1 || audioFormat == null) return false

            extractor.selectTrack(audioTrackIndex)
            muxer = MediaMuxer(outFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val muxTrack = muxer.addTrack(audioFormat)
            muxer.start()

            val buffer = ByteBuffer.allocate(512 * 1024)
            val bufferInfo = MediaCodec.BufferInfo()
            while (true) {
                val sampleSize = extractor.readSampleData(buffer, 0)
                if (sampleSize < 0) break
                bufferInfo.offset = 0
                bufferInfo.size = sampleSize
                bufferInfo.presentationTimeUs = extractor.sampleTime
                bufferInfo.flags = extractor.sampleFlags
                muxer.writeSampleData(muxTrack, buffer, bufferInfo)
                extractor.advance()
            }
            true
        } catch (_: Throwable) {
            outFile.delete()
            false
        } finally {
            try {
                muxer?.stop()
                muxer?.release()
            } catch (_: Throwable) {
            }
            try {
                extractor.release()
            } catch (_: Throwable) {
            }
        }
    }

    private fun copyUriToFile(uri: Uri, target: File): Boolean {
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(target).use { output ->
                    input.copyTo(output)
                }
            } != null
        } catch (_: Throwable) {
            target.delete()
            false
        }
    }

    private fun queryDisplayName(uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) cursor.getString(idx) else null
                    } else null
                }
        } catch (_: Throwable) {
            null
        }
    }

    fun readDurationMs(file: File): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        } catch (_: Throwable) {
            0L
        } finally {
            try {
                retriever.release()
            } catch (_: Throwable) {
            }
        }
    }
}
