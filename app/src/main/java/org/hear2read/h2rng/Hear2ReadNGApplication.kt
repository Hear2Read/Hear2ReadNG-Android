package org.hear2read.h2rng

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.hear2read.h2rng.data.H2RVoiceRepository
import org.hear2read.h2rng.data.VoiceSyncProvider
import org.hear2read.h2rng.utils.LOG_PREFIX
import org.hear2read.h2rng.utils.copyDataDir

/**
 * Application instance to initialize the H2RVoiceRepository used by both the MainActivity and the
 * TextToSpeechService. This is done to avoid duplicating the repository initialization.
 *
 */
class Hear2ReadNGApplication : Application() {

    val LOG_TAG = LOG_PREFIX + Application::class.simpleName

    // Application-level coroutine scope
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val context = this

    override fun onCreate() {
        super.onCreate()
        // Initialize VoiceRepository asynchronously
        // We don't block here - initialization happens in background
        copyDataDir(this, "espeak-ng-data")
        initializeVoiceRepository()
    }

    private fun initializeVoiceRepository() {
        applicationScope.launch {
            H2RVoiceRepository.initializeDefault()
            VoiceSyncProvider().get().syncVoiceStates(context)
            H2RVoiceRepository.markAsReady()
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        // Note: onTerminate() is never called in production, only in emulator
        // Real cleanup should be in Activity/Service onDestroy
    }
}