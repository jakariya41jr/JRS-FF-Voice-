package com.example.backup

import android.content.Context
import android.net.Uri
import com.example.data.db.SoundCardEntity
import com.example.data.db.VoiceProfileEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

@Serializable
data class BackupBundle(
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val cards: List<BackupCardRecord> = emptyList(),
    val customVoices: List<VoiceProfileEntity> = emptyList()
)

@Serializable
data class BackupCardRecord(
    val title: String,
    val category: String,
    val audioFileName: String?,
    val ttsText: String?,
    val durationMs: Long,
    val isFavorite: Boolean,
    val colorHex: Long
)

class BackupManager(private val context: Context) {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    private val soundsDir: File
        get() = File(context.filesDir, "sounds").apply { mkdirs() }

    suspend fun exportToZip(
        targetUri: Uri,
        cards: List<SoundCardEntity>,
        voices: List<VoiceProfileEntity>
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val cardRecords = cards.map { card ->
                val f = File(card.filePath)
                val fileName = if (card.filePath.isNotBlank() && f.exists()) f.name else null
                BackupCardRecord(
                    title = card.title,
                    category = card.category,
                    audioFileName = fileName,
                    ttsText = card.ttsText,
                    durationMs = card.durationMs,
                    isFavorite = card.isFavorite,
                    colorHex = card.colorHex
                )
            }
            val bundle = BackupBundle(
                cards = cardRecords,
                customVoices = voices.filter { it.isCustom }
            )
            val manifestBytes = json.encodeToString(bundle).toByteArray(Charsets.UTF_8)

            context.contentResolver.openOutputStream(targetUri)?.use { out ->
                ZipOutputStream(BufferedOutputStream(out)).use { zip ->
                    zip.putNextEntry(ZipEntry("manifest.json"))
                    zip.write(manifestBytes)
                    zip.closeEntry()

                    val writtenFiles = mutableSetOf<String>()
                    cards.forEach { card ->
                        val f = File(card.filePath)
                        if (card.filePath.isNotBlank() && f.exists() && writtenFiles.add(f.name)) {
                            zip.putNextEntry(ZipEntry("sounds/${f.name}"))
                            FileInputStream(f).use { fis ->
                                fis.copyTo(zip)
                            }
                            zip.closeEntry()
                        }
                    }
                }
            } ?: return@withContext false
            true
        } catch (_: Throwable) {
            false
        }
    }

    suspend fun importFromZip(sourceUri: Uri): Pair<List<SoundCardEntity>, List<VoiceProfileEntity>>? =
        withContext(Dispatchers.IO) {
            try {
                var bundle: BackupBundle? = null
                val restoredFiles = mutableMapOf<String, String>()

                context.contentResolver.openInputStream(sourceUri)?.use { input ->
                    ZipInputStream(BufferedInputStream(input)).use { zip ->
                        var entry: ZipEntry? = zip.nextEntry
                        while (entry != null) {
                            val name = entry.name
                            if (!entry.isDirectory) {
                                if (name == "manifest.json") {
                                    val text = zip.readBytes().toString(Charsets.UTF_8)
                                    bundle = json.decodeFromString<BackupBundle>(text)
                                } else if (name.startsWith("sounds/") && !name.contains("..")) {
                                    val cleanName = File(name).name
                                    val dest = File(soundsDir, "restored_${System.currentTimeMillis()}_$cleanName")
                                    FileOutputStream(dest).use { fos ->
                                        zip.copyTo(fos)
                                    }
                                    restoredFiles[cleanName] = dest.absolutePath
                                }
                            }
                            zip.closeEntry()
                            entry = zip.nextEntry
                        }
                    }
                }

                val parsed = bundle ?: return@withContext null
                val cards = parsed.cards.map { rec ->
                    val path = rec.audioFileName?.let { restoredFiles[it] }.orEmpty()
                    SoundCardEntity(
                        title = rec.title,
                        category = rec.category,
                        filePath = path,
                        ttsText = rec.ttsText,
                        durationMs = rec.durationMs,
                        isFavorite = rec.isFavorite,
                        colorHex = rec.colorHex
                    )
                }
                val voices = parsed.customVoices.map { it.copy(id = 0L, isCustom = true, isDefault = false) }
                Pair(cards, voices)
            } catch (_: Throwable) {
                null
            }
        }
}
