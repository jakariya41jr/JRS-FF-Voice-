package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.EmeraldOk

@Composable
fun SettingsScreen(
    strings: AppStrings,
    language: AppLanguage,
    darkMode: Boolean,
    savedApiKey: String,
    onSaveApiKey: (String) -> Unit,
    onSetLanguage: (AppLanguage) -> Unit,
    onSetDarkMode: (Boolean) -> Unit,
    onExportBackupClick: () -> Unit,
    onImportBackupClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var apiKeyInput by remember(savedApiKey) { mutableStateOf(savedApiKey) }
    var showKey by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = strings.settingsTitle,
            style = MaterialTheme.typography.titleLarge
        )

        // Cartesia Local API Key Card
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
                        imageVector = Icons.Filled.Key,
                        contentDescription = strings.cartesiaApiKeyTitle,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(strings.cartesiaApiKeyTitle, style = MaterialTheme.typography.titleMedium)
                }

                OutlinedTextField(
                    value = apiKeyInput,
                    onValueChange = { apiKeyInput = it },
                    placeholder = { Text(strings.cartesiaApiKeyHint) },
                    singleLine = true,
                    visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showKey = !showKey }) {
                            Icon(
                                imageVector = if (showKey) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = "Toggle key visibility"
                            )
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("cartesia_api_key_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            apiKeyInput = ""
                            onSaveApiKey("")
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("clear_api_key_button")
                    ) {
                        Text(strings.clear)
                    }
                    Button(
                        onClick = { onSaveApiKey(apiKeyInput) },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("save_api_key_button")
                    ) {
                        Icon(Icons.Filled.Save, contentDescription = strings.save)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.save)
                    }
                }
            }
        }

        // Language & Theme Card
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Language,
                            contentDescription = strings.languageLabel,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.languageLabel, style = MaterialTheme.typography.bodyLarge)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val isBn = language == AppLanguage.BN
                    Button(
                        onClick = { onSetLanguage(AppLanguage.BN) },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("select_lang_bn")
                    ) {
                        Text(if (isBn) "✓ বাংলা" else "বাংলা")
                    }
                    OutlinedButton(
                        onClick = { onSetLanguage(AppLanguage.EN) },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("select_lang_en")
                    ) {
                        Text(if (!isBn) "✓ English" else "English")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(strings.darkModeLabel, style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = darkMode,
                        onCheckedChange = onSetDarkMode,
                        modifier = Modifier.testTag("settings_dark_mode_switch")
                    )
                }
            }
        }

        // Backup & Restore (.zip)
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
                Text(strings.backupSectionTitle, style = MaterialTheme.typography.titleMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onExportBackupClick,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("export_backup_button")
                    ) {
                        Icon(Icons.Filled.Upload, contentDescription = strings.exportBackup)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.exportBackup, maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = onImportBackupClick,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("import_backup_button")
                    ) {
                        Icon(Icons.Filled.Download, contentDescription = strings.importBackup)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.importBackup, maxLines = 1)
                    }
                }
            }
        }

        // Offline & Privacy Guarantee Card
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = EmeraldOk.copy(alpha = 0.10f),
            border = BorderStroke(1.dp, EmeraldOk.copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.CloudOff,
                        contentDescription = strings.offlineNoticeTitle,
                        tint = EmeraldOk
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = strings.offlineNoticeTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldOk
                    )
                }
                Text(
                    text = strings.offlineNoticeBody,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
