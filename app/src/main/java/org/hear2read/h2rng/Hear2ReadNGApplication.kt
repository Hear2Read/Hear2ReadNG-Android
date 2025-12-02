package org.hear2read.h2rng

import android.app.Application
import android.util.Log
import com.google.android.play.core.assetpacks.AssetPackManager
import com.google.android.play.core.assetpacks.AssetPackManagerFactory
import com.google.android.play.core.assetpacks.model.AssetPackStatus
import com.google.android.play.core.ktx.requestPackStates
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.hear2read.h2rng.data.H2RVoiceRepository

/**
 * Application instance to initialize the H2RVoiceRepository used by both the MainActivity and the
 * TextToSpeechService. This is done to avoid duplicating the repository initialization.
 *
 */
class Hear2ReadNGApplication : Application() {

    val LOG_TAG = "H2RNG_" + Application::class.simpleName

    // Application-level coroutine scope
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // Shared instances
    lateinit var assetPackManager: AssetPackManager
        private set

    // Track voices initialization state
    private var isRepositoryInitialized = false

    override fun onCreate() {
        super.onCreate()

        // Initialize AssetPackManager (doesn't require async)
        assetPackManager = AssetPackManagerFactory.getInstance(this)

        // Initialize VoiceRepository asynchronously
        // We don't block here - initialization happens in background

        initializeVoiceRepository()
    }

    private fun initializeVoiceRepository() {
        applicationScope.launch {
            H2RVoiceRepository.initializeDefault()
            syncDownloadStates()
            isRepositoryInitialized = true
        }
    }

    /**
     * Sync download states
     *
     */
    suspend fun syncDownloadStates() {
        val voices = H2RVoiceRepository.getCurrentVoices()
        val packList = voices.map{voice -> voice.assetID }
        Log.d(LOG_TAG, "syncDownloadStates: packlist: $packList")
        val packStates = assetPackManager.requestPackStates(packList)

        voices.forEach { voice ->
            val packState = packStates.packStates()[voice.assetID]
            Log.d(LOG_TAG, "syncDownloadStates: packstate of: ${voice.id} is ${packState.toString()}")

            if (packState != null) {
                val isDownloaded = packState.status() == AssetPackStatus.COMPLETED
                val size = packState.totalBytesToDownload()

                Log.d(LOG_TAG, "syncDownloadStates: adding: ${voice.id} isDownloaded? ${isDownloaded}")
                H2RVoiceRepository.updateDownloadInfo(voice.assetID, isDownloaded, size)
            }
        }
//        isRepoSynced = true
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