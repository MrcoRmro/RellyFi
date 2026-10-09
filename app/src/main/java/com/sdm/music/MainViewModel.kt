package com.sdm.music

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val musicRepo = MusicRepository(application)
    private val ytRepo = YtDlpRepository(application)

    private val _playlists = MutableStateFlow<Map<String, List<String>>>(emptyMap())
    val playlists: StateFlow<Map<String, List<String>>> = _playlists

    // --- VARIABLES PARA MAINACTIVITY ---
    private val _fypRecommendations = MutableStateFlow<List<Map<String, String>>>(emptyList())
    val fypRecommendations: StateFlow<List<Map<String, String>>> = _fypRecommendations

    private val _isFypLoading = MutableStateFlow(false)
    val isFypLoading: StateFlow<Boolean> = _isFypLoading
    // -----------------------------------

    private val _downloadProgress = MutableStateFlow<Map<String, Float>>(emptyMap())
    val downloadProgress: StateFlow<Map<String, Float>> = _downloadProgress

    init {
        // Limpiador: Borra descargas interrumpidas al iniciar
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val baseDir = File(getApplication<Application>().getExternalFilesDir(null), "Base-Musica")
                if (baseDir.exists()) {
                    baseDir.walkTopDown().forEach { file ->
                        if (file.isFile && (file.extension == "part" || file.extension == "ytdl" || file.name.endsWith(".temp"))) {
                            file.delete()
                        }
                    }
                }
            } catch (e: Exception) { }
        }
        loadLocalMusic()
    }

    fun loadLocalMusic() {
        viewModelScope.launch(Dispatchers.IO) { _playlists.value = musicRepo.getPlaylists() }
    }

    fun toggleLike(path: String): Boolean {
        val isLiked = musicRepo.toggleLike(path)
        loadLocalMusic()
        return isLiked
    }

    fun isSongLiked(path: String): Boolean {
        val name = path.substringAfterLast("/")
        return _playlists.value["Mi favs"]?.any { it.endsWith(name) } == true
    }

    fun addSongToPlaylist(songPath: String, playlistName: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = File(songPath)
                if (file.exists()) {
                    val baseDir = file.parentFile?.parentFile ?: file.parentFile
                    val destDir = File(baseDir, playlistName)
                    if (!destDir.exists()) destDir.mkdirs()
                    
                    val destFile = File(destDir, file.name)
                    if (!destFile.exists()) {
                        file.copyTo(destFile)
                    }
                    loadLocalMusic()
                    withContext(Dispatchers.Main) { onResult(true) }
                } else {
                    withContext(Dispatchers.Main) {  }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {  }
            }
        }
    }

    suspend fun resolveLiveStreamUrl(videoId: String): String {
        val searchResults = ytRepo.searchYoutube(videoId, 1)
        if (searchResults.isNotEmpty()) {
            val realId = searchResults[0]["id"] ?: return ""
            return ytRepo.getStreamUrl(realId)
        }
        return ""
    }

    // --- MOTOR DE DESCARGA SIMPLE (1x1) ---
    private fun executeSingleDownload(url: String, playlistName: String) {
        try {
            val destFolder = File(getApplication<Application>().getExternalFilesDir(null), "Base-Musica/$playlistName")
            if (!destFolder.exists()) destFolder.mkdirs()

            val req = YoutubeDLRequest(url)
                req.addOption("--extractor-args", "youtube:player_client=android,web")
                req.addOption("--compat-options", "no-youtube-unavailable-videos")
            req.addOption("-x")
            req.addOption("--audio-format", "m4a")
            req.addOption("--concurrent-fragments", "4") 
            req.addOption("--embed-thumbnail")
            req.addOption("--embed-metadata")
            req.addOption("-o", File(destFolder, "%(title)s.%(ext)s").absolutePath)

            var isConverting = false
            var convertTick = 0f

            YoutubeDL.getInstance().execute(req, null) { progress, _, line ->
                val text = line?.lowercase() ?: ""
                if (progress >= 99f || text.contains("[extractaudio]")) {
                    isConverting = true
                }

                val p = if (progress < 0f) 0f else (progress / 100f)
                val trackProgress = when {
                    isConverting -> {
                        convertTick += 0.015f
                        0.75f + convertTick.coerceAtMost(0.24f)
                    }
                    else -> p * 0.75f
                }

                val currentMap = _downloadProgress.value.toMutableMap()
                currentMap[playlistName] = trackProgress
                _downloadProgress.value = currentMap
            }
        } catch (e: Exception) {
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                    android.widget.Toast.makeText(getApplication(), "🛑 MOTOR DICE: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                }
                
        }
    }

    fun downloadSingleTrack(url: String, playlistName: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Despertar la ola visual al 1%
                var map = _downloadProgress.value.toMutableMap()
                map[playlistName] = 0.01f
                _downloadProgress.value = map

                // 2. Ejecutar la descarga
                executeSingleDownload(url, playlistName)
                
                // 3. Limpiar la ola y actualizar carpetas
                map = _downloadProgress.value.toMutableMap()
                map.remove(playlistName)
                _downloadProgress.value = map
                loadLocalMusic()
                
                withContext(Dispatchers.Main) { onResult(true) }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {  }
            }
        }
    }
}
