package com.sdm.music

import android.content.Context
import android.media.MediaMetadataRetriever
import java.io.File

class MusicRepository(context: Context) {
    // Android obliga a guardar los archivos en una carpeta asignada a la app para no requerir permisos invasivos
    val baseFolder = File(context.getExternalFilesDir(null), "Base-Musica").apply { mkdirs() }
    val favsFolder = File(baseFolder, "Mi favs").apply { mkdirs() }

    fun getPlaylists(): Map<String, List<String>> {
        val map = mutableMapOf<String, List<String>>()
        baseFolder.listFiles()?.forEach { folder ->
            if (folder.isDirectory) {
                val songs = folder.listFiles { file -> (file.name.endsWith(".mp3", true) || file.name.endsWith(".m4a", true) || file.name.endsWith(".webm", true)) }?.map { it.absolutePath }
                map[folder.name] = songs ?: emptyList() // Mostrar la carpeta aunque tenga 0 canciones
            }
        }
        val rootSongs = baseFolder.listFiles { file -> (file.name.endsWith(".mp3", true) || file.name.endsWith(".m4a", true) || file.name.endsWith(".webm", true)) }?.map { it.absolutePath }
        if (!rootSongs.isNullOrEmpty()) {
            map["General"] = rootSongs
        }
        return map
    }

    fun getSongMetadata(path: String): Map<String, String> {
        val meta = mutableMapOf<String, String>()
        try {
            val mmr = MediaMetadataRetriever()
            mmr.setDataSource(path)
            meta["artist"] = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: "Unknown"
            meta["genre"] = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE) ?: "Unknown"
            mmr.release()
        } catch (e: Exception) {}
        return meta
    }
    
    fun toggleLike(path: String): Boolean {
        val file = File(path)
        val dest = File(favsFolder, file.name)
        return if (dest.exists()) {
            dest.delete()
            false
        } else {
            file.copyTo(dest, overwrite = true)
            true
        }
    }
}
