package com.sdm.music

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object UpdateChecker {

    private const val GITHUB_USER = "MrcoRmro"
    private const val REPO_NAME = "RellyFi" // Ajustar si tu repo en GitHub tiene otro nombre
    private const val CHANNEL_ID = "rellyfi_updates"
    private const val PREFS_NAME = "rellyfi_updater"

    fun verificarActualizacion(activity: Activity) {
        solicitarPermisoNotificaciones(activity)

        Thread {
            try {
                val apiUrl = "https://api.github.com/repos/$GITHUB_USER/$REPO_NAME/releases/latest"
                val conn = (URL(apiUrl).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    setRequestProperty("Accept", "application/vnd.github+json")
                    setRequestProperty("User-Agent", "RellyFi-Android")
                    connectTimeout = 6000
                    readTimeout = 6000
                }

                if (conn.responseCode != 200) return@Thread

                val jsonText = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()

                val json = JSONObject(jsonText)
                val tagRemoto = json.optString("tag_name", "").trim()
                val notas = json.optString("body", "Hay una nueva versión disponible.")
                val htmlUrl = json.optString("html_url", "")

                // Buscar el primer archivo .apk adjunto en el release; si no hay, usar la página del release
                var downloadUrl = htmlUrl
                val assets = json.optJSONArray("assets")
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val name = asset.optString("name", "")
                        if (name.endsWith(".apk", ignoreCase = true)) {
                            downloadUrl = asset.optString("browser_download_url", htmlUrl)
                            break
                        }
                    }
                }

                val versionLocal = obtenerVersionInstalada(activity)
                if (tagRemoto.isEmpty() || !esVersionMayor(tagRemoto, versionLocal)) return@Thread

                val prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val yaNotificada = prefs.getString("ultima_version_notificada", "") == tagRemoto

                activity.runOnUiThread {
                    if (activity.isFinishing || activity.isDestroyed) return@runOnUiThread

                    if (!yaNotificada) {
                        mostrarNotificacionSistema(activity, tagRemoto, downloadUrl)
                        prefs.edit().putString("ultima_version_notificada", tagRemoto).apply()
                    }

                    mostrarDialogoActualizacion(activity, tagRemoto, notas, downloadUrl)
                }
            } catch (_: Exception) {
                // Sin conexión o sin releases publicados: ignorar en silencio
            }
        }.start()
    }

    private fun solicitarPermisoNotificaciones(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (activity.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                activity.requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1002)
            }
        }
    }

    private fun obtenerVersionInstalada(context: Context): String {
        return try {
            val pInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            pInfo.versionName ?: "0.0"
        } catch (_: Exception) {
            "0.0"
        }
    }

    private fun esVersionMayor(remota: String, local: String): Boolean {
        val r = remota.trim().removePrefix("v").removePrefix("V").substringBefore("-")
            .split(".").map { it.filter(Char::isDigit).toIntOrNull() ?: 0 }
        val l = local.trim().removePrefix("v").removePrefix("V").substringBefore("-")
            .split(".").map { it.filter(Char::isDigit).toIntOrNull() ?: 0 }

        val maxLen = maxOf(r.size, l.size)
        for (i in 0 until maxLen) {
            val rv = r.getOrElse(i) { 0 }
            val lv = l.getOrElse(i) { 0 }
            if (rv != lv) return rv > lv
        }
        return false
    }

    private fun mostrarNotificacionSistema(context: Context, version: String, url: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Actualizaciones de RellyFi",
                NotificationManager.IMPORTANCE_HIGH
            )
            manager.createNotificationChannel(channel)
        }

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        val pendingIntent = PendingIntent.getActivity(
            context,
            2001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(context, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(context)
        }

        val notification = builder
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("RellyFi $version disponible")
            .setContentText("Toca para descargar la nueva versión")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        manager.notify(2001, notification)
    }

    private fun mostrarDialogoActualizacion(activity: Activity, version: String, notas: String, url: String) {
        AlertDialog.Builder(activity)
            .setTitle("Nueva versión disponible ($version)")
            .setMessage(notas.take(600))
            .setPositiveButton("Descargar") { _, _ ->
                activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }
            .setNegativeButton("Más tarde", null)
            .show()
    }
}
