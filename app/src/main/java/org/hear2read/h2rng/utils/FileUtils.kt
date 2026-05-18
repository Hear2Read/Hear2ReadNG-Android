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

import org.hear2read.h2rng.BuildConfig

// Constants for the lag tag
private val FILENAME = "FileUtils"
private val LOG_TAG = "H2RNG_${FILENAME}"

/**
 * Helper to copy asset file to internal storage
 * TODO: remove
 *
 * @param context [Context] to access the asset file
 * @param filename Name of the file to be copied
 */
fun copyAssetFile(context: Context, filename: String) {
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

fun copyAssetFile(context: Context, filename: String, overWrite: Boolean): String {
    val destFile = File(context.filesDir, filename)

    if (destFile.exists() && !overWrite) return destFile.absolutePath  // already extracted, skip

    Log.d(LOG_TAG, "copyAssetFile $filename, to: $destFile.absolutePath")

    context.assets.open(filename).use { input ->
        destFile.outputStream().use { output ->
            input.copyTo(output)
        }
    }
    return destFile.absolutePath
}

fun copyAssetsRecur(context: Context, path: String, overWrite: Boolean): Boolean {
    val assets: Array<String>?
    assets = context.assets.list(path)
    Log.d(LOG_TAG, "Checking assets at $path, found: $assets")
    var success = true
    try {
        if (assets!!.isEmpty()) {
            Log.d(LOG_TAG, "Asset is a file: $path")
            copyAssetFile(context, path, overWrite)
        } else {
            val fullPath = "${context.filesDir.absolutePath}/$path"
            Log.d(LOG_TAG, "Making dir $fullPath")
            val dir = File(fullPath)
            dir.mkdirs()
            for (asset in assets.iterator()) {
                Log.d(LOG_TAG, "Recursing on $asset")
                val p: String = if (path == "") "" else "$path/"
                success = success and copyAssetsRecur(context, p + asset, overWrite)
            }
        }
        return success
    } catch (ex: IOException) {
        Log.e(LOG_TAG,"Failed to copy asset $path: ${ex.message}")
        return false
    }
}

/**
 * Copy assets to internal storage? Not sure what this is doing, seems only for the data assets
 *
 * @param context [Context] used to access filesystem
 * @param path Path of asset folder
 */
fun copyAssets(context: Context, path: String) {
    val currentVersion = BuildConfig.VERSION_CODE
    val prefs = context.getSharedPreferences("asset_versions", Context.MODE_PRIVATE)
    val extractedVersion = prefs.getInt("current_version", -1)
    val overWrite = extractedVersion != currentVersion

    val success = copyAssetsRecur(context, path, overWrite)

    if (success)
        prefs.edit().putInt("current_version", currentVersion).apply()
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
 * TODO: remove redundant - directly check file as we know the name
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
 * @param zipFile file to unzip
 * @param wavDir dir to unzip to
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