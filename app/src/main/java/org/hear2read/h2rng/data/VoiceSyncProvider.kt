package org.hear2read.h2rng.data

import data.VoiceSyncImpl

/**
 * Provider for relevant implementation of [VoiceSync] interface depending on the flavour
 *
 */
class VoiceSyncProvider {
    fun get(): VoiceSync = VoiceSyncImpl()
}