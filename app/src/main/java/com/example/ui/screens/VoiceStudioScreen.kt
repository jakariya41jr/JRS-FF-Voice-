package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Man
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Woman
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.db.VoiceProfileEntity
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.CoralStop
import com.example.ui.theme.ElectricIndigo
import com.example.ui.theme.EmeraldOk
import com.example.ui.theme.NeonCyan

@Composable
fun VoiceStudioScreen(
    strings: AppStrings,
    language: AppLanguage,
    hasCartesiaKey: Boolean,
    profiles: List<VoiceProfileEntity>,
    selectedProfile: VoiceProfileEntity?,
    selectedGender: String,
    pitch: Float,
    speed: Float,
    editableVoiceId: String,
    ttsText: String,
    isSynthesizing: Boolean,
    onSelectGender: (String) -> Unit,
    onSelectProfile: (VoiceProfileEntity) -> Unit,
    onPitchChange: (Float) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onVoiceIdChange: (String) -> Unit,
    onSaveVoiceParams: () -> Unit,
    onPreviewVoice: () -> Unit,
    onTtsTextChange: (String) -> Unit,
    onPlayDirect: () -> Unit,
    onSaveToCard: () -> Unit,
    onAddCustomProfile: (name: String, gender: String, lang: String, voiceId: String) -> Unit,
    onDeleteCustomProfile: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Engine Mode Indicator Banner
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (hasCartesiaKey) EmeraldOk.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(
                1.dp,
                if (hasCartesiaKey) EmeraldOk.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.RecordVoiceOver,
                    contentDescription = strings.voiceStudioTitle,
                    tint = if (hasCartesiaKey) EmeraldOk else NeonCyan
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (hasCartesiaKey) strings.cartesiaReady else strings.offlineTtsFallback,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Gender Switch: Male vs Female
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = strings.genderSwitchLabel,
                    style = MaterialTheme.typography.titleMedium
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GenderToggleCard(
                        label = strings.femaleVoice,
                        icon = Icons.Filled.Woman,
                        selected = selectedGender == "FEMALE",
                        accent = ElectricIndigo,
                        onClick = { onSelectGender("FEMALE") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("gender_female_button")
                    )
                    GenderToggleCard(
                        label = strings.maleVoice,
                        icon = Icons.Filled.Man,
                        selected = selectedGender == "MALE",
                        accent = NeonCyan,
                        onClick = { onSelectGender("MALE") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("gender_male_button")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.voiceProfilesLabel,
                        style = MaterialTheme.typography.labelLarge
                    )
                    TextButton(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.testTag("add_custom_voice_button")
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = strings.addCustomVoice, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(strings.addCustomVoice)
                    }
                }

                // Voice Profile Picker Cards
                val filteredProfiles = profiles.filter { it.gender == selectedGender }.ifEmpty { profiles }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    filteredProfiles.forEach { profile ->
                        val isSelected = selectedProfile?.id == profile.id
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                if (isSelected) 1.5.dp else 1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onSelectProfile(profile) }
                                .testTag("voice_profile_${profile.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (language == AppLanguage.BN) profile.nameBn else profile.nameEn,
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                    Text(
                                        text = "${profile.languageCode.uppercase()} • ${profile.gender}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (profile.isCustom) {
                                    IconButton(
                                        onClick = { onDeleteCustomProfile(profile.id) },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.DeleteOutline,
                                            contentDescription = strings.deleteVoiceProfile,
                                            tint = CoralStop
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Voice Parameters (Pitch, Speed, Voice ID, Preview)
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "${strings.pitchLabel}: ${String.format("%.2f", pitch)}x",
                    style = MaterialTheme.typography.labelLarge
                )
                Slider(
                    value = pitch,
                    onValueChange = onPitchChange,
                    valueRange = 0.6f..1.6f,
                    modifier = Modifier.testTag("voice_pitch_slider")
                )

                Text(
                    text = "${strings.speedLabel}: ${String.format("%.2f", speed)}x",
                    style = MaterialTheme.typography.labelLarge
                )
                Slider(
                    value = speed,
                    onValueChange = onSpeedChange,
                    valueRange = 0.6f..1.6f,
                    modifier = Modifier.testTag("voice_speed_slider")
                )

                OutlinedTextField(
                    value = editableVoiceId,
                    onValueChange = onVoiceIdChange,
                    label = { Text(strings.cartesiaVoiceIdLabel) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("cartesia_voice_id_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onPreviewVoice,
                        enabled = !isSynthesizing,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("preview_voice_button")
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = strings.previewVoice)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.previewVoice)
                    }
                    OutlinedButton(
                        onClick = onSaveVoiceParams,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("save_voice_params_button")
                    ) {
                        Icon(Icons.Filled.Save, contentDescription = strings.save)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(strings.save)
                    }
                }
            }
        }

        // Text-to-Voice Box -> Play Direct or Save to Card
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = strings.ttsInputLabel,
                    style = MaterialTheme.typography.titleMedium
                )
                OutlinedTextField(
                    value = ttsText,
                    onValueChange = onTtsTextChange,
                    placeholder = { Text(strings.ttsInputHint) },
                    minLines = 3,
                    maxLines = 5,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tts_text_input")
                )

                Text(
                    text = strings.samplePhrases,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val samples = listOf(
                        "হ্যালো বন্ধুরা, সবাই কেমন আছেন?",
                        "একটু চুপ থাকো, গেম শুরু হচ্ছে!",
                        "नमस्ते दोस्तों, स्वागत है!",
                        "Let's go team, ready now!"
                    )
                    samples.forEach { sample ->
                        AssistChip(
                            onClick = { onTtsTextChange(sample) },
                            label = { Text(sample, maxLines = 1) }
                        )
                    }
                }

                if (isSynthesizing) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.generatingVoice, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onPlayDirect,
                        enabled = ttsText.isNotBlank() && !isSynthesizing,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("tts_play_direct_button")
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = strings.playDirect)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.playDirect)
                    }

                    Button(
                        onClick = onSaveToCard,
                        enabled = ttsText.isNotBlank() && !isSynthesizing,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("tts_save_card_button")
                    ) {
                        Icon(Icons.Filled.BookmarkAdd, contentDescription = strings.saveToCard)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.saveToCard)
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddCustomVoiceDialog(
            strings = strings,
            initialGender = selectedGender,
            onDismiss = { showAddDialog = false },
            onSave = { name, gender, lang, voiceId ->
                onAddCustomProfile(name, gender, lang, voiceId)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun GenderToggleCard(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    accent: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (selected) accent.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) accent else MaterialTheme.colorScheme.outline
        ),
        modifier = modifier
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AddCustomVoiceDialog(
    strings: AppStrings,
    initialGender: String,
    onDismiss: () -> Unit,
    onSave: (name: String, gender: String, lang: String, voiceId: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf(initialGender) }
    var lang by remember { mutableStateOf("bn") }
    var voiceId by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.addCustomVoice) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(strings.voiceNameLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { gender = "FEMALE" },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (gender == "FEMALE") "✓ ${strings.femaleVoice}" else strings.femaleVoice)
                    }
                    OutlinedButton(
                        onClick = { gender = "MALE" },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (gender == "MALE") "✓ ${strings.maleVoice}" else strings.maleVoice)
                    }
                }
                OutlinedTextField(
                    value = lang,
                    onValueChange = { lang = it },
                    label = { Text(strings.voiceLanguageLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = voiceId,
                    onValueChange = { voiceId = it },
                    label = { Text(strings.cartesiaVoiceIdLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, gender, lang, voiceId) },
                enabled = name.isNotBlank()
            ) {
                Text(strings.save)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        }
    )
}
