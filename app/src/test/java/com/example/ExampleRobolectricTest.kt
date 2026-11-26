package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.audio.WavSynth
import com.example.data.prefs.AppPreferences
import com.example.ui.i18n.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("VoxDeck", appName)
    }

    @Test
    fun `preferences persist language and local api key`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = AppPreferences(context)
        prefs.setLanguage(AppLanguage.EN)
        assertEquals(AppLanguage.EN, prefs.language.value)
        prefs.toggleLanguage()
        assertEquals(AppLanguage.BN, prefs.language.value)

        prefs.setCartesiaApiKey("sk_test_local_123")
        assertEquals("sk_test_local_123", prefs.cartesiaApiKey.value)
    }

    @Test
    fun `wav synth writes valid wav header`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val out = File(context.cacheDir, "test_chime.wav")
        val dur = WavSynth.writeChimePad(out)
        assertTrue(dur > 0)
        assertTrue(out.exists() && out.length() > 44)
        val header = out.readBytes().take(4).toByteArray().toString(Charsets.US_ASCII)
        assertEquals("RIFF", header)
        out.delete()
    }
}
