package com.example.ui.navigation

import kotlinx.serialization.Serializable

sealed interface AppRoute {
    @Serializable
    data object Soundboard : AppRoute

    @Serializable
    data object VoiceStudio : AppRoute

    @Serializable
    data object SystemOverlay : AppRoute

    @Serializable
    data object Settings : AppRoute
}
