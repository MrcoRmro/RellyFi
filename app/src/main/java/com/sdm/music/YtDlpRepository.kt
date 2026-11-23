package com.sdm.music

import android.content.Context
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

class YtDlpRepository(private val context: Context) {

    private var isUpdating = false

    @OptIn(DelicateCoroutinesApi::class)
    private fun ensureInitialized() {
        try {
            val appCtx = context.applicationContext as android.app.Application
            YoutubeDL.getInstance().init(appCtx)
            FFmpeg.getInstance().init(appCtx)
            if (!isUpdating) {
                isUpdating = true
                GlobalScope.launch(Dispatchers.IO) {
                    try { YoutubeDL.getInstance().updateYoutubeDL(appCtx) } catch (e: Exception) {}
                }
            }
        } catch (e: Exception) {
            throw Exception("InitError: ${e.message}")
        }
    }

    suspend fun searchYoutube(query: String, maxResults: Int = 1): List<Map<String, String>> = withContext(Dispatchers.IO) {
        val results = mutableListOf<Map<String, String>>()
        try {
            ensureInitialized()
            // Búsqueda normal para cuando el usuario escribe en la barra de búsqueda
            val request = YoutubeDLRequest("ytsearch$maxResults:$query")
            request.addOption("--dump-json")
            request.addOption("--flat-playlist") // OPTIMIZACIÓN EXTREMA DE VELOCIDAD
            request.addOption("--no-warnings")
            request.addOption("--ignore-errors")

            val response = YoutubeDL.getInstance().execute(request, null, null)
            val lines = response.out.split("\n") 
            for (line in lines) {
                if (line.isNotBlank() && line.startsWith("{")) {
                    try {
                        val data = JSONObject(line)
                        val id = data.optString("id", "")
                        val title = data.optString("title", "Desconocido")
                        var thumb = data.optString("thumbnail", "")
                        if (data.has("thumbnails")) {
                            val thumbsArray = data.getJSONArray("thumbnails")
                            if (thumbsArray.length() > 0) thumb = thumbsArray.getJSONObject(thumbsArray.length() - 1).optString("url", thumb)
                        }
                        if (id.isNotEmpty()) results.add(mapOf("id" to id, "title" to title, "thumbnail" to thumb))
                    } catch (e: Exception) {}
                }
            }
        } catch (e: Exception) {}
        results
    }

    // Búsqueda rápida de recomendaciones usando ytsearch (flat-playlist + dump-json)
    suspend fun getSongRecommendations(query: String, limit: Int = 12): List<Map<String, String>> = withContext(Dispatchers.IO) {
        val results = mutableListOf<Map<String, String>>()
        try {
            ensureInitialized()
            val request = YoutubeDLRequest("ytsearch$limit:$query")
            request.addOption("--dump-json")
            request.addOption("--flat-playlist")
            request.addOption("--no-warnings")
            request.addOption("--ignore-errors")

            val response = YoutubeDL.getInstance().execute(request, null, null)
            val lines = response.out.split("\n") 
            for (line in lines) {
                if (line.isNotBlank() && line.startsWith("{")) {
                    try {
                        val data = JSONObject(line)
                        val id = data.optString("id", "")
                        val title = data.optString("title", "Desconocido")
                        var thumb = data.optString("thumbnail", "")
                        if (data.has("thumbnails")) {
                            val thumbsArray = data.getJSONArray("thumbnails")
                            if (thumbsArray.length() > 0) thumb = thumbsArray.getJSONObject(thumbsArray.length() - 1).optString("url", thumb)
                        }
                        if (id.isNotEmpty()) results.add(mapOf("id" to id, "title" to title, "thumbnail" to thumb))
                    } catch (e: Exception) {}
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("YtDlpRepository", "Error en getSongRecommendations: ${e.message}", e)
        }
        results
    }

    suspend fun getStrictMusicRecommendations(artist: String, limit: Int = 15): List<Map<String, String>> {
        return getSongRecommendations("$artist Mix", limit)
    }

    suspend fun getStreamUrl(videoId: String): String = withContext(Dispatchers.IO) {
        try {
            ensureInitialized()
            val request = YoutubeDLRequest("https://www.youtube.com/watch?v=$videoId")
            request.addOption("--dump-json")
            request.addOption("--no-warnings")
            request.addOption("--ignore-errors")
            request.addOption("-f", "bestaudio/best")
            
            val response = YoutubeDL.getInstance().execute(request, null, null)
            val data = JSONObject(response.out.split("\n")?.first() ?: "")
            data.optString("url", "")
        } catch (e: Exception) { "" }
    }
}
