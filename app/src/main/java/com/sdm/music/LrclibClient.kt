package com.sdm.music

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

object LrclibClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val req = chain.request().newBuilder()
                .header("User-Agent", "RellyFi/1.0 (https://github.com/MrcoRmro/RellyFi)")
                .build()
            chain.proceed(req)
        }
        .build()

    suspend fun fetchAndSaveLyrics(
        mediaFile: File,
        rawTitle: String,
        rawArtist: String,
        durationSeconds: Int
    ): File? = withContext(Dispatchers.IO) {
        val (cleanArtist, cleanTrack) = sanitizeMetadata(rawTitle, rawArtist)
        if (cleanTrack.isBlank()) return@withContext null

        val lrcContent = queryExact(cleanTrack, cleanArtist, durationSeconds)
            ?: querySearch(cleanTrack, cleanArtist)
            ?: return@withContext null

        val lrcFile = File(mediaFile.parentFile, "${mediaFile.nameWithoutExtension}.lrc")
        lrcFile.writeText(lrcContent, Charsets.UTF_8)
        lrcFile
    }

    private fun queryExact(track: String, artist: String, durationSec: Int): String? {
        val url = buildString {
            append("https://lrclib.net/api/get?track_name=")
            append(encode(track))
            append("&artist_name=")
            append(encode(artist))
            if (durationSec > 0) {
                append("&duration=").append(durationSec)
            }
        }
        val request = Request.Builder().url(url).get().build()
        return runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                val body = response.body?.string() ?: return@use null
                val json = JSONObject(body)
                json.optString("syncedLyrics").takeIf { it.isNotBlank() }
                    ?: json.optString("plainLyrics").takeIf { it.isNotBlank() }
            }
        }.getOrNull()
    }

    private fun querySearch(track: String, artist: String): String? {
        val url = "https://lrclib.net/api/search?track_name=${encode(track)}&artist_name=${encode(artist)}"
        val request = Request.Builder().url(url).get().build()
        return runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                val body = response.body?.string() ?: return@use null
                val array = JSONArray(body)
                if (array.length() == 0) return@use null
                val first = array.getJSONObject(0)
                first.optString("syncedLyrics").takeIf { it.isNotBlank() }
                    ?: first.optString("plainLyrics").takeIf { it.isNotBlank() }
            }
        }.getOrNull()
    }

    private fun sanitizeMetadata(rawTitle: String, rawArtist: String): Pair<String, String> {
        val noiseRegex = Regex("""(\[.*?\]|\(.*?\)|official\s*video|official\s*audio|lyric\s*video|lyrics|hd|4k)""", RegexOption.IGNORE_CASE)
        val cleanChannel = rawArtist.replace(Regex("""\s*-\s*Topic$""", RegexOption.IGNORE_CASE), "").trim()

        val cleanedTitle = rawTitle.replace(noiseRegex, "").trim()
        return if (cleanedTitle.contains(" - ")) {
            val parts = cleanedTitle.split(" - ", limit = 2)
            Pair(parts[0].trim(), parts[1].trim())
        } else {
            Pair(cleanChannel, cleanedTitle)
        }
    }

    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")
}
