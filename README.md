# RellyFi 🎵

RellyFi es una aplicación de música para Android 100% nativa y offline, diseñada para descargar, gestionar y reproducir audio local sin depender de servidores externos. 

Originalmente concebida como un cliente que se conectaba a un servidor Python privado a través de Tailscale, **RellyFi evolucionó a su V1 Autónoma**: todo el procesamiento, extracción y reproducción ocurre directamente en el dispositivo del usuario.

## ✨ Características Principales

* **Descarga Autónoma:** Motor `yt-dlp` integrado en la app para extraer audio de YouTube directamente a la memoria del teléfono.
* **Escudos Anti-Bot:** Implementación de armadura de cliente (`player_client=android`) y actualización silenciosa de parches desde GitHub para evadir los bloqueos de YouTube.
* **Procesamiento de Audio:** Usa `FFmpeg` embebido para conversión limpia a formatos ligeros (`.m4a`, `.webm`).
* **Gestión Offline:** Sistema de carpetas y listas de reproducción locales.
* **Reproducción Robusta:** Manejo de cola inteligente (prevención de *Void Crashes* al eliminar pistas en reproducción).
* **UI Dinámica:** Interfaz responsiva con soporte para temas Claro/Oscuro y efectos visuales adaptativos.

## 🛠️ Tecnologías Utilizadas

* **Lenguaje:** Kotlin
* **Plataforma:** Android (SDK nativo)
* **Motores Internos:** `youtubedl-android` (yt-dlp) + FFmpeg
* **Gestor de Dependencias:** Gradle (Kotlin DSL)

## 🚀 Evolución del Proyecto (Tech Debt resuelto)

* **[Refactor]** Transición de arquitectura Cliente-Servidor (Python/Tailscale) a Standalone (Nativo).
* **[Fix]** Corrección del "Bug del Vacío" en el `MediaPlayer` al mutar las listas de reproducción en tiempo real.
* **[Feature]** Sistema de auto-actualización del motor de scraping para mantener la app viva ante los cambios de algoritmo de las plataformas de video.

---
*Desarrollado en Arch Linux.*
