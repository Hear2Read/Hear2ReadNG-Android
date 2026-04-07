package org.hear2read.h2rng

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.hear2read.h2rng.data.H2RVoiceRepository
import org.hear2read.h2rng.data.VoiceSyncProvider
import org.hear2read.h2rng.utils.copyDataDir

/**
 * Application instance to initialize the H2RVoiceRepository used by both the MainActivity and the
 * TextToSpeechService. This is done to avoid duplicating the repository initialization.
 *
 */
class Hear2ReadNGApplication : Application() {

    val LOG_TAG = "H2RNG_" + Application::class.simpleName

    // Application-level coroutine scope
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val context = this

    // Track voices initialization state
    private var isRepositoryInitialized = false

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
//            syncDownloadStates()
            VoiceSyncProvider().get().syncVoiceStates(context)
            isRepositoryInitialized = true
        }
    }


    /**
     * Function to query state of repository initialization. Used in the MainActivity and the TTS
     * service to check for initialzation.
     *
     * @return
     */
    fun isVoiceRepositoryReady(): Boolean = isRepositoryInitialized

    override fun onTerminate() {
        super.onTerminate()
        // Note: onTerminate() is never called in production, only in emulator
        // Real cleanup should be in Activity/Service onDestroy
    }
}