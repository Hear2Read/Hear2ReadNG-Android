package org.hear2read.h2rng.utils

import android.content.Context
import android.util.Log
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

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
        Log.d(LOG_TAG,"Checking assets at $path, found: $assets")
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

/**
 * Unzip wavs to directory specified. Based on https://stackoverflow.com/a/64294854
 *
 * @param zipFile
 * @param wavDir
 */
fun unzipWavs(zipFile : File, wavDir : File) {
    Log.d(LOG_TAG, "unzipWavs: $zipFile to $wavDir")
    ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { inStream ->
        if (wavDir.exists() && !wavDir.isDirectory) {
//            throw IllegalStateException("wavDir file must be directory or not exist")
            Log.e(LOG_TAG, "wavDir file must be directory or not exist")
            wavDir.delete()
            }

        if (!wavDir.isDirectory) wavDir.mkdirs()

        val wavDirPath = wavDir.absolutePath.let {
            if (!it.endsWith(File.separator)) "$it${File.separator}"
            else it
        }

        var zipEntry: ZipEntry?
        var unzipFile: File
        var unzipParentDir: File?

        while (inStream.nextEntry.also { zipEntry = it } != null) {
            unzipFile = File(wavDirPath + zipEntry!!.name)
            if (zipEntry!!.isDirectory) {
                Log.d(LOG_TAG, "unzipWavs: directory $zipEntry")
                if (!unzipFile.isDirectory) unzipFile.mkdirs()
            } else {
                unzipParentDir = unzipFile.parentFile
                if (unzipParentDir != null && !unzipParentDir.isDirectory) {
                    unzipParentDir.mkdirs()
                }
                BufferedOutputStream(FileOutputStream(unzipFile)).use { outStream ->
                    inStream.copyTo(outStream)
                }
            }
        }
    }
}