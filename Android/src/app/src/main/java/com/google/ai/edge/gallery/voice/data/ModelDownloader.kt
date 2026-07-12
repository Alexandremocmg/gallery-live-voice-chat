package com.google.ai.edge.gallery.voice.data

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import android.util.Log
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.io.File
import java.security.MessageDigest

class ModelDownloader(private val context: Context) {

    companion object {
        const val MODEL_FILE_NAME = "gemma-4-e2b.tflite"
        // Placeholder for the actual Github Release model URL
        const val MODEL_URL = "https://github.com/google-ai-edge/gallery/releases/download/v1.0/gemma-4-e2b.tflite"
        // Expected SHA-256 for validation (placeholder, adjust in production)
        const val EXPECTED_SHA256 = ""
    }

    private val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    fun isModelDownloaded(): Boolean {
        val file = File(context.filesDir, MODEL_FILE_NAME)
        return file.exists() // In production, we could also check hash here
    }

    fun downloadModel(): Flow<DownloadState> = callbackFlow {
        if (isModelDownloaded()) {
            trySend(DownloadState.Success(File(context.filesDir, MODEL_FILE_NAME)))
            close()
            return@callbackFlow
        }

        trySend(DownloadState.Downloading)

        val request = DownloadManager.Request(Uri.parse(MODEL_URL))
            .setTitle("Downloading Kabem Voice Model")
            .setDescription("Downloading the Gemma language model for local inference")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, MODEL_FILE_NAME)

        val downloadId = downloadManager.enqueue(request)

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id == downloadId) {
                    val query = DownloadManager.Query().setFilterById(downloadId)
                    val cursor = downloadManager.query(query)
                    if (cursor.moveToFirst()) {
                        val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                        val status = if (statusIndex >= 0) cursor.getInt(statusIndex) else -1
                        if (status == DownloadManager.STATUS_SUCCESSFUL) {
                            val uriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                            if (uriIndex >= 0) {
                                val localUri = cursor.getString(uriIndex)
                                val downloadedFile = File(Uri.parse(localUri).path!!)
                                
                                // Move to internal filesDir for security and private access
                                val targetFile = File(this@ModelDownloader.context.filesDir, MODEL_FILE_NAME)
                                downloadedFile.copyTo(targetFile, overwrite = true)
                                downloadedFile.delete()
                                
                                if (validateHash(targetFile)) {
                                    trySend(DownloadState.Success(targetFile))
                                } else {
                                    targetFile.delete()
                                    trySend(DownloadState.Error("Model validation failed (SHA-256 mismatch)."))
                                }
                            }
                        } else {
                            trySend(DownloadState.Error("Download failed with status: $status"))
                        }
                    }
                    cursor.close()
                    close()
                }
            }
        }

        context.registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_EXPORTED)

        awaitClose {
            context.unregisterReceiver(receiver)
        }
    }

    private fun validateHash(file: File): Boolean {
        if (EXPECTED_SHA256.isEmpty()) return true // Skip if no hash provided
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            val bytes = file.readBytes()
            digest.update(bytes)
            val hash = digest.digest().joinToString("") { "%02x".format(it) }
            return hash.equals(EXPECTED_SHA256, ignoreCase = true)
        } catch (e: Exception) {
            Log.e("ModelDownloader", "Hash validation failed", e)
            return false
        }
    }

    fun getModelFile(): File? {
        val file = File(context.filesDir, MODEL_FILE_NAME)
        return if (file.exists()) file else null
    }
}

sealed class DownloadState {
    object Idle : DownloadState()
    object Downloading : DownloadState()
    data class Success(val file: File) : DownloadState()
    data class Error(val message: String) : DownloadState()
}
