package org.hear2read.h2rng.utils

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

// Constants for the lag tag
private val FILENAME = "FileUtils"
private val LOG_TAG = "H2RNG_${FILENAME}"

/**
 * Helper to copy asset file to internal storage
 * TODO: we are only using the asset path as returned by the AssetManager so this is not useful
 *
 * @param context [Context] to access the asset file TODO: replace with path
 * @param filename Name of the file to be copied
 */
fun copyFile(context: Context, filename: String) {
    try {
        val istream = context.assets.open(filename)
        val newFilename = context.filesDir.absolutePath + "/" + filename

//        println("newFilename: $newFilename")

        val ostream = FileOutputStream(newFilename)
//        Log.d(LOG_TAG, "Copying $filename to $newFilename")
        val buffer = ByteArray(1024)
        var read = 0
        while (read != -1) {
            ostream.write(buffer, 0, read)
            read = istream.read(buffer)
        }
        istream.close()
        ostream.flush()
        ostream.close()
    } catch (ex: Exception) {
        Log.e(LOG_TAG,"Failed to copy file $filename: ${ex.message}")
    }
}

/**
 * Copy assets to internal storage? Not sure what this is doing, seems only for the data assets
 * TODO: we are only using the asset path as returned by the AssetManager so this is not useful?
 *
 * @param context [Context] used to access filesystem
 * @param path Path of asset folder
 */
fun copyAssets(context: Context, path: String) {
    val assets: Array<String>?
    try {
        assets = context.assets.list(path)
        if (assets!!.isEmpty()) {
            copyFile(context, path)
        } else {
            val fullPath = "${context.filesDir.absolutePath}/$path"
            val dir = File(fullPath)
            dir.mkdirs()
            for (asset in assets.iterator()) {
                val p: String = if (path == "") "" else "$path/"
                copyAssets(context, p + asset)
            }
        }
    } catch (ex: IOException) {
        Log.e(LOG_TAG,"Failed to copy asset $path: ${ex.message}")
    }
}

/**
 * Copy data dir containing the phonemization data for the TTS engine
 *
 * @param context
 * @param dataDir
 * @return
 */
fun copyDataDir(context: Context, dataDir: String): String {
    Log.d(LOG_TAG,"data dir is $dataDir")
    copyAssets(context, dataDir)

    val newDataDir = File(context.filesDir!!.absolutePath, dataDir)
    Log.d(LOG_TAG,"newDataDir: ${newDataDir.absolutePath}")
    return newDataDir.absolutePath
}

/**
 * Get file in directory with the extension provided
 *
 * @param directory The directory to search
 * @param extension The extension to match
 * @return
 */
fun getFileWithExtension(directory: String, extension: String) : File? {
    val files = File(directory).listFiles()
    if (files != null) {
        Log.d(LOG_TAG, "getFileWithExtension found files: ${files.map { it.name }}")
    }
    if (files != null) {
        for (file in files) {
            if (file.isFile && file.name.endsWith(extension)) {
                return file
            }
        }
    }

    return null
}