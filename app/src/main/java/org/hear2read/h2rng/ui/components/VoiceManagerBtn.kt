package org.hear2read.h2rng.ui.components

import androidx.compose.runtime.Composable

/**
 * Interface to create floating button navigating to VoiceManagerScreen. Empty implementation in
 * "local" flavor
 *
 */
interface VoiceManagerBtn {
    /**
     * Floating button that opens the VoiceManagerScreen in the "pad"(production) flavor
     *
     * @param onInstallVoiceClick onClick behaviour of the button
     * @receiver
     */
    @Composable
    fun floatingButton(onInstallVoiceClick: () -> Unit)
}