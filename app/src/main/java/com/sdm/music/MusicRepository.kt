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
                val songs = folder.listFiles { file ->
                    file.isFile && (
                        file.name.endsWith(".mp3", true) ||
                        file.name.endsWith(".m4a", true) ||
                        file.name.endsWith(".webm", true)
                    )
                }?.map { it.absolutePath }
                map[folder.name] = songs ?: emptyList() // Mostrar la carpeta aunque tenga 0 canciones
            }
        }
        val rootSongs = baseFolder.listFiles { file ->
            file.isFile && (
                file.name.endsWith(".mp3", true) ||
                file.name.endsWith(".m4a", true) ||
                file.name.endsWith(".webm", true)
            )
        }?.map { it.absolutePath }
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
        } catch (_: Exception) {}
        return meta
    }

    fun toggleLike(path: String): Boolean {
        if (!favsFolder.exists()) favsFolder.mkdirs()

        val file = File(path)
        val dest = File(favsFolder, file.name)

        if (dest.exists()) {
            dest.delete()
            return false
        }

        // Busca el archivo original o una copia válida con el mismo nombre en cualquier carpeta de Base-Musica
        val existingSource = when {
            file.exists() -> file
            else -> findMatchingAudioInBase(file)
        }

        if (existingSource == null || !existingSource.exists()) {
            return false
        }

        return try {
            if (existingSource.canonicalPath != dest.canonicalPath) {
                existingSource.copyTo(dest, overwrite = true)
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun findMatchingAudioInBase(target: File): File? {
        val targetName = target.name
        val targetBase = target.nameWithoutExtension
        val audioExts = setOf("mp3", "m4a", "webm")

        baseFolder.listFiles()?.forEach { entry ->
            if (entry.isDirectory) {
                entry.listFiles()?.firstOrNull { candidate ->
                    candidate.isFile && (
                        candidate.name.equals(targetName, ignoreCase = true) ||
                        (candidate.nameWithoutExtension.equals(targetBase, ignoreCase = true) &&
                            candidate.extension.lowercase() in audioExts)
                    )
                }?.let { return it }
            } else if (entry.isFile) {
                if (entry.name.equals(targetName, ignoreCase = true) ||
                    (entry.nameWithoutExtension.equals(targetBase, ignoreCase = true) &&
                        entry.extension.lowercase() in audioExts)
                ) {
                    return entry
                }
            }
        }
        return null
    }
}
