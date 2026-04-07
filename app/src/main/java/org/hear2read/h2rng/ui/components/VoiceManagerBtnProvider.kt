package org.hear2read.h2rng.ui.components

import androidx.compose.runtime.Composable
import ui.components.VoiceManagerBtnImpl

/**
 * Provider providing flavor dependent version of the implementation of the VoiceManagerBtn
 * interface
 *
 * @constructor
 */
object VoiceManagerBtnProvider {
    /**
     * Function returning the flavor dependent implementation of the VoiceManagerBtn interface
     *
     */
    @Composable
    fun get() = VoiceManagerBtnImpl()
}