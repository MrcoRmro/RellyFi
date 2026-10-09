package com.sdm.music

import android.app.Application
import android.util.Log
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MiApp : Application() {
    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate() {
        super.onCreate()
        try {
            // Inicializamos la base de la librería
            YoutubeDL.getInstance().init(this)
            FFmpeg.getInstance().init(this)
            Log.d("MiApp", "Motores inicializados localmente")
            
            // Actualizador Automático: En 0.14.0 solo pasamos el contexto
            GlobalScope.launch(Dispatchers.IO) {
                try {
                    YoutubeDL.getInstance().updateYoutubeDL(this@MiApp)
                    Log.d("MiApp", "Binario de yt-dlp descargado y actualizado con éxito a la última versión")
                } catch (e: Exception) {
                    Log.e("MiApp", "No se pudo actualizar yt-dlp", e)
                }
            }
        } catch (e: Exception) {
            Log.e("MiApp", "Error crítico inicializando yt-dlp", e)
        }
    }
}
