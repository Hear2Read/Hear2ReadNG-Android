package org.hear2read.h2rng.data

import android.content.Context

/**
 * Interface providing functionality to update the voice repository
 *
 */
interface VoiceSync {
    /**
     * Function syncing voice state in repository.
     *
     * @param context Used to instantiate relevant Asset delivery manager
     */
    suspend fun syncVoiceStates(context: Context)
}