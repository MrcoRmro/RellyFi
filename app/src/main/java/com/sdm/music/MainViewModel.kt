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
import com.sdm.music.FypCacheDao
import com.sdm.music.FypCacheEntity
import com.sdm.music.AppDatabase

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val musicRepo = MusicRepository(application)
    private val ytRepo = YtDlpRepository(application)
    private val fypCacheDao: FypCacheDao = AppDatabase.getInstance(application).fypCacheDao()
    private val downloadDao: DownloadDao = AppDatabase.getInstance(application).downloadDao()
    private var lastFypArtist: String = ""

    val pendingDownloadsCount = downloadDao.getPendingCountFlow()
    val downloadQueue = downloadDao.getAllFlow()

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

    private fun cleanArtistName(raw: String): String {
        return raw
            .replace(Regex("\\s*[-–_]\\s*(Topic|VEVO|Official|Oficial|Music|Records|Entertainment|Channel|TV|HD|4K|Audio|Video|Lyrics)(\\s|$)", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\(.*?\\)", RegexOption.DOT_MATCHES_ALL), "")
            .replace(Regex("\\[.*?\\]", RegexOption.DOT_MATCHES_ALL), "")
            .replace("_", " ")
            .replace(Regex("\\s{2,}"), " ")
            .trim()
    }

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
                // Si quedaron canciones pendientes en SQLite, reactivar el trabajador de WorkManager
                val pending = downloadDao.getNextPending()
                if (pending != null) {
                    PlaylistDownloadWorker.enqueueWork(getApplication())
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

    fun triggerFypForSong(songPath: String, songTitle: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Extraer artista y título de la canción que se reproduce actualmente
                val metadata = if (!songPath.startsWith("LIVE_STREAM|")) {
                    musicRepo.getSongMetadata(songPath)
                } else emptyMap()

                val metaArtist = metadata["artist"]?.takeIf { it.isNotBlank() && !it.equals("Unknown", ignoreCase = true) }
                val cleanTitle = cleanArtistName(songTitle.substringBeforeLast(".").replace("_", " "))

                val detectedArtist = metaArtist ?: when {
                    cleanTitle.contains(" - ") -> cleanTitle.substringBefore(" - ").trim()
                    cleanTitle.contains(" – ") -> cleanTitle.substringBefore(" – ").trim()
                    else -> ""
                }
                val cleanArtist = cleanArtistName(detectedArtist)

                val searchKey = when {
                    cleanArtist.isNotBlank() && cleanArtist.length >= 2 -> cleanArtist
                    cleanTitle.isNotBlank() && cleanTitle.length >= 2 -> cleanTitle
                    else -> "Música recomendada"
                }

                if (searchKey.equals(lastFypArtist, ignoreCase = true) && _fypRecommendations.value.isNotEmpty()) {
                    return@launch
                }

                // 2. Verificar caché en Room (válido por 24 horas)
                val oneDayAgo = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
                val freshCount = fypCacheDao.countFreshEntries(searchKey, oneDayAgo)

                if (freshCount > 0) {
                    val cached = fypCacheDao.getForArtist(searchKey)
                    val mapped = cached.map { mapOf("id" to it.videoId, "title" to it.title, "thumbnail" to it.thumbnail) }
                    withContext(Dispatchers.Main) {
                        lastFypArtist = searchKey
                        _fypRecommendations.value = mapped
                    }
                    return@launch
                }

                // 3. Caché miss: Buscar en segundo plano en YouTube con flat-playlist
                withContext(Dispatchers.Main) {
                    _isFypLoading.value = true
                    _fypRecommendations.value = emptyList()
                }

                val query = if (cleanArtist.isNotBlank()) {
                    "$cleanArtist canciones"
                } else {
                    "$cleanTitle mix"
                }

                val results = ytRepo.getSongRecommendations(query, 12)

                if (results.isNotEmpty()) {
                    lastFypArtist = searchKey
                    fypCacheDao.deleteForArtist(searchKey)
                    val entities = results.map { r ->
                        FypCacheEntity(
                            artistKey = searchKey,
                            videoId = r["id"] ?: "",
                            title = r["title"] ?: "",
                            thumbnail = r["thumbnail"] ?: ""
                        )
                    }
                    fypCacheDao.insertAll(entities)

                    withContext(Dispatchers.Main) { _fypRecommendations.value = results }
                } else {
                    // Fallback con mix amplio
                    val fallbackResults = ytRepo.getSongRecommendations("$searchKey mix", 10)
                    if (fallbackResults.isNotEmpty()) {
                        lastFypArtist = searchKey
                        withContext(Dispatchers.Main) { _fypRecommendations.value = fallbackResults }
                    }
                }

            } catch (e: Exception) {
                android.util.Log.e("RellyFi", "Error en triggerFypForSong: ${e.message}", e)
            } finally {
                withContext(Dispatchers.Main) { _isFypLoading.value = false }
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
                    android.widget.Toast.makeText(getApplication(), "MOTOR DICE: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
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

    // Registro previo de Playlist inteligente (cola en SQLite sin descargar)
    fun registerPlaylistForDownload(
        playlistUrl: String,
        folderName: String? = null,
        onStatus: ((String) -> Unit)? = null,
        onFinished: ((Int, String) -> Unit)? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) { onStatus?.invoke("Leyendo lista de canciones...") }

                // 1. Lectura rápida sin descargar audio
                val (playlistTitle, tracks) = ytRepo.extractPlaylistTracks(playlistUrl)

                if (tracks.isEmpty()) {
                    withContext(Dispatchers.Main) { onFinished?.invoke(0, "No se encontraron canciones en el enlace") }
                    return@launch
                }

                val finalFolderName = if (!folderName.isNullOrBlank()) folderName.trim() else playlistTitle.replace("/", "_").trim()

                // 2. Guardar en SQLite con estado PENDIENTE
                val entities = tracks.map { track ->
                    val vid = track["id"] ?: ""
                    DownloadItemEntity(
                        playlistName = finalFolderName,
                        playlistUrl = playlistUrl,
                        videoId = vid,
                        videoUrl = "https://www.youtube.com/watch?v=$vid",
                        title = track["title"] ?: "Canción",
                        thumbnail = track["thumbnail"] ?: "",
                        status = "PENDIENTE"
                    )
                }

                downloadDao.insertAll(entities)

                // 3. Delegar la descarga a WorkManager con Foreground Service
                PlaylistDownloadWorker.enqueueWork(getApplication())

                withContext(Dispatchers.Main) {
                    onFinished?.invoke(entities.size, finalFolderName)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onFinished?.invoke(0, "Error: ${e.message}")
                }
            }
        }
    }
}
