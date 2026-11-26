package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.db.SoundCardEntity
import com.example.ui.SoundFilter
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.AmberStar
import com.example.ui.theme.CoralStop
import com.example.ui.theme.EmeraldOk
import kotlin.math.roundToInt

@Composable
fun DashboardScreen(
    strings: AppStrings,
    cards: List<SoundCardEntity>,
    playingCardId: Long?,
    volume: Float,
    searchQuery: String,
    selectedFilter: SoundFilter,
    isOverlayRunning: Boolean,
    onVolumeChange: (Float) -> Unit,
    onSearchChange: (String) -> Unit,
    onFilterChange: (SoundFilter) -> Unit,
    onPlayToggle: (SoundCardEntity) -> Unit,
    onFavoriteToggle: (SoundCardEntity) -> Unit,
    onRenameCard: (Long, String) -> Unit,
    onDeleteCard: (SoundCardEntity) -> Unit,
    onImportMediaClick: () -> Unit,
    onOverlayToggle: (Boolean) -> Unit,
    onStopAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    var renamingCard by remember { mutableStateOf<SoundCardEntity?>(null) }
    var renameInput by remember { mutableStateOf("") }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 158.dp),
        modifier = modifier
            .fillMaxSize()
            .testTag("soundboard_grid"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Master Studio Console Header (Volume + Floating Overlay + All Stop)
        item(span = { GridItemSpan(maxLineSpan) }) {
            MasterConsolePanel(
                strings = strings,
                volume = volume,
                isOverlayRunning = isOverlayRunning,
                isPlaying = playingCardId != null,
                onVolumeChange = onVolumeChange,
                onOverlayToggle = onOverlayToggle,
                onImportMediaClick = onImportMediaClick,
                onStopAll = onStopAll
            )
        }

        // Search & Filter Bar
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_cards_input"),
                    placeholder = { Text(strings.searchHint) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = strings.searchHint
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { onSearchChange("") },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = strings.clear
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val filters = listOf(
                        SoundFilter.ALL to strings.filterAll,
                        SoundFilter.FAVORITES to strings.filterFavorites,
                        SoundFilter.TTS to strings.filterTts,
                        SoundFilter.IMPORTED to strings.filterImported
                    )
                    filters.forEach { (filter, label) ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { onFilterChange(filter) },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("filter_chip_${filter.name.lowercase()}")
                        )
                    }
                }
            }
        }

        if (cards.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                EmptySoundboardCard(
                    strings = strings,
                    onImportMediaClick = onImportMediaClick
                )
            }
        } else {
            items(cards, key = { it.id }) { card ->
                SoundPadCard(
                    card = card,
                    isPlaying = card.id == playingCardId,
                    strings = strings,
                    onTap = { onPlayToggle(card) },
                    onFavoriteToggle = { onFavoriteToggle(card) },
                    onRenameClick = {
                        renamingCard = card
                        renameInput = card.title
                    },
                    onDeleteClick = { onDeleteCard(card) }
                )
            }
        }
    }

    val targetRename = renamingCard
    if (targetRename != null) {
        AlertDialog(
            onDismissRequest = { renamingCard = null },
            title = { Text(strings.renameCard) },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    label = { Text(strings.cardTitleLabel) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rename_card_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRenameCard(targetRename.id, renameInput)
                        renamingCard = null
                    },
                    modifier = Modifier.testTag("confirm_rename_button")
                ) {
                    Text(strings.save)
                }
            },
            dismissButton = {
                TextButton(onClick = { renamingCard = null }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}

@Composable
private fun MasterConsolePanel(
    strings: AppStrings,
    volume: Float,
    isOverlayRunning: Boolean,
    isPlaying: Boolean,
    onVolumeChange: (Float) -> Unit,
    onOverlayToggle: (Boolean) -> Unit,
    onImportMediaClick: () -> Unit,
    onStopAll: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        tonalElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.06f)
                        )
                    )
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Volume row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = strings.volume,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${strings.volume} ${(volume * 100).roundToInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.width(105.dp)
                )
                Slider(
                    value = volume,
                    onValueChange = onVolumeChange,
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("master_volume_slider")
                )
            }

            // Quick Actions Row: Import Audio/Video, Floating Overlay Switch, All Stop
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onImportMediaClick,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("import_media_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = strings.importMedia,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = strings.importMedia,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        1.dp,
                        if (isOverlayRunning) EmeraldOk else MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Layers,
                            contentDescription = strings.floatingOverlay,
                            tint = if (isOverlayRunning) EmeraldOk else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isOverlayRunning) strings.overlayRunning else strings.overlayStopped,
                            style = MaterialTheme.typography.labelMedium
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Switch(
                            checked = isOverlayRunning,
                            onCheckedChange = onOverlayToggle,
                            modifier = Modifier.testTag("dashboard_overlay_switch")
                        )
                    }
                }
            }

            if (isPlaying) {
                Button(
                    onClick = onStopAll,
                    colors = ButtonDefaults.buttonColors(containerColor = CoralStop),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("banner_all_stop_button")
                ) {
                    Icon(Icons.Filled.Stop, contentDescription = strings.allStop)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(strings.allStop, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SoundPadCard(
    card: SoundCardEntity,
    isPlaying: Boolean,
    strings: AppStrings,
    onTap: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onRenameClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val padColor = Color(card.colorHex)
    val borderColor by animateColorAsState(
        targetValue = if (isPlaying) padColor else MaterialTheme.colorScheme.outline,
        label = "padBorder"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.14f,
        targetValue = 0.32f,
        animationSpec = infiniteRepeatable(
            animation = tween(550),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Card(
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(if (isPlaying) 2.dp else 1.dp, borderColor),
        colors = CardDefaults.cardColors(
            containerColor = if (isPlaying) padColor.copy(alpha = pulseAlpha)
            else MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(142.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onTap)
            .testTag("sound_card_${card.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(padColor.copy(alpha = 0.20f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.GraphicEq else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) strings.playingNow else strings.tapToPlay,
                        tint = padColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onFavoriteToggle,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("favorite_card_${card.id}")
                    ) {
                        Icon(
                            imageVector = if (card.isFavorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                            contentDescription = strings.favorite,
                            tint = if (card.isFavorite) AmberStar else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("menu_card_${card.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = "Options",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(strings.renameCard) },
                                leadingIcon = {
                                    Icon(Icons.Filled.Edit, contentDescription = strings.renameCard)
                                },
                                onClick = {
                                    menuExpanded = false
                                    onRenameClick()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(strings.deleteCard, color = CoralStop) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = strings.deleteCard,
                                        tint = CoralStop
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    onDeleteClick()
                                }
                            )
                        }
                    }
                }
            }

            Column {
                Text(
                    text = card.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isPlaying) strings.playingNow else card.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isPlaying) padColor else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (card.durationMs > 0) {
                        Text(
                            text = String.format("%.1fs", card.durationMs / 1000f),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptySoundboardCard(
    strings: AppStrings,
    onImportMediaClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.AudioFile,
                contentDescription = strings.emptyBoardTitle,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = strings.emptyBoardTitle,
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = strings.emptyBoardSubtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onImportMediaClick,
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = strings.importMedia)
                Spacer(modifier = Modifier.width(6.dp))
                Text(strings.importMedia)
            }
        }
    }
}
