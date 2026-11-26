package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.prefs.AudioRoutingMode
import com.example.system.RootStatus
import com.example.system.ShizukuStatus
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.AmberStar
import com.example.ui.theme.CoralStop
import com.example.ui.theme.EmeraldOk

@Composable
fun SystemOverlayScreen(
    strings: AppStrings,
    hasOverlayPermission: Boolean,
    isOverlayRunning: Boolean,
    rootStatus: RootStatus,
    shizukuStatus: ShizukuStatus,
    routingMode: AudioRoutingMode,
    customShellCommand: String,
    commandOutput: String,
    onRequestOverlayPermission: () -> Unit,
    onToggleOverlay: (Boolean) -> Unit,
    onCheckRoot: () -> Unit,
    onRefreshShizuku: () -> Unit,
    onRequestShizuku: () -> Unit,
    onSelectRoutingMode: (AudioRoutingMode) -> Unit,
    onExecuteShellCommand: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var shellInput by remember(customShellCommand) { mutableStateOf(customShellCommand) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = strings.systemControlTitle,
            style = MaterialTheme.typography.titleLarge
        )

        // 1. Floating Overlay Card
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Layers,
                        contentDescription = strings.overlayCardTitle,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(strings.overlayCardTitle, style = MaterialTheme.typography.titleMedium)
                }

                StatusPill(
                    text = if (hasOverlayPermission) strings.overlayPermissionGranted else strings.overlayPermissionMissing,
                    color = if (hasOverlayPermission) EmeraldOk else AmberStar
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!hasOverlayPermission) {
                        Button(
                            onClick = onRequestOverlayPermission,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("grant_overlay_button")
                        ) {
                            Text(strings.grantOverlayPermission)
                        }
                    } else {
                        Button(
                            onClick = { onToggleOverlay(!isOverlayRunning) },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("toggle_overlay_service_button")
                        ) {
                            Text(if (isOverlayRunning) strings.stopOverlay else strings.startOverlay)
                        }
                    }
                }
            }
        }

        // 2. Root Status Card
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Security,
                        contentDescription = strings.rootCardTitle,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(strings.rootCardTitle, style = MaterialTheme.typography.titleMedium)
                }

                val rootText = when (rootStatus) {
                    RootStatus.GRANTED -> strings.rootGranted
                    RootStatus.NOT_AVAILABLE -> strings.rootNotAvailable
                    RootStatus.UNKNOWN -> strings.checkRoot
                }
                val rootColor = when (rootStatus) {
                    RootStatus.GRANTED -> EmeraldOk
                    RootStatus.NOT_AVAILABLE -> CoralStop
                    RootStatus.UNKNOWN -> AmberStar
                }
                StatusPill(text = rootText, color = rootColor)

                OutlinedButton(
                    onClick = onCheckRoot,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("check_root_button")
                ) {
                    Icon(Icons.Filled.AdminPanelSettings, contentDescription = strings.checkRoot)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(strings.checkRoot)
                }
            }
        }

        // 3. Shizuku Status Card
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Build,
                        contentDescription = strings.shizukuCardTitle,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(strings.shizukuCardTitle, style = MaterialTheme.typography.titleMedium)
                }

                val (shizukuText, shizukuColor) = when (shizukuStatus) {
                    ShizukuStatus.GRANTED -> strings.shizukuGranted to EmeraldOk
                    ShizukuStatus.PERMISSION_REQUIRED -> strings.shizukuNeedsPermission to AmberStar
                    ShizukuStatus.NOT_RUNNING -> strings.shizukuNotRunning to CoralStop
                }
                StatusPill(text = shizukuText, color = shizukuColor)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onRefreshShizuku,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("refresh_shizuku_button")
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = strings.checkShizuku)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(strings.checkShizuku)
                    }
                    Button(
                        onClick = onRequestShizuku,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("request_shizuku_button")
                    ) {
                        Text(strings.requestShizuku)
                    }
                }
            }
        }

        // 4. Audio Injection & Routing Controls
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Terminal,
                        contentDescription = strings.audioInjectionTitle,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(strings.audioInjectionTitle, style = MaterialTheme.typography.titleMedium)
                }

                val modes = listOf(
                    AudioRoutingMode.STANDARD_MEDIA to strings.modeStandardMedia,
                    AudioRoutingMode.VOICE_CALL_STREAM to strings.modeVoiceCall,
                    AudioRoutingMode.ROOT_HAL_INJECT to strings.modeRootHal,
                    AudioRoutingMode.SHIZUKU_SHELL to strings.modeShizukuShell
                )
                modes.forEach { (mode, label) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelectRoutingMode(mode) }
                            .padding(vertical = 4.dp)
                            .testTag("routing_mode_${mode.key}")
                    ) {
                        RadioButton(
                            selected = routingMode == mode,
                            onClick = { onSelectRoutingMode(mode) }
                        )
                        Text(label, style = MaterialTheme.typography.bodyLarge)
                    }
                }

                OutlinedTextField(
                    value = shellInput,
                    onValueChange = { shellInput = it },
                    label = { Text(strings.customShellCmdLabel) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("audio_shell_cmd_input")
                )

                Button(
                    onClick = { onExecuteShellCommand(shellInput) },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("run_audio_shell_button")
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = strings.runAudioCommand)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.runAudioCommand)
                }

                if (commandOutput.isNotBlank()) {
                    Text(
                        text = strings.commandOutputLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = commandOutput,
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusPill(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.55f))
    ) {
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}
