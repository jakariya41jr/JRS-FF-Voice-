package com.example.system

import android.content.pm.PackageManager
import com.example.data.prefs.AppPreferences
import com.example.data.prefs.AudioRoutingMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

enum class RootStatus {
    UNKNOWN,
    GRANTED,
    NOT_AVAILABLE
}

enum class ShizukuStatus {
    NOT_RUNNING,
    PERMISSION_REQUIRED,
    GRANTED
}

class RootShizukuController(private val prefs: AppPreferences) {
    private val _rootStatus = MutableStateFlow(RootStatus.UNKNOWN)
    val rootStatus: StateFlow<RootStatus> = _rootStatus.asStateFlow()

    private val _shizukuStatus = MutableStateFlow(ShizukuStatus.NOT_RUNNING)
    val shizukuStatus: StateFlow<ShizukuStatus> = _shizukuStatus.asStateFlow()

    private val _commandOutput = MutableStateFlow("")
    val commandOutput: StateFlow<String> = _commandOutput.asStateFlow()

    private var listenersRegistered = false

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        refreshShizukuStatus()
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        _shizukuStatus.value = ShizukuStatus.NOT_RUNNING
    }

    private val permissionResultListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode == SHIZUKU_REQ_CODE) {
                _shizukuStatus.value = if (grantResult == PackageManager.PERMISSION_GRANTED) {
                    ShizukuStatus.GRANTED
                } else {
                    ShizukuStatus.PERMISSION_REQUIRED
                }
            }
        }

    fun registerShizukuListeners() {
        if (listenersRegistered) return
        listenersRegistered = true
        try {
            Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
            Shizuku.addBinderDeadListener(binderDeadListener)
            Shizuku.addRequestPermissionResultListener(permissionResultListener)
        } catch (_: Throwable) {
        }
        refreshShizukuStatus()
    }

    fun unregisterShizukuListeners() {
        if (!listenersRegistered) return
        listenersRegistered = false
        try {
            Shizuku.removeBinderReceivedListener(binderReceivedListener)
            Shizuku.removeBinderDeadListener(binderDeadListener)
            Shizuku.removeRequestPermissionResultListener(permissionResultListener)
        } catch (_: Throwable) {
        }
    }

    fun refreshShizukuStatus() {
        try {
            if (!Shizuku.pingBinder()) {
                _shizukuStatus.value = ShizukuStatus.NOT_RUNNING
                return
            }
            val granted = Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            _shizukuStatus.value = if (granted) {
                ShizukuStatus.GRANTED
            } else {
                ShizukuStatus.PERMISSION_REQUIRED
            }
        } catch (_: Throwable) {
            _shizukuStatus.value = ShizukuStatus.NOT_RUNNING
        }
    }

    fun requestShizukuPermission() {
        try {
            if (!Shizuku.pingBinder()) {
                _shizukuStatus.value = ShizukuStatus.NOT_RUNNING
                _commandOutput.value = "Shizuku binder is not active. Start Shizuku app first."
                return
            }
            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                _shizukuStatus.value = ShizukuStatus.GRANTED
            } else if (!Shizuku.shouldShowRequestPermissionRationale()) {
                Shizuku.requestPermission(SHIZUKU_REQ_CODE)
            } else {
                Shizuku.requestPermission(SHIZUKU_REQ_CODE)
            }
        } catch (t: Throwable) {
            _commandOutput.value = "Shizuku error: ${t.message}"
        }
    }

    suspend fun checkRootAccess(): RootStatus = withContext(Dispatchers.IO) {
        val suPaths = listOf("/system/bin/su", "/system/xbin/su", "/sbin/su", "/su/bin/su", "/magisk/.core/bin/su")
        val hasBinary = suPaths.any { File(it).exists() }
        val result = runProcess(arrayOf("su", "-c", "id"))
        val granted = result.exitCode == 0 && result.output.contains("uid=0")
        val status = if (granted) RootStatus.GRANTED else RootStatus.NOT_AVAILABLE
        _rootStatus.value = status
        _commandOutput.value = if (granted) {
            "Root verified: ${result.output.trim()}"
        } else {
            val hint = if (hasBinary) "su binary found, permission denied or cancelled." else "su binary not detected on device."
            "Root check: $hint ${result.output.trim()}".trim()
        }
        status
    }

    suspend fun executeAudioControlCommand(command: String): String = withContext(Dispatchers.IO) {
        val cleanCmd = command.trim()
        if (cleanCmd.isEmpty()) return@withContext ""
        prefs.setCustomShellCommand(cleanCmd)

        val mode = prefs.routingMode.value
        val output = when (mode) {
            AudioRoutingMode.ROOT_HAL_INJECT -> {
                if (_rootStatus.value != RootStatus.GRANTED) {
                    checkRootAccess()
                }
                if (_rootStatus.value == RootStatus.GRANTED) {
                    val res = runProcess(arrayOf("su", "-c", cleanCmd))
                    "[Root exit=${res.exitCode}]\n${res.output.ifBlank { "(no output)" }}"
                } else {
                    "Root permission is not granted. Cannot execute root HAL command."
                }
            }
            AudioRoutingMode.SHIZUKU_SHELL -> {
                refreshShizukuStatus()
                if (_shizukuStatus.value == ShizukuStatus.GRANTED) {
                    val res = runShizukuShell(cleanCmd)
                    "[Shizuku exit=${res.exitCode}]\n${res.output.ifBlank { "(no output)" }}"
                } else {
                    "Shizuku permission is not granted or Shizuku is not running."
                }
            }
            AudioRoutingMode.VOICE_CALL_STREAM -> {
                "Voice Communication stream routing active (USAGE_VOICE_COMMUNICATION)."
            }
            AudioRoutingMode.STANDARD_MEDIA -> {
                "Standard Media stream routing active (USAGE_MEDIA)."
            }
        }
        _commandOutput.value = output
        output
    }

    private data class CmdResult(val exitCode: Int, val output: String)

    private fun runProcess(cmd: Array<String>): CmdResult {
        return try {
            val process = ProcessBuilder(*cmd)
                .redirectErrorStream(true)
                .start()
            val out = BufferedReader(InputStreamReader(process.inputStream)).use { it.readText() }
            val code = process.waitFor()
            CmdResult(code, out.take(1500))
        } catch (t: Throwable) {
            CmdResult(-1, t.message ?: "Execution failed")
        }
    }

    private fun runShizukuShell(command: String): CmdResult {
        return try {
            val method = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            ).apply { isAccessible = true }
            val process = method.invoke(null, arrayOf("sh", "-c", command), null, null) as Process
            val out = BufferedReader(InputStreamReader(process.inputStream)).use { it.readText() }
            val err = BufferedReader(InputStreamReader(process.errorStream)).use { it.readText() }
            val code = process.waitFor()
            val combined = listOf(out, err).filter { it.isNotBlank() }.joinToString("\n")
            CmdResult(code, combined.take(1500))
        } catch (t: Throwable) {
            CmdResult(-1, "Shizuku shell error: ${t.message}")
        }
    }

    companion object {
        private const val SHIZUKU_REQ_CODE = 4401
    }
}
