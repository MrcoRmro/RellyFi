package com.sdm.music

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

class PlaylistDownloadWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val downloadDao = AppDatabase.getInstance(context).downloadDao()

    companion object {
        const val CHANNEL_ID = "RELLYFI_PLAYLIST_CHANNEL"
        const val NOTIFICATION_ID = 9992

        fun enqueueWork(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<PlaylistDownloadWorker>()
                .setConstraints(constraints)
                .addTag("PLAYLIST_DOWNLOAD_WORK")
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "PlaylistDownloadWork",
                ExistingWorkPolicy.KEEP,
                workRequest
            )
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        createNotificationChannel()

        // Asegurar que yt-dlp y ffmpeg están inicializados
        try {
            val appCtx = context.applicationContext as android.app.Application
            YoutubeDL.getInstance().init(appCtx)
            FFmpeg.getInstance().init(appCtx)
        } catch (e: Exception) { }

        // Recuperación ante fallos: Si se cerró la app o falló el proceso,
        // cualquier pista en 'DESCARGANDO' vuelve a 'PENDIENTE'
        downloadDao.resetDownloadingToPending()
        cleanIncompleteTempFiles()

        while (true) {
            if (isStopped) {
                // WorkManager detuvo el trabajo (por ejemplo, desconexión de red)
                cleanIncompleteTempFiles()
                downloadDao.resetDownloadingToPending()
                return@withContext Result.retry()
            }

            val nextItem = downloadDao.getNextPending() ?: break // Cola finalizada

            // Cambiar estado a DESCARGANDO
            downloadDao.updateStatus(nextItem.id, "DESCARGANDO")

            // Actualizar Foreground Service con notificación activa
            try {
                val fgInfo = createForegroundInfo("Descargando: ${nextItem.title}", "Carpeta: ${nextItem.playlistName}")
                setForeground(fgInfo)
            } catch (e: Exception) {
                android.util.Log.e("PlaylistDownloadWorker", "Error iniciando Foreground: ${e.message}")
            }

            // Ejecutar descarga con yt-dlp
            val success = downloadSingleTrackInternal(nextItem)

            if (success) {
                downloadDao.updateStatus(nextItem.id, "COMPLETADA")
                cleanIncompleteTempFiles(nextItem.playlistName)
            } else {
                if (!isNetworkConnected()) {
                    // Se interrumpió la conexión: limpiar temp y reintentar cuando vuelva la red
                    cleanIncompleteTempFiles(nextItem.playlistName)
                    downloadDao.updateStatus(nextItem.id, "PENDIENTE")
                    return@withContext Result.retry()
                } else {
                    // Error irrecuperable en esta pista (video bloqueado, borrado, etc.)
                    cleanIncompleteTempFiles(nextItem.playlistName)
                    downloadDao.updateStatus(nextItem.id, "FALLIDA")
                }
            }
        }

        showCompletedNotification()
        Result.success()
    }

    private fun downloadSingleTrackInternal(item: DownloadItemEntity): Boolean {
        return try {
            val destFolder = File(context.getExternalFilesDir(null), "Base-Musica/${item.playlistName}").apply { mkdirs() }
            val req = YoutubeDLRequest(item.videoUrl)
            req.addOption("--extractor-args", "youtube:player_client=android,web")
            req.addOption("--compat-options", "no-youtube-unavailable-videos")
            req.addOption("-x")
            req.addOption("--audio-format", "m4a")
            req.addOption("--concurrent-fragments", "4")
            req.addOption("--embed-thumbnail")
            req.addOption("--embed-metadata")
            req.addOption("-o", File(destFolder, "%(title)s.%(ext)s").absolutePath)

            var isError = false
            YoutubeDL.getInstance().execute(req, null) { _, _, line ->
                if (line?.contains("ERROR:", ignoreCase = true) == true) {
                    isError = true
                }
            }
            !isError
        } catch (e: Exception) {
            android.util.Log.e("PlaylistDownloadWorker", "Error descargando ${item.title}: ${e.message}", e)
            false
        }
    }

    private fun cleanIncompleteTempFiles(playlistName: String? = null) {
        try {
            val baseDir = File(context.getExternalFilesDir(null), "Base-Musica")
            val targetDir = if (playlistName != null) File(baseDir, playlistName) else baseDir
            if (targetDir.exists()) {
                targetDir.walkTopDown().forEach { file ->
                    if (file.isFile && (file.extension == "part" || file.extension == "ytdl" || file.name.endsWith(".temp"))) {
                        file.delete()
                    }
                }
            }
        } catch (e: Exception) { }
    }

    private fun isNetworkConnected(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun createForegroundInfo(title: String, subtitle: String): ForegroundInfo {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(subtitle)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    private fun showCompletedNotification() {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Descarga de Playlist finalizada")
            .setContentText("Todas las pistas pendientes han sido procesadas con éxito.")
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager.notify(NOTIFICATION_ID + 2, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Descargas de Playlists",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Progreso de descarga en segundo plano para playlists"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
}
