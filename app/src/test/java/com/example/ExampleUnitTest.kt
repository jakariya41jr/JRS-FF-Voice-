package com.example

import com.example.audio.WavSynth
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.stringsFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ExampleUnitTest {
    @Test
    fun `wav synth writes valid riff wave header`() {
        val tempFile = File.createTempFile("voxdeck_test", ".wav")
        try {
            val dur = WavSynth.writeChimePad(tempFile)
            assertTrue(dur > 0)
            assertTrue(tempFile.length() > 44)
            val bytes = tempFile.readBytes()
            assertEquals("RIFF", String(bytes, 0, 4, Charsets.US_ASCII))
            assertEquals("WAVE", String(bytes, 8, 4, Charsets.US_ASCII))
        } finally {
            tempFile.delete()
        }
    }

    @Test
    fun `bengali and english strings switch cleanly`() {
        val bn = stringsFor(AppLanguage.BN)
        val en = stringsFor(AppLanguage.EN)
        assertEquals("সাউন্ডবোর্ড", bn.navSoundboard)
        assertEquals("Board", en.navSoundboard)
    }
}
