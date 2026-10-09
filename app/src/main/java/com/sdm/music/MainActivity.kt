package com.sdm.music

import android.Manifest
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.Icon
import android.media.AudioAttributes
import android.media.MediaMetadata
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.Collections
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

data class ThemeColors(val bgBase: Int, val w1c1: Int, val w1c2: Int, val w2c1: Int, val w2c2: Int, val w3c: Int, val textPrimary: Int, val textSecondary: Int, val accent: Int, val accentDark: Int, val accentLight: Int, val cardBg: Int, val navBg: Int, val isDark: Boolean)
val THEMES = mapOf(
    "Blanco con rojo" to ThemeColors(Color.parseColor("#FFF5F5"), Color.parseColor("#FF8A80"), Color.parseColor("#FF80AB"), Color.parseColor("#D50000"), Color.parseColor("#880E4F"), Color.parseColor("#FFCDD2"), Color.parseColor("#222222"), Color.parseColor("#B71C1C"), Color.parseColor("#D50000"), Color.parseColor("#B71C1C"), Color.parseColor("#FFCDD2"), Color.parseColor("#FFFFFF"), Color.parseColor("#E6FFFFFF"), false),
    "Negro con rojo" to ThemeColors(Color.parseColor("#121212"), Color.parseColor("#880E4F"), Color.parseColor("#4A0000"), Color.parseColor("#D50000"), Color.parseColor("#220000"), Color.parseColor("#3E0000"), Color.parseColor("#FFFFFF"), Color.parseColor("#FFCDD2"), Color.parseColor("#D50000"), Color.parseColor("#B71C1C"), Color.parseColor("#880E4F"), Color.parseColor("#2A2A2A"), Color.parseColor("#CC121212"), true),
    "Blanco con negro" to ThemeColors(Color.parseColor("#FBFDFB"), Color.parseColor("#E0E0E0"), Color.parseColor("#BDBDBD"), Color.parseColor("#757575"), Color.parseColor("#424242"), Color.parseColor("#EEEEEE"), Color.parseColor("#222222"), Color.parseColor("#616161"), Color.parseColor("#424242"), Color.parseColor("#212121"), Color.parseColor("#E0E0E0"), Color.parseColor("#FFFFFF"), Color.parseColor("#E6FFFFFF"), false),
    "Negro con morado" to ThemeColors(Color.parseColor("#121212"), Color.parseColor("#4A148C"), Color.parseColor("#311B92"), Color.parseColor("#7B1FA2"), Color.parseColor("#12005E"), Color.parseColor("#2A0054"), Color.parseColor("#FFFFFF"), Color.parseColor("#E1BEE7"), Color.parseColor("#AB47BC"), Color.parseColor("#8E24AA"), Color.parseColor("#4A148C"), Color.parseColor("#2A2A2A"), Color.parseColor("#CC121212"), true),
    "Blanco con morado" to ThemeColors(Color.parseColor("#F4F0FF"), Color.parseColor("#B39DDB"), Color.parseColor("#CE93D8"), Color.parseColor("#6A1B9A"), Color.parseColor("#4A148C"), Color.parseColor("#E1BEE7"), Color.parseColor("#222222"), Color.parseColor("#6A1B9A"), Color.parseColor("#8E24AA"), Color.parseColor("#4A148C"), Color.parseColor("#E1BEE7"), Color.parseColor("#FFFFFF"), Color.parseColor("#E6FFFFFF"), false),
    "Negro con verde" to ThemeColors(Color.parseColor("#121212"), Color.parseColor("#1B5E20"), Color.parseColor("#003300"), Color.parseColor("#2E7D32"), Color.parseColor("#001100"), Color.parseColor("#002200"), Color.parseColor("#FFFFFF"), Color.parseColor("#A5D6A7"), Color.parseColor("#4CAF50"), Color.parseColor("#388E3C"), Color.parseColor("#1B5E20"), Color.parseColor("#2A2A2A"), Color.parseColor("#CC121212"), true),
    "Blanco con verde" to ThemeColors(Color.parseColor("#F0FDF4"), Color.parseColor("#A5D6A7"), Color.parseColor("#81C784"), Color.parseColor("#2E7D32"), Color.parseColor("#1B5E20"), Color.parseColor("#C8E6C9"), Color.parseColor("#222222"), Color.parseColor("#2E7D32"), Color.parseColor("#388E3C"), Color.parseColor("#1B5E20"), Color.parseColor("#C8E6C9"), Color.parseColor("#FFFFFF"), Color.parseColor("#E6FFFFFF"), false)
)

class WaveMeshGradientView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG); private var time = 0f; var theme: ThemeColors = THEMES["Blanco con rojo"]!!
    private val handlerAnim = Handler(Looper.getMainLooper()); private val runnable = object : Runnable { override fun run() { time += 0.02f; invalidate(); handlerAnim.postDelayed(this, 30) } }
    init { handlerAnim.post(runnable) }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas); val w = width.toFloat(); val h = height.toFloat(); if (w == 0f || h == 0f) return
        canvas.drawColor(theme.bgBase)
        paint.shader = RadialGradient(w * 0.3f + sin(time) * 120f, h * 0.5f + sin(time * 0.8f) * 180f, w * 0.65f, intArrayOf(theme.w1c1, theme.w1c2, Color.TRANSPARENT), floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP); canvas.drawCircle(w * 0.3f + sin(time) * 120f, h * 0.5f + sin(time * 0.8f) * 180f, w * 0.65f, paint)
        paint.shader = RadialGradient(w * 0.8f + sin(time * 1.2f) * 100f, h * 0.25f + sin(time * 0.5f) * 150f, w * 0.6f, intArrayOf(theme.w2c1, theme.w2c2, Color.TRANSPARENT), floatArrayOf(0f, 0.45f, 1f), Shader.TileMode.CLAMP); canvas.drawCircle(w * 0.8f + sin(time * 1.2f) * 100f, h * 0.25f + sin(time * 0.5f) * 150f, w * 0.6f, paint)
        paint.shader = RadialGradient(w * 0.7f + sin(time * 0.7f) * 140f, h * 0.8f + sin(time * 1.1f) * 120f, w * 0.55f, intArrayOf(theme.w3c, Color.TRANSPARENT), floatArrayOf(0f, 1f), Shader.TileMode.CLAMP); canvas.drawCircle(w * 0.7f + sin(time * 0.7f) * 140f, h * 0.8f + sin(time * 1.1f) * 120f, w * 0.55f, paint)
    }
    override fun onDetachedFromWindow() { super.onDetachedFromWindow(); handlerAnim.removeCallbacks(runnable) }
}

class WaveFillView(context: Context) : View(context) {
    var progress: Float = 0f; set(value) { field = value; invalidate() }
    var themeColor: Int = Color.BLUE; set(value) { field = value; paint.color = value; paint.alpha = 90; invalidate() }
    private var phase = 0f; private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { alpha = 90 }; private val path = Path()
    private val handlerAnim = Handler(Looper.getMainLooper()); private val runnable = object : Runnable { override fun run() { if (progress > 0f) { phase += 0.15f; invalidate() }; handlerAnim.postDelayed(this, 30) } }
    init { handlerAnim.post(runnable) }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas); if (progress <= 0f) return; val w = width.toFloat(); val h = height.toFloat(); if (w == 0f || h == 0f) return
        val fillW = w * progress; path.reset(); path.moveTo(0f, h); var y = h; while (y >= 0f) { path.lineTo(min(fillW + sin((y * 0.08f) + phase) * 15f, w), y); y -= 4f }; path.lineTo(0f, 0f); path.close(); canvas.drawPath(path, paint)
    }
    override fun onDetachedFromWindow() { super.onDetachedFromWindow(); handlerAnim.removeCallbacks(runnable) }
}

class RadialLikeButton(context: Context) : View(context) {
    var isLiked = false; private set
    var themeColors: ThemeColors? = null; set(value) { field = value; invalidate() }
    private var touchX = 0f; private var touchY = 0f; private var rippleRadius = 0f; var onClickAction: (() -> Unit)? = null
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 5f }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; textSize = 33f; typeface = Typeface.DEFAULT_BOLD }
    fun setLikedState(liked: Boolean) { isLiked = liked; rippleRadius = if (liked) 9999f else 0f; invalidate() }
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> { touchX = event.x; touchY = event.y; return true }
            MotionEvent.ACTION_UP -> { 
                isLiked = !isLiked 
                val maxRadius = hypot(width.toDouble(), height.toDouble()).toFloat()
                val anim = ValueAnimator.ofFloat(if (isLiked) 0f else maxRadius, if (isLiked) maxRadius else 0f)
                anim.duration = 400; anim.addUpdateListener { rippleRadius = it.animatedValue as Float; invalidate() }; anim.start()
                onClickAction?.invoke(); return true 
            }
        }
        return super.onTouchEvent(event)
    }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas); val rect = RectF(0f, 0f, width.toFloat(), height.toFloat())
        themeColors?.let { strokePaint.color = it.accent; fillPaint.color = it.accent }
        canvas.drawRoundRect(rect, 40f, 40f, strokePaint)
        val maxR = hypot(width.toDouble(), height.toDouble()).toFloat()
        val drawRadius = if (rippleRadius >= 9999f) maxR else rippleRadius
        if (drawRadius > 0f) { 
            canvas.save(); val path = Path().apply { addRoundRect(rect, 40f, 40f, Path.Direction.CW) }
            canvas.clipPath(path)
            val cx = if (touchX == 0f) width / 2f else touchX; val cy = if (touchY == 0f) height / 2f else touchY
            canvas.drawCircle(cx, cy, drawRadius, fillPaint); canvas.restore() 
        }
        themeColors?.let { textPaint.color = if (drawRadius > width/3f) Color.WHITE else it.accentDark }
        val textY = (height / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f)
        canvas.drawText("LIKE", width / 2f, textY, textPaint)
    }
}

class VinylDiskView(context: Context) : View(context) {
    var bitmap: Bitmap? = null; set(value) { field = value; invalidate() }
    var isSpinning = false; var holeColor: Int = Color.WHITE; private var spinRotation = 0f
    private val handlerAnim = Handler(Looper.getMainLooper()); private val runnable = object : Runnable { override fun run() { if (isSpinning) { spinRotation = (spinRotation + 0.5f) % 360f; rotation = spinRotation }; handlerAnim.postDelayed(this, 16) } }
    init { handlerAnim.post(runnable) }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas); val cx = width / 2f; val cy = height / 2f; val radius = width.coerceAtMost(height) / 2f; canvas.drawCircle(cx, cy, radius, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#121212") })
        for (i in 1..6) canvas.drawCircle(cx, cy, radius - (i * (radius / 8f)), Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#2A2A2A"); style = Paint.Style.STROKE; strokeWidth = 2f })
        val artRadius = radius * 0.45f
        bitmap?.let { bmp -> val pathClip = Path().apply { addCircle(cx, cy, artRadius, Path.Direction.CW) }; canvas.save(); canvas.clipPath(pathClip); val scale = (artRadius * 2) / bmp.width.coerceAtMost(bmp.height).toFloat(); val matrixTransform = Matrix().apply { setScale(scale, scale); postTranslate(cx - (bmp.width * scale) / 2f, cy - (bmp.height * scale) / 2f) }; canvas.drawBitmap(bmp, matrixTransform, null); canvas.restore() } ?: run { canvas.drawCircle(cx, cy, artRadius, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.DKGRAY }) }
        canvas.drawCircle(cx, cy, radius * 0.05f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = holeColor })
    }
    override fun onDetachedFromWindow() { super.onDetachedFromWindow(); handlerAnim.removeCallbacks(runnable) }
}

class WavySeekBar(context: Context) : View(context) {
    private val wavePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 8f; strokeCap = Paint.Cap.ROUND }
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 8f }
    private val thumbPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    fun setColors(track: Int, wave: Int, thumb: Int) { trackPaint.color = track; wavePaint.color = wave; thumbPaint.color = thumb; invalidate() }
    var progress: Float = 0.0f; var isWaving: Boolean = false; var onProgressChanged: ((Float, Boolean) -> Unit)? = null
    private var phase = 0f; private var currentAmplitude = 0f; private val maxAmplitude = 8f
    private val handlerAnim = Handler(Looper.getMainLooper()); private val runnable = object : Runnable { override fun run() { currentAmplitude += ((if (isWaving) maxAmplitude else 0f) - currentAmplitude) * 0.15f; if (currentAmplitude > 0.1f || isWaving) phase += 0.15f; invalidate(); handlerAnim.postDelayed(this, 30) } }
    init { handlerAnim.post(runnable) }
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val w = width.toFloat(); if (w == 0f) return false
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> { parent?.requestDisallowInterceptTouchEvent(true); progress = (event.x / w).coerceIn(0f, 1f); onProgressChanged?.invoke(progress, true); invalidate(); return true }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> { parent?.requestDisallowInterceptTouchEvent(false); onProgressChanged?.invoke(progress, false); return true }
        }
        return super.onTouchEvent(event)
    }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas); val w = width.toFloat(); val h = height.toFloat(); val centerY = h / 2f; if (w == 0f) return
        val currentX = w * progress; canvas.drawLine(currentX, centerY, w, centerY, trackPaint); val path = Path(); path.moveTo(0f, centerY); var x = 0f
        while (x <= currentX) { path.lineTo(x, centerY + sin((x * 0.08f) + phase) * currentAmplitude); x += 4f }; canvas.drawPath(path, wavePaint); canvas.drawCircle(currentX, centerY + sin((currentX * 0.08f) + phase) * currentAmplitude, 14f, thumbPaint)
    }
    override fun onDetachedFromWindow() { super.onDetachedFromWindow(); handlerAnim.removeCallbacks(runnable) }
}

class QueueAdapter(var songs: MutableList<String>, private var currentIndexProvider: () -> Int, private var themeProvider: () -> ThemeColors, private val onItemClick: (Int) -> Unit, private val onStartDrag: (RecyclerView.ViewHolder) -> Unit, private val getGlassDrawable: (Boolean) -> GradientDrawable, private val loadCover: (String, ImageView) -> Unit) : RecyclerView.Adapter<QueueAdapter.QueueViewHolder>() {
    fun updateList(newList: MutableList<String>) { this.songs = newList; notifyDataSetChanged() }
    class QueueViewHolder(val layout: LinearLayout) : RecyclerView.ViewHolder(layout) { val imgCover: ImageView = layout.getChildAt(0) as ImageView; val txtSongName: TextView = layout.getChildAt(1) as TextView; val txtHandle: TextView = layout.getChildAt(2) as TextView }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QueueViewHolder { 
        val row = LinearLayout(parent.context).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(25, 25, 25, 25); layoutParams = RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 8, 0, 8) } }
        val img = ImageView(parent.context).apply { layoutParams = LinearLayout.LayoutParams(130, 130).apply { setMargins(0, 0, 30, 0) }; scaleType = ImageView.ScaleType.CENTER_CROP; clipToOutline = true; outlineProvider = object : ViewOutlineProvider() { override fun getOutline(view: View, outline: Outline) { outline.setRoundRect(0, 0, view.width, view.height, 20f) } } }
        val txtName = TextView(parent.context).apply { textSize = 15f; layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f) }
        val txtHandle = TextView(parent.context).apply { text = "≡"; textSize = 35f; typeface = Typeface.DEFAULT_BOLD; setPadding(20, 0, 20, 0) }
        row.addView(img); row.addView(txtName); row.addView(txtHandle)
        return QueueViewHolder(row) 
    }
    override fun onBindViewHolder(holder: QueueViewHolder, position: Int) { 
        val path = songs[position]; val theme = themeProvider(); val isCurrent = position == currentIndexProvider()
        holder.imgCover.setImageBitmap(null); holder.imgCover.setBackgroundColor(Color.parseColor(if(theme.isDark) "#2A2A2A" else "#E0E0E0")); loadCover(path, holder.imgCover)
        
        if (path.startsWith("LIVE_STREAM|")) {
            val parts = path.split("|")
            holder.txtSongName.text = parts.getOrNull(2)?.replace("_", " ") ?: "Live Audio"
        } else {
            holder.txtSongName.text = path.substringAfterLast("/").substringBeforeLast(".").replace("_", " ")
        }
        
        holder.txtSongName.setTextColor(theme.textPrimary); holder.txtSongName.typeface = if (isCurrent) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        holder.txtHandle.setTextColor(theme.accentDark)
        if (isCurrent) { holder.layout.background = GradientDrawable().apply { setColor(theme.accentLight); cornerRadius = 30f; setStroke(2, Color.parseColor(if (theme.isDark) "#33FFFFFF" else "#33000000")) } } else { holder.layout.background = getGlassDrawable(theme.isDark) }
        holder.layout.setOnClickListener { onItemClick(holder.adapterPosition) }; holder.txtHandle.setOnTouchListener { _, event -> if (event.actionMasked == MotionEvent.ACTION_DOWN) onStartDrag(holder); false } 
    }
    override fun getItemCount(): Int = songs.size
    fun onItemMove(fromPosition: Int, toPosition: Int) { if (fromPosition < toPosition) { for (i in fromPosition until toPosition) Collections.swap(songs, i, i + 1) } else { for (i in fromPosition downTo toPosition + 1) Collections.swap(songs, i, i - 1) }; notifyItemMoved(fromPosition, toPosition) }
}

class MainActivity : AppCompatActivity() {
    private lateinit var prefs: SharedPreferences
    private lateinit var currentTheme: ThemeColors
    private lateinit var viewModel: MainViewModel
    
    private lateinit var mainLayout: CoordinatorLayout
    private lateinit var waveBackgroundView: WaveMeshGradientView
    private lateinit var tabScrollView: HorizontalScrollView
    private lateinit var pagesContainer: LinearLayout
    
    private lateinit var sectionHome: ScrollView
    private lateinit var sectionPlaylist: LinearLayout
    private lateinit var sectionQueue: LinearLayout
    private lateinit var sectionSearch: LinearLayout
    private lateinit var sectionSettings: LinearLayout
    
    private lateinit var scrollFoldersView: ScrollView
    private lateinit var searchInput: EditText
    private lateinit var playlistFoldersContainer: LinearLayout
    private lateinit var playlistSongsContainer: LinearLayout
    private lateinit var songsListVerticalContainer: LinearLayout
    
    private lateinit var recyclerViewQueue: RecyclerView
    private lateinit var queueAdapter: QueueAdapter
    private lateinit var itemTouchHelper: ItemTouchHelper
    
    private lateinit var txtPlayingFrom: TextView
    private lateinit var imgVinyl: VinylDiskView
    private lateinit var txtTitle: TextView
    private lateinit var txtArtist: TextView
    private lateinit var wavySeekBar: WavySeekBar
    private lateinit var txtCurrentTime: TextView
    private lateinit var txtTotalTime: TextView
    private lateinit var btnPrev: ImageButton
    private lateinit var btnPlayPause: ImageButton
    private lateinit var btnNext: ImageButton
    private lateinit var btnShuffle: Button
    private lateinit var btnFavRadial: RadialLikeButton
    private lateinit var btnLoop: Button
    private lateinit var dotLoop: View
    
    private lateinit var txtFypHeader: TextView
    private lateinit var fypContainer: LinearLayout
    
    private lateinit var txtFoldersHeader: TextView
    private lateinit var txtFolderTitle: TextView
    private lateinit var btnBackFolder: Button
    private lateinit var btnSelectMode: Button
    private lateinit var btnDeleteSelected: ImageButton
    private lateinit var btnCrearPlaylist: Button
    private lateinit var txtQueueHeader: TextView
    private lateinit var txtSearchHeader: TextView
    private lateinit var inputYtSearch: EditText
    private lateinit var btnDoSearch: Button
    private lateinit var resultContainer: LinearLayout
            private lateinit var txtSettingsHeader: TextView
    private lateinit var switchCrossfade: Switch
    private lateinit var btnSleepTimer: Button
    private lateinit var btnCambiarTema: Button
    
    private lateinit var bottomNavGlass: LinearLayout
    private lateinit var btnHomeNav: Button
    private lateinit var btnPlaylistsNav: Button
    private lateinit var btnListNav: Button
    private lateinit var btnSearchNav: Button
    private lateinit var btnSettingsNav: Button

    private var mediaPlayer: MediaPlayer? = null
    private var isCrossfadeEnabled: Boolean = false
    private var mapPlaylists: MutableMap<String, List<String>> = mutableMapOf()
    private var playlistActual: MutableList<String> = mutableListOf()
    private var nombrePlaylistActual: String = "General"
    private var currentIndex: Int = 0
    private var isLooping: Boolean = false
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var notificationManager: NotificationManager
    private lateinit var mediaSession: MediaSession

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            windowManager.defaultDisplay.supportedModes.maxByOrNull { it.refreshRate }?.let { maxMode ->
                val layoutParams = window.attributes
                layoutParams.preferredDisplayModeId = maxMode.modeId
                window.attributes = layoutParams
            }
        }
        
        // Cargar ViewModel usando el Provider (Arquitectura limpia)
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1) }
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { notificationManager.createNotificationChannel(NotificationChannel("SDM_CHANNEL", "Reproductor SDM", NotificationManager.IMPORTANCE_LOW)) }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { window.setDecorFitsSystemWindows(false) } 
        else { window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION }
        window.statusBarColor = Color.TRANSPARENT; window.navigationBarColor = Color.TRANSPARENT

        mediaSession = MediaSession(this, "SDM_SESSION")
        mediaSession.setCallback(object : MediaSession.Callback() {
            override fun onPlay() { togglePlayPause() }
            override fun onPause() { togglePlayPause() }
            override fun onSkipToNext() { reproducirSiguiente() }
            override fun onSkipToPrevious() { reproducirAnterior() }
            override fun onSeekTo(pos: Long) { try { mediaPlayer?.seekTo(pos.toInt()); actualizarNotificacion() } catch(e: Exception) {} }
        })
        mediaSession.isActive = true
        prefs = getSharedPreferences("SDM_SESSION", Context.MODE_PRIVATE)
        isCrossfadeEnabled = prefs.getBoolean("CROSSFADE", false)
        val savedThemeName = prefs.getString("APP_THEME", "Blanco con rojo") ?: "Blanco con rojo"
        currentTheme = THEMES[savedThemeName] ?: THEMES["Blanco con rojo"]!!
        
        mainLayout = CoordinatorLayout(this)
        waveBackgroundView = WaveMeshGradientView(this).apply { theme = currentTheme }
        mainLayout.addView(waveBackgroundView, CoordinatorLayout.LayoutParams(CoordinatorLayout.LayoutParams.MATCH_PARENT, CoordinatorLayout.LayoutParams.MATCH_PARENT))
        
        crearPantallaReproductor()
        updateUIColors(currentTheme)
        setContentView(mainLayout)

        // Observadores del ViewModel (Reemplazan la lógica de Sincronización)
        lifecycleScope.launch {
            viewModel.playlists.collect { playlists ->
                mapPlaylists = playlists.toMutableMap()
                val fRecien = java.io.File(getExternalFilesDir(null), "Base-Musica/Recien agregados")
        var tempMap = mapPlaylists.toMutableMap()
        if (fRecien.exists() && !tempMap.containsKey("Recien agregados")) {
            tempMap["Recien agregados"] = emptyList()
        }
        viewModel.downloadProgress.value.keys.forEach {
            if (!tempMap.containsKey(it)) {
                tempMap[it] = emptyList()
            }
        }
        mapPlaylists = tempMap
        renderizarListaDeCarpetas()
                if (playlistActual.isEmpty() && mapPlaylists.isNotEmpty()) {
                    val primera = mapPlaylists.keys.first()
                    nombrePlaylistActual = primera
                    playlistActual = mapPlaylists[primera]?.toMutableList() ?: mutableListOf()
                    if(::queueAdapter.isInitialized) queueAdapter.updateList(playlistActual)
                }
            }
        }

        lifecycleScope.launch {
            viewModel.fypRecommendations.collect { recs ->
                fypContainer.removeAllViews()
                if (recs.isEmpty()) return@collect
                
                for (item in recs) {
                    val vid = item["id"] ?: ""
                    val vtitle = item["title"] ?: ""
                    val vthumb = item["thumbnail"] ?: ""
                    
                    val row = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(25, 25, 25, 25); background = getGlassDrawable(currentTheme.isDark); layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 8, 0, 8) } }
                    val imgC = ImageView(this@MainActivity).apply { layoutParams = LinearLayout.LayoutParams(130, 130).apply { setMargins(0, 0, 30, 0) }; scaleType = ImageView.ScaleType.CENTER_CROP; clipToOutline = true; outlineProvider = object : ViewOutlineProvider() { override fun getOutline(view: View, outline: Outline) { outline.setRoundRect(0, 0, view.width, view.height, 20f) } }; setBackgroundColor(Color.parseColor(if(currentTheme.isDark) "#2A2A2A" else "#E0E0E0")) }
                    loadExternalImageAsync(vthumb, imgC)
                    val txtN = TextView(this@MainActivity).apply { text = vtitle; textSize = 15f; setTextColor(currentTheme.textPrimary); layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f) }
                    row.addView(imgC); row.addView(txtN)
                    row.setOnClickListener {
                        // 1. DETENER AUDIO INMEDIATAMENTE
                        try { 
                            // Intentamos pausar enviando el broadcast que lee tu MediaSession
                            sendBroadcast(android.content.Intent("ACTION_PAUSE"))
                        } catch(e: Exception) {}

                        // 2. REINICIO VISUAL INSTANTÁNEO
                        val safeTitle = vtitle.replace("|", "")
                        txtTitle.text = safeTitle
                        txtArtist.text = "Cargando audio..."
                        txtCurrentTime.text = "00:00"
                        wavySeekBar.progress = 0f
                        
                        // Cargar la carátula temporal vacía o la de baja resolución
                        // Vaciar la carátula anterior al instante
                        try { (imgVinyl as? android.widget.ImageView)?.setImageDrawable(null) } catch(e: Exception) {}
                        
                        // Cargar la nueva carátula de forma nativa en segundo plano
                        if (vthumb.isNotEmpty()) {
                            lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                try {
                                    val stream = java.net.URL(vthumb).openStream()
                                    val bmp = android.graphics.BitmapFactory.decodeStream(stream)
                                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                        try { (imgVinyl as? android.widget.ImageView)?.setImageBitmap(bmp) } catch(e: Exception) {}
                                    }
                                } catch(e: Exception) {}
                            }
                        }

                        Toast.makeText(this@MainActivity, "Conectando stream...", Toast.LENGTH_SHORT).show()

                        lifecycleScope.launch {
                            // 3. OBTENER AUDIO EN SEGUNDO PLANO
                            val rUrl = viewModel.resolveLiveStreamUrl(vid)

                            if (rUrl.isNotEmpty()) {
                                val safeTitle = vtitle.replace("|", "")
                                val queueItem = "LIVE_STREAM|${rUrl}|$safeTitle|$vthumb|https://www.youtube.com/watch?v=$vid"
                                resetLoopState(); nombrePlaylistActual = "Radio Inteligente"; playlistActual = mutableListOf(queueItem); currentIndex = 0
                                queueAdapter.updateList(playlistActual)
                                reproducirCancionActual(); sectionHome.smoothScrollTo(0, 0)
                            } else {
                                Toast.makeText(this@MainActivity, "Error conectando", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    fypContainer.addView(row)
                }
            }
        }

        lifecycleScope.launch {
            
        lifecycleScope.launch {
            viewModel.downloadProgress.collect { progressMap ->
                for (i in 0 until playlistFoldersContainer.childCount) {
                    val child = playlistFoldersContainer.getChildAt(i)
                    val folderName = child.tag as? String
                    if (folderName != null) {
                        val waveView = findWaveFillView(child)
                        val rawP = progressMap[folderName] ?: 0f
                        val isConv = rawP >= 2f
                        waveView?.progress = if (isConv) (rawP - 2f) else rawP
                        waveView?.alpha = if (isConv) 1.0f else 0.4f
                    }
                }
            }
        }

        viewModel.isFypLoading.collect { isLoading ->
                if (isLoading) {
                    fypContainer.removeAllViews()
                    fypContainer.addView(TextView(this@MainActivity).apply { text = "Generando Inteligencia Artificial..."; gravity = Gravity.CENTER; setPadding(0, 40, 0, 40); setTextColor(currentTheme.textSecondary) })
                }
            }
        }
    }

    private fun getGlassDrawable(isDark: Boolean): GradientDrawable { val glassColor = if (isDark) Color.parseColor("#4D000000") else Color.parseColor("#66FFFFFF"); val strokeColor = if (isDark) Color.parseColor("#33FFFFFF") else Color.parseColor("#33000000"); return GradientDrawable().apply { setColor(glassColor); cornerRadius = 30f; setStroke(2, strokeColor) } }

    private fun animateThemeTransition(newTheme: ThemeColors) {
        val oldTheme = currentTheme; val animator = ValueAnimator.ofFloat(0f, 1f); animator.duration = 600; val eval = ArgbEvaluator()
        animator.addUpdateListener { anim ->
            val f = anim.animatedFraction
            val tempTheme = ThemeColors(
                eval.evaluate(f, oldTheme.bgBase, newTheme.bgBase) as Int, eval.evaluate(f, oldTheme.w1c1, newTheme.w1c1) as Int, eval.evaluate(f, oldTheme.w1c2, newTheme.w1c2) as Int, eval.evaluate(f, oldTheme.w2c1, newTheme.w2c1) as Int, eval.evaluate(f, oldTheme.w2c2, newTheme.w2c2) as Int, eval.evaluate(f, oldTheme.w3c, newTheme.w3c) as Int,
                eval.evaluate(f, oldTheme.textPrimary, newTheme.textPrimary) as Int, eval.evaluate(f, oldTheme.textSecondary, newTheme.textSecondary) as Int, eval.evaluate(f, oldTheme.accent, newTheme.accent) as Int, eval.evaluate(f, oldTheme.accentDark, newTheme.accentDark) as Int, eval.evaluate(f, oldTheme.accentLight, newTheme.accentLight) as Int,
                eval.evaluate(f, oldTheme.cardBg, newTheme.cardBg) as Int, eval.evaluate(f, oldTheme.navBg, newTheme.navBg) as Int, newTheme.isDark
            )
            updateUIColors(tempTheme)
        }
        animator.start(); currentTheme = newTheme
    }

    private fun updateUIColors(t: ThemeColors) {
        waveBackgroundView.theme = t
        txtPlayingFrom.setTextColor(t.accentDark); imgVinyl.holeColor = t.bgBase; txtTitle.setTextColor(t.textPrimary); txtArtist.setTextColor(t.accentDark)
        wavySeekBar.setColors(t.accentLight, t.accent, t.accentDark); txtCurrentTime.setTextColor(t.accentDark); txtTotalTime.setTextColor(t.accentDark)
        btnPrev.background = GradientDrawable().apply { setColor(t.accentLight); cornerRadius = 80f }; btnPrev.setColorFilter(t.accentDark)
        btnPlayPause.background = GradientDrawable().apply { setColor(t.accent); cornerRadius = 50f }; btnPlayPause.setColorFilter(Color.WHITE)
        btnNext.background = GradientDrawable().apply { setColor(t.accentLight); cornerRadius = 80f }; btnNext.setColorFilter(t.accentDark)
        btnShuffle.setTextColor(t.accentDark); btnLoop.setTextColor(t.accentDark)
        (dotLoop.background as GradientDrawable).setColor(if (isLooping) t.accent else Color.TRANSPARENT); dotLoop.invalidate()
        btnFavRadial.themeColors = t
        
        txtFypHeader.setTextColor(t.textPrimary)
        txtFypHeader.background = getGlassDrawable(t.isDark)
        for(i in 0 until fypContainer.childCount){ val row = fypContainer.getChildAt(i) as? LinearLayout ?: continue; row.background = getGlassDrawable(t.isDark); (row.getChildAt(1) as? TextView)?.setTextColor(t.textPrimary) }
        
        searchInput.setTextColor(t.textPrimary); searchInput.setHintTextColor(t.textSecondary); searchInput.background = getGlassDrawable(t.isDark)
        txtFoldersHeader.setTextColor(t.textPrimary); txtFolderTitle.setTextColor(t.textPrimary)
        btnBackFolder.setTextColor(t.accent); btnSelectMode.setTextColor(t.accent); btnDeleteSelected.setColorFilter(t.accentDark)
        txtQueueHeader.setTextColor(t.textPrimary)
        txtSearchHeader.setTextColor(t.textPrimary)
        inputYtSearch.setTextColor(t.textPrimary); inputYtSearch.setHintTextColor(t.textSecondary); inputYtSearch.background = getGlassDrawable(t.isDark)
        btnDoSearch.setTextColor(t.accent); resultContainer.background = getGlassDrawable(t.isDark)
        
        
        txtSettingsHeader.setTextColor(t.textPrimary)
        switchCrossfade.setTextColor(t.textPrimary)
        btnSleepTimer.background = getGlassDrawable(t.isDark); btnSleepTimer.setTextColor(t.accentDark)
        btnCrearPlaylist.background = getGlassDrawable(t.isDark); btnCrearPlaylist.setTextColor(t.accentDark)
        btnCambiarTema.background = getGlassDrawable(t.isDark); btnCambiarTema.setTextColor(t.accentDark)
        bottomNavGlass.background = GradientDrawable().apply { setColor(t.navBg); cornerRadius = 80f; setStroke(2, Color.parseColor(if(t.isDark) "#44FFFFFF" else "#44000000")) }
        btnHomeNav.setTextColor(t.accentDark); btnPlaylistsNav.setTextColor(t.accentDark); btnListNav.setTextColor(t.accentDark); btnSearchNav.setTextColor(t.accentDark); btnSettingsNav.setTextColor(t.accentDark)
        
        if (::queueAdapter.isInitialized) queueAdapter.notifyDataSetChanged()
        for (i in 0 until playlistFoldersContainer.childCount) { val child = playlistFoldersContainer.getChildAt(i); if (child is FrameLayout && child.tag != null) { child.background = getGlassDrawable(t.isDark); (child.getChildAt(0) as? WaveFillView)?.themeColor = t.accent; val row = child.getChildAt(1) as LinearLayout; (row.getChildAt(0) as TextView).setTextColor(t.textPrimary); (row.getChildAt(1) as ImageButton).setColorFilter(t.accentDark) } }
        for (i in 0 until songsListVerticalContainer.childCount) { val row = songsListVerticalContainer.getChildAt(i) as? LinearLayout ?: continue; row.background = getGlassDrawable(t.isDark); (row.getChildAt(1) as? TextView)?.setTextColor(t.textPrimary) }
    }

    private fun loadExternalImageAsync(urlString: String, imageView: ImageView) {
        if(urlString.isEmpty()) return
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val conn = URL(urlString).openConnection() as HttpURLConnection
                conn.connectTimeout = 5000; conn.doInput = true; conn.connect()
                if (conn.responseCode == 200) { 
                    val bitmap = BitmapFactory.decodeStream(conn.inputStream)
                    withContext(Dispatchers.Main) { imageView.setImageBitmap(bitmap) }
                }
                conn.disconnect()
            } catch (e: Exception) {}
        }
    }

    private fun loadCoverListAsync(rutaArchivo: String, imageView: ImageView) {
        if(rutaArchivo.startsWith("LIVE_STREAM|")){
            loadExternalImageAsync(rutaArchivo.split("|").getOrNull(3) ?: "", imageView)
            return
        }
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                var bitmap: Bitmap? = null
                val mmr = MediaMetadataRetriever()
                mmr.setDataSource(rutaArchivo)
                val pic = mmr.embeddedPicture
                if (pic != null) { bitmap = BitmapFactory.decodeByteArray(pic, 0, pic.size) } 
                else {
                    val baseName = File(rutaArchivo).nameWithoutExtension
                    val dir = File(rutaArchivo).parentFile
                    val cover = dir?.listFiles()?.firstOrNull { it.name.startsWith(baseName) && (it.name.endsWith(".jpg") || it.name.endsWith(".png")) }
                    bitmap = if (cover != null) BitmapFactory.decodeFile(cover.absolutePath) else null
                }
                mmr.release()
                if (bitmap != null) withContext(Dispatchers.Main) { imageView.setImageBitmap(bitmap) }
            } catch (e: Exception) {}
        }
    }

    private fun crearPantallaReproductor() {
        val contentFrame = FrameLayout(this).apply { layoutParams = CoordinatorLayout.LayoutParams(CoordinatorLayout.LayoutParams.MATCH_PARENT, CoordinatorLayout.LayoutParams.MATCH_PARENT) }
        tabScrollView = object : HorizontalScrollView(this) { override fun onTouchEvent(ev: MotionEvent?): Boolean = false; override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean = false }.apply { isSmoothScrollingEnabled = true; layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT).apply { setMargins(0,0,0,220) }; overScrollMode = View.OVER_SCROLL_NEVER
            isVerticalScrollBarEnabled = false; isHorizontalScrollBarEnabled = false }
        pagesContainer = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val screenWidth = resources.displayMetrics.widthPixels; val lpPage = LinearLayout.LayoutParams(screenWidth, LinearLayout.LayoutParams.MATCH_PARENT)

        // --- HOME (FYP INCORPORADO) ---
        sectionHome = ScrollView(this).apply { layoutParams = lpPage; isFillViewport = true; overScrollMode = View.OVER_SCROLL_NEVER
            isVerticalScrollBarEnabled = false; isVerticalScrollBarEnabled = false }
        val homeContent = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL; setPadding(40, 140, 40, 40) }
        
        txtPlayingFrom = TextView(this).apply { text = "REPRODUCIENDO"; textSize = 14f; typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.CENTER; setPadding(0, 0, 0, 20) }
        homeContent.addView(txtPlayingFrom)
        imgVinyl = VinylDiskView(this).apply { layoutParams = LinearLayout.LayoutParams(600, 600).apply { setMargins(0, 0, 0, 20) } }
        homeContent.addView(imgVinyl)
        txtTitle = TextView(this).apply { text = "Selecciona una pista"; textSize = 20f; typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.START; layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT) }
        txtArtist = TextView(this).apply { text = "Lista de Reproduccion"; textSize = 14f; gravity = Gravity.START; setPadding(0, 5, 0, 20); layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT) }
        wavySeekBar = WavySeekBar(this).apply { layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 50).apply { setMargins(0, 5, 0, 5) } }
        wavySeekBar.onProgressChanged = { prog, fromUser -> if (fromUser) { try { mediaPlayer?.let { it.seekTo((prog * it.duration).toInt()); actualizarNotificacion() } } catch (e: Exception) {} } }
        val timeLayout = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT); setPadding(0, 0, 0, 10) }
        txtCurrentTime = TextView(this).apply { text = "00:00"; textSize = 11f }; txtTotalTime = TextView(this).apply { text = "00:00"; textSize = 11f; gravity = Gravity.END; layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f) }
        timeLayout.addView(txtCurrentTime); timeLayout.addView(txtTotalTime)

        val controlsRow1 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER; setPadding(0, 0, 0, 10) }
        btnPrev = ImageButton(this).apply { setImageResource(android.R.drawable.ic_media_previous); layoutParams = LinearLayout.LayoutParams(130, 130).apply { setMargins(15, 0, 15, 0) }; setOnClickListener { reproducirAnterior() } }
        btnPlayPause = ImageButton(this).apply { setImageResource(android.R.drawable.ic_media_play); layoutParams = LinearLayout.LayoutParams(160, 150).apply { setMargins(15, 0, 15, 0) }; setOnClickListener { togglePlayPause() } }
        btnNext = ImageButton(this).apply { setImageResource(android.R.drawable.ic_media_next); layoutParams = LinearLayout.LayoutParams(130, 130).apply { setMargins(15, 0, 15, 0) }; setOnClickListener { reproducirSiguiente() } }
        controlsRow1.addView(btnPrev); controlsRow1.addView(btnPlayPause); controlsRow1.addView(btnNext)

        val controlsRow2 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER; setPadding(0, 5, 0, 10) }
        btnShuffle = Button(this).apply { text = "MEZCLAR"; textSize = 11f; setBackgroundColor(Color.TRANSPARENT); layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f); setOnClickListener { if (playlistActual.isNotEmpty()) { resetLoopState(); playlistActual.shuffle(); currentIndex = 0; reproducirCancionActual(); val pulse = ValueAnimator.ofArgb(currentTheme.accentLight, Color.TRANSPARENT); pulse.duration = 400; pulse.addUpdateListener { btnShuffle.setBackgroundColor(it.animatedValue as Int) }; pulse.start(); if(::queueAdapter.isInitialized) queueAdapter.notifyDataSetChanged() } } }
        btnFavRadial = RadialLikeButton(this).apply { layoutParams = LinearLayout.LayoutParams(0, 120, 1f).apply { setMargins(10,0,10,0) }; onClickAction = { toggleFavCurrentSong() } }
        val wrapLoop = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER; layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f) }
        btnLoop = Button(this).apply { text = "REPETIR"; textSize = 11f; setBackgroundColor(Color.TRANSPARENT); setPadding(0,0,0,0) }
        dotLoop = View(this).apply { layoutParams = LinearLayout.LayoutParams(15, 15).apply { setMargins(5,0,0,0) }; background = GradientDrawable().apply { cornerRadius=15f; setColor(Color.TRANSPARENT) } }
        wrapLoop.addView(btnLoop); wrapLoop.addView(dotLoop)
        btnLoop.setOnClickListener { isLooping = !isLooping; mediaPlayer?.isLooping = isLooping; (dotLoop.background as GradientDrawable).setColor(if (isLooping) currentTheme.accent else Color.TRANSPARENT); dotLoop.invalidate() }

        controlsRow2.addView(btnShuffle); controlsRow2.addView(btnFavRadial); controlsRow2.addView(wrapLoop)
        
        homeContent.addView(txtTitle); homeContent.addView(txtArtist); homeContent.addView(wavySeekBar); homeContent.addView(timeLayout); homeContent.addView(controlsRow1); homeContent.addView(controlsRow2)
        
        val divider = View(this).apply { layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 2).apply { setMargins(0, 80, 0, 40) }; setBackgroundColor(Color.parseColor("#33888888")) }
        txtFypHeader = android.widget.Button(this).apply { 
        text = "Agregar a Playlist"
        isAllCaps = false
        setOnClickListener { 
            if (playlistActual.isNotEmpty() && currentIndex >= 0 && currentIndex < playlistActual.size) {
                showAddToPlaylistDialog(playlistActual[currentIndex])
            } else {
                android.widget.Toast.makeText(this@MainActivity, "Reproduce una canción primero", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
        textSize = 15f
        typeface = Typeface.DEFAULT_BOLD
        setPadding(0, 35, 0, 35)
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 40, 0, 0) }
    }
        fypContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT) }
        homeContent.addView(divider); homeContent.addView(txtFypHeader); homeContent.addView(fypContainer)
        sectionHome.addView(homeContent)
        
        // --- PLAYLIST ---
        sectionPlaylist = LinearLayout(this).apply { layoutParams = lpPage; orientation = LinearLayout.VERTICAL; setPadding(40, 140, 40, 20) }
        searchInput = EditText(this).apply {
            hint = "Buscar canción..."
            setSingleLine(); setPadding(40, 40, 40, 40)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, 20) }
            addTextChangedListener(object: TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    val query = s.toString().lowercase()
                    if (query.isEmpty()) { scrollFoldersView.visibility = View.VISIBLE; playlistSongsContainer.visibility = View.GONE } else {
                        scrollFoldersView.visibility = View.GONE; playlistSongsContainer.visibility = View.VISIBLE; songsListVerticalContainer.removeAllViews(); txtFolderTitle.text = "Resultados"; btnSelectMode.visibility = View.GONE; btnDeleteSelected.visibility = View.GONE
                        val results = mutableListOf<String>()
                        for ((folder, songs) in mapPlaylists) { for (song in songs) { if (song.lowercase().contains(query)) results.add(song) } }
                        results.forEachIndexed { idx, songPath ->
                            val songRow = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(25, 25, 25, 25); background = getGlassDrawable(currentTheme.isDark); layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 8, 0, 8) }; setOnClickListener { resetLoopState(); nombrePlaylistActual = "Búsqueda"; playlistActual = results; currentIndex = idx; if(::queueAdapter.isInitialized) queueAdapter.updateList(playlistActual); reproducirCancionActual(); cambiarPestana(0) } }
                            val imgCover = ImageView(this@MainActivity).apply { layoutParams = LinearLayout.LayoutParams(130, 130).apply { setMargins(0, 0, 30, 0) }; scaleType = ImageView.ScaleType.CENTER_CROP; clipToOutline = true; outlineProvider = object : ViewOutlineProvider() { override fun getOutline(view: View, outline: Outline) { outline.setRoundRect(0, 0, view.width, view.height, 20f) } }; setBackgroundColor(Color.parseColor(if(currentTheme.isDark) "#2A2A2A" else "#E0E0E0")) }
                            loadCoverListAsync(songPath, imgCover)
                            val txtName = TextView(this@MainActivity).apply { text = songPath.substringAfterLast("/").substringBeforeLast(".").replace("_", " "); textSize = 15f; setTextColor(currentTheme.textPrimary); layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f) }
                            songRow.addView(imgCover); songRow.addView(txtName); songsListVerticalContainer.addView(songRow)
                        }
                    }
                }
            })
        }
        sectionPlaylist.addView(searchInput)
        playlistFoldersContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT) }
        scrollFoldersView = ScrollView(this).apply { layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT); addView(playlistFoldersContainer) }
        playlistSongsContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; visibility = View.GONE }
        val topBarFolder = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, 0, 0, 20) }
        txtFolderTitle = TextView(this).apply { text = "Carpeta"; textSize = 20f; typeface = Typeface.DEFAULT_BOLD; layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f) }
        btnBackFolder = Button(this).apply { text = "ATRAS"; setBackgroundColor(Color.TRANSPARENT); setOnClickListener { searchInput.text.clear(); playlistSongsContainer.visibility = View.GONE; scrollFoldersView.visibility = View.VISIBLE; btnSelectMode.visibility = View.GONE; btnDeleteSelected.visibility = View.GONE } }
        btnSelectMode = Button(this).apply { text = "SELECCIONAR"; setBackgroundColor(Color.TRANSPARENT); setPadding(10,0,10,0); visibility = View.GONE }
        btnDeleteSelected = ImageButton(this).apply { setImageResource(android.R.drawable.ic_menu_delete); background = null; setPadding(20,0,20,0); visibility = View.GONE }

        btnCrearPlaylist = Button(this).apply { text = "Crear Carpeta"; setOnClickListener { mostrarDialogoCrear() }; layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, 20) } }
        txtFoldersHeader = TextView(this).apply { text = "Carpetas Existentes"; textSize = 22f; typeface = Typeface.DEFAULT_BOLD; setPadding(0, 10, 0, 20) }
        topBarFolder.addView(btnBackFolder); topBarFolder.addView(txtFolderTitle); topBarFolder.addView(btnSelectMode); topBarFolder.addView(btnDeleteSelected)
        songsListVerticalContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        playlistSongsContainer.addView(topBarFolder); playlistSongsContainer.addView(ScrollView(this).apply { addView(songsListVerticalContainer) })
        sectionPlaylist.addView(scrollFoldersView); sectionPlaylist.addView(playlistSongsContainer)

        // --- COLA ---
        sectionQueue = LinearLayout(this).apply { layoutParams = lpPage; orientation = LinearLayout.VERTICAL; setPadding(40, 140, 40, 20) }
        txtQueueHeader = TextView(this).apply { text = "COLA DE REPRODUCCION"; textSize = 16f; typeface = Typeface.DEFAULT_BOLD; setPadding(0, 10, 0, 20) }
        sectionQueue.addView(txtQueueHeader)
        recyclerViewQueue = RecyclerView(this).apply { layoutManager = LinearLayoutManager(this@MainActivity); layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT) }
        queueAdapter = QueueAdapter(playlistActual, { currentIndex }, { currentTheme }, { pos -> resetLoopState(); currentIndex = pos; reproducirCancionActual(); queueAdapter.notifyDataSetChanged() }, { vh -> itemTouchHelper.startDrag(vh) }, { isDark -> getGlassDrawable(isDark) }, { path, imgView -> loadCoverListAsync(path, imgView) })
        recyclerViewQueue.adapter = queueAdapter
        val callback = object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {
            override fun onMove(r: RecyclerView, vh: RecyclerView.ViewHolder, tgt: RecyclerView.ViewHolder): Boolean { val fP = vh.adapterPosition; val tP = tgt.adapterPosition; if (fP == currentIndex) currentIndex = tP else if (fP < currentIndex && tP >= currentIndex) currentIndex-- else if (fP > currentIndex && tP <= currentIndex) currentIndex++; queueAdapter.onItemMove(fP, tP); return true }
            override fun onSwiped(vh: RecyclerView.ViewHolder, d: Int) {}
            override fun isLongPressDragEnabled(): Boolean = false
            override fun onSelectedChanged(vh: RecyclerView.ViewHolder?, state: Int) { super.onSelectedChanged(vh, state); if (state == ItemTouchHelper.ACTION_STATE_DRAG) { vh?.itemView?.alpha = 0.8f; vh?.itemView?.scaleX = 1.02f; vh?.itemView?.scaleY = 1.02f } }
            override fun clearView(r: RecyclerView, vh: RecyclerView.ViewHolder) { super.clearView(r, vh); vh.itemView.alpha = 1.0f; vh.itemView.scaleX = 1.0f; vh.itemView.scaleY = 1.0f }
        }
        itemTouchHelper = ItemTouchHelper(callback); itemTouchHelper.attachToRecyclerView(recyclerViewQueue)
        sectionQueue.addView(recyclerViewQueue)

        // --- BUSQUEDA YOUTUBE ---
        sectionSearch = LinearLayout(this).apply { layoutParams = lpPage; orientation = LinearLayout.VERTICAL; setPadding(40, 140, 40, 20) }
        txtSearchHeader = TextView(this).apply { text = "BUSCAR EN YOUTUBE"; textSize = 16f; typeface = Typeface.DEFAULT_BOLD; setPadding(0, 10, 0, 20) }
        inputYtSearch = EditText(this).apply { 
            hint = "Nombre de la canción..."
            setSingleLine()
            setPadding(40, 40, 40, 40)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, 20) }
            addTextChangedListener(object: android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: android.text.Editable?) {
                    if (s.toString().trim().isEmpty()) {
                        resultContainer.removeAllViews()
                        resultContainer.visibility = View.GONE
                    }
                }
            })
        }
        btnDoSearch = Button(this).apply { text = "BUSCAR"; setBackgroundColor(Color.TRANSPARENT) }
        resultContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 20, 0, 40) }; visibility = View.GONE }
        sectionSearch.addView(txtSearchHeader); sectionSearch.addView(inputYtSearch); sectionSearch.addView(btnDoSearch); val searchScroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
            overScrollMode = View.OVER_SCROLL_NEVER
            isVerticalScrollBarEnabled = false
        }
        searchScroll.addView(resultContainer)
        sectionSearch.addView(searchScroll)
        
        

        btnDoSearch.setOnClickListener {
        val q = inputYtSearch.text.toString().trim()
        if (q.isEmpty()) return@setOnClickListener
        btnDoSearch.text = "BUSCANDO..."
        lifecycleScope.launch {
            val results = YtDlpRepository(this@MainActivity).searchYoutube(q, 5)
            resultContainer.removeAllViews()
            if (results.isNotEmpty()) {
                for (data in results) {
                    val row = LinearLayout(this@MainActivity).apply { 
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        setPadding(25, 25, 25, 25)
                        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 8, 0, 8) }
                        background = getGlassDrawable(currentTheme.isDark) 
                    }
                    val img = ImageView(this@MainActivity).apply { 
                        layoutParams = LinearLayout.LayoutParams(130, 130).apply { setMargins(0, 0, 30, 0) }
                        scaleType = ImageView.ScaleType.CENTER_CROP
                        clipToOutline = true
                        outlineProvider = object : ViewOutlineProvider() { 
                            override fun getOutline(view: View, outline: Outline) { outline.setRoundRect(0, 0, view.width, view.height, 20f) } 
                        }
                        setBackgroundColor(Color.parseColor(if(currentTheme.isDark) "#2A2A2A" else "#E0E0E0")) 
                    }
                    val thumbUrl = data["thumbnail"] ?: ""
                    loadExternalImageAsync(thumbUrl, img)
                    
                    val titleText = data["title"] ?: ""
                    val txt = TextView(this@MainActivity).apply { 
                        text = titleText
                        textSize = 15f
                        setTextColor(currentTheme.textPrimary)
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(0,0,15,0) }
                        maxLines = 2
                        ellipsize = android.text.TextUtils.TruncateAt.END 
                    }
                    val btnDl = ImageButton(this@MainActivity).apply { 
                        setImageResource(android.R.drawable.stat_sys_download)
                        background = null
                        setPadding(20,0,20,0)
                        setColorFilter(currentTheme.accentDark) 
                    }
                    
                    btnDl.setOnClickListener {
                        val ytId = data["id"] ?: ""
                        val ytUrl = "https://www.youtube.com/watch?v=$ytId"
                        android.widget.Toast.makeText(this@MainActivity, "Iniciando descarga...", android.widget.Toast.LENGTH_SHORT).show()
                        val destFolder = java.io.File(getExternalFilesDir(null), "Base-Musica/Recien agregados")
                        if (!destFolder.exists()) { destFolder.mkdirs() }
                        viewModel.loadLocalMusic()
                        viewModel.downloadSingleTrack(ytUrl, "Recien agregados") { success ->
                            if (success) {
                                android.widget.Toast.makeText(this@MainActivity, "¡Descarga completa!", android.widget.Toast.LENGTH_SHORT).show()
                            } else {
                                android.widget.Toast.makeText(this@MainActivity, "Error en descarga", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    row.addView(img)
                    row.addView(txt)
                    row.addView(btnDl)
                    resultContainer.addView(row)
                }
                resultContainer.visibility = View.VISIBLE
            } else {
                android.widget.Toast.makeText(this@MainActivity, "No se encontró nada", android.widget.Toast.LENGTH_SHORT).show()
            }
            btnDoSearch.text = "BUSCAR"
        }
    }
        

        // --- AJUSTES ---
        sectionSettings = LinearLayout(this).apply { layoutParams = lpPage; orientation = LinearLayout.VERTICAL; setPadding(40, 140, 40, 20) }
        txtSettingsHeader = TextView(this).apply { text = "Ajustes del Sistema"; textSize = 22f; setPadding(0, 20, 0, 30); setTypeface(null, Typeface.BOLD) }
        btnCambiarTema = Button(this).apply { text = "Cambiar Tema Visual"; setOnClickListener { mostrarSelectorTemas() }; layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, 20) } }
        val txtHeaderPlayback = TextView(this).apply { text = "Ajustes de Reproducción"; textSize = 22f; setPadding(0, 60, 0, 30); setTypeface(null, Typeface.BOLD) }
        switchCrossfade = Switch(this).apply { text = "Fundido Suave (Fade Out)"; textSize = 16f; isChecked = isCrossfadeEnabled; setPadding(40,40,40,40); setOnCheckedChangeListener { _, isChecked -> isCrossfadeEnabled = isChecked; prefs.edit().putBoolean("CROSSFADE", isChecked).apply() } }
        btnSleepTimer = Button(this).apply { text = "Temporizador de Apagado"; setOnClickListener { mostrarDialogoSleepTimer() }; layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 20, 0, 20) } }

        sectionSettings.addView(txtSettingsHeader); sectionSettings.addView(btnCambiarTema); sectionSettings.addView(txtHeaderPlayback); sectionSettings.addView(switchCrossfade); sectionSettings.addView(btnSleepTimer)
        
        pagesContainer.addView(sectionHome); pagesContainer.addView(sectionPlaylist); pagesContainer.addView(sectionQueue); pagesContainer.addView(sectionSearch); pagesContainer.addView(sectionSettings)
        tabScrollView.addView(pagesContainer); contentFrame.addView(tabScrollView)

        // --- MENÚ INFERIOR ---
        bottomNavGlass = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; elevation = 20f; gravity = Gravity.CENTER; layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, 120).apply { gravity = Gravity.BOTTOM; setMargins(30, 0, 30, 80) } }
        btnHomeNav = Button(this).apply { text = "INICIO"; textSize = 11f; maxLines = 1; background = null; setOnClickListener { cambiarPestana(0) } }
        btnPlaylistsNav = Button(this).apply { text = "PLAYLIST"; textSize = 11f; maxLines = 1; background = null; setOnClickListener { cambiarPestana(1) } }
        btnListNav = Button(this).apply { text = "COLA"; textSize = 11f; maxLines = 1; background = null; setOnClickListener { cambiarPestana(2) } }
        btnSearchNav = Button(this).apply { text = "BUSCAR"; textSize = 11f; maxLines = 1; background = null; setOnClickListener { cambiarPestana(3) } }
        btnSettingsNav = Button(this).apply { text = "AJUSTES"; textSize = 11f; maxLines = 1; background = null; setOnClickListener { cambiarPestana(4) } }
        bottomNavGlass.addView(btnHomeNav, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f))
        bottomNavGlass.addView(btnPlaylistsNav, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f))
        bottomNavGlass.addView(btnListNav, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f))
        bottomNavGlass.addView(btnSearchNav, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f))
        bottomNavGlass.addView(btnSettingsNav, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f))
        contentFrame.addView(bottomNavGlass); mainLayout.addView(contentFrame)
    }

    private fun mostrarDialogoSleepTimer() {
        val options = arrayOf("Desactivar", "15 minutos", "30 minutos", "45 minutos", "60 minutos")
        androidx.appcompat.app.AlertDialog.Builder(this).setTitle("Apagar música en...").setItems(options) { _, w ->
            handler.removeCallbacksAndMessages("SLEEP_TIMER")
            if (w == 0) { Toast.makeText(this, "Temporizador desactivado", Toast.LENGTH_SHORT).show(); return@setItems }
            val mins = w * 15; handler.postDelayed({ if (mediaPlayer?.isPlaying == true) togglePlayPause(); Toast.makeText(this, "Música apagada", Toast.LENGTH_LONG).show() }, "SLEEP_TIMER", (mins * 60 * 1000).toLong())
            Toast.makeText(this, "La música se pausará en $mins minutos", Toast.LENGTH_SHORT).show()
        }.show()
    }

    private fun mostrarSelectorTemas() { val themeNames = THEMES.keys.toTypedArray(); androidx.appcompat.app.AlertDialog.Builder(this).setTitle("Selecciona un Tema").setItems(themeNames) { _, which -> val selectedName = themeNames[which]; prefs.edit().putString("APP_THEME", selectedName).apply(); animateThemeTransition(THEMES[selectedName]!!) }.show() }

    private fun toggleFavCurrentSong() {
        if (playlistActual.isEmpty()) return
        val rutaArchivo = playlistActual[currentIndex]
        
        if (rutaArchivo.startsWith("LIVE_STREAM|")) {
            val parts = rutaArchivo.split("|")
            val ytUrl = parts.getOrNull(4)
            if (ytUrl.isNullOrEmpty()) return
            
            val isNowLiked = !btnFavRadial.isLiked
            btnFavRadial.setLikedState(isNowLiked)
            
            if (isNowLiked) {
                Toast.makeText(this, "Descargando a 'Mi favs' localmente...", Toast.LENGTH_SHORT).show()
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val destFolder = File(getExternalFilesDir(null), "Base-Musica/Mi favs").apply { mkdirs() }
                        val req = YoutubeDLRequest(ytUrl)
                        req.addOption("-x")
                        req.addOption("--audio-format", "mp3")
                    req.addOption("--embed-thumbnail")
                    req.addOption("--embed-metadata")
                        req.addOption("-o", File(destFolder, "%(title)s.%(ext)s").absolutePath)
                        YoutubeDL.getInstance().execute(req, null, null)
                        withContext(Dispatchers.Main) { viewModel.loadLocalMusic() }
                    } catch (e: Exception) { }
                }
            } else {
                Toast.makeText(this, "Para quitar el Like, bórrala desde la carpeta 'Mi favs'", Toast.LENGTH_LONG).show()
                btnFavRadial.setLikedState(true) 
            }
            return
        }

        // Like para archivos físicos locales
        val isNowLiked = viewModel.toggleLike(rutaArchivo)
        btnFavRadial.setLikedState(isNowLiked)
    }

    private fun cambiarPestana(index: Int) { tabScrollView.post { tabScrollView.smoothScrollTo(index * tabScrollView.width, 0) }; if (index == 2) { queueAdapter.notifyDataSetChanged() } }

        private fun renderizarListaDeCarpetas() {
        playlistFoldersContainer.removeAllViews(); playlistFoldersContainer.addView(btnCrearPlaylist); playlistFoldersContainer.addView(txtFoldersHeader)
        mapPlaylists.keys.forEach { folderName ->
            val folderWrapper = FrameLayout(this).apply { layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 10, 0, 10) }; background = getGlassDrawable(currentTheme.isDark); outlineProvider = ViewOutlineProvider.BACKGROUND; clipToOutline = true; tag = folderName }
            val waveView = WaveFillView(this).apply { themeColor = currentTheme.accent; layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT) }
            val folderRow = LinearLayout(this).apply { layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT); orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(40, 40, 40, 40); setOnClickListener { abrirCarpetaPlaylist(folderName) } }
            folderRow.addView(TextView(this).apply { text = folderName; textSize = 16f; typeface = Typeface.DEFAULT_BOLD; setTextColor(currentTheme.textPrimary); layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f) })
            folderRow.addView(ImageButton(this).apply { setImageResource(android.R.drawable.ic_menu_edit); background = null; setColorFilter(currentTheme.accentDark); setPadding(10,10,10,10); setOnClickListener { mostrarDialogoEditar(folderName) } })
            folderWrapper.addView(waveView); folderWrapper.addView(folderRow); playlistFoldersContainer.addView(folderWrapper)
        }
    }

    private fun mostrarDialogoCrear() { 
        val input = EditText(this).apply { hint = "Nombre" }
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Nueva Carpeta")
            .setView(LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(50,40,50,40); addView(input) })
            .setPositiveButton("Crear") { _, _ -> 
                val nuevo = input.text.toString().trim()
                if (nuevo.isNotEmpty()) { 
                    File(getExternalFilesDir(null), "Base-Musica/$nuevo").mkdirs()
                    viewModel.loadLocalMusic()
                    android.widget.Toast.makeText(this@MainActivity, "Carpeta '$nuevo' creada", android.widget.Toast.LENGTH_SHORT).show()
                } 
            }
            .setNegativeButton("Cancelar", null)
            .show() 
    }

    private fun mostrarDialogoEditar(folderName: String) {
        val input = EditText(this).apply { setText(folderName) }
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Editar: $folderName")
            .setView(LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(50,40,50,40); addView(input) })
            .setPositiveButton("Renombrar") { _, _ -> 
                val nuevo = input.text.toString().trim()
                if(nuevo.isNotEmpty() && nuevo != folderName) { 
                    val old = File(getExternalFilesDir(null), "Base-Musica/$folderName")
                    val new = File(getExternalFilesDir(null), "Base-Musica/$nuevo")
                    if(old.exists()) { 
                        old.renameTo(new)
                        viewModel.loadLocalMusic()
                        // Seguridad al renombrar la playlist actual
                        if (nombrePlaylistActual == folderName) {
                            nombrePlaylistActual = nuevo
                            txtArtist.text = "Playlist: $nombrePlaylistActual"
                        }
                    } 
                } 
            }
            .setNegativeButton("Borrar") { _, _ -> 
                val f = File(getExternalFilesDir(null), "Base-Musica/$folderName")
                if(f.exists()){ 
                    f.deleteRecursively()
                    viewModel.loadLocalMusic() 
                    
                    // Lógica de seguridad anti-crash
                    if (nombrePlaylistActual == folderName) {
                        try {
                            mediaPlayer?.pause()
                            mediaPlayer?.release()
                            mediaPlayer = null
                        } catch(e: Exception){}
                        
                        playlistActual.clear()
                        currentIndex = 0
                        nombrePlaylistActual = "General"
                        
                        txtTitle.text = "Selecciona una pista"
                        txtArtist.text = "Lista de Reproduccion"
                        txtCurrentTime.text = "00:00"
                        txtTotalTime.text = "00:00"
                        wavySeekBar.progress = 0f
                        wavySeekBar.isWaving = false
                        imgVinyl.isSpinning = false
                        imgVinyl.bitmap = null
                        btnPlayPause.setImageResource(android.R.drawable.ic_media_play)
                        
                        if (::queueAdapter.isInitialized) queueAdapter.updateList(playlistActual)
                        notificationManager.cancel(1)
                        
                        
                    }
                } 
            }
            .setNeutralButton("Cancelar", null)
            .show() 
    }

    private fun abrirCarpetaPlaylist(folderName: String) { 
        val lista = mapPlaylists[folderName] ?: return; txtFolderTitle.text = folderName; songsListVerticalContainer.removeAllViews()
        btnSelectMode.visibility = View.VISIBLE
        var isSelectionMode = false; val selectedSongs = mutableSetOf<String>()

        fun updateSelectionUI() {
            btnSelectMode.text = if (isSelectionMode) "CANCELAR" else "SELECCIONAR"
            btnDeleteSelected.visibility = if (isSelectionMode && selectedSongs.isNotEmpty()) View.VISIBLE else View.GONE
            for (i in 0 until songsListVerticalContainer.childCount) {
                val row = songsListVerticalContainer.getChildAt(i) as? LinearLayout ?: continue
                val songPath = row.tag as? String ?: continue; val dot = row.getChildAt(2) as? View ?: continue
                if (isSelectionMode) { dot.visibility = View.VISIBLE; (dot.background as GradientDrawable).setColor(if (selectedSongs.contains(songPath)) currentTheme.accent else Color.TRANSPARENT) } else { dot.visibility = View.GONE }
            }
        }

        btnSelectMode.setOnClickListener { isSelectionMode = !isSelectionMode; if (!isSelectionMode) selectedSongs.clear(); updateSelectionUI() }
        btnDeleteSelected.setOnClickListener {
            if (selectedSongs.isEmpty()) return@setOnClickListener
            
            val currentPath = if (playlistActual.isNotEmpty() && currentIndex in playlistActual.indices) playlistActual[currentIndex] else null
            var wasPlayingDeleted = false
            
            for (song in selectedSongs) { 
                java.io.File(song).delete() 
                if (song == currentPath) wasPlayingDeleted = true
                playlistActual.remove(song)
            }
            
            if (wasPlayingDeleted) {
                if (playlistActual.isEmpty()) {
                    try { mediaPlayer?.pause() } catch(e:Exception){}
                    currentIndex = 0; txtTitle.text = "Selecciona una pista"; txtArtist.text = "Lista de Reproducción"
                } else {
                    currentIndex = currentIndex.coerceAtMost(playlistActual.size - 1)
                    reproducirCancionActual()
                }
            } else if (currentPath != null && playlistActual.contains(currentPath)) {
                currentIndex = playlistActual.indexOf(currentPath)
            }
            
            if (::queueAdapter.isInitialized) queueAdapter.updateList(playlistActual)
            isSelectionMode = false; selectedSongs.clear(); btnBackFolder.performClick(); viewModel.loadLocalMusic()
        }

        lista.forEachIndexed { idx, songPath ->
            val songRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(25, 25, 25, 25); background = getGlassDrawable(currentTheme.isDark); layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 8, 0, 8) }; tag = songPath }
            val imgCover = ImageView(this).apply { layoutParams = LinearLayout.LayoutParams(130, 130).apply { setMargins(0, 0, 30, 0) }; scaleType = ImageView.ScaleType.CENTER_CROP; clipToOutline = true; outlineProvider = object : ViewOutlineProvider() { override fun getOutline(view: View, outline: Outline) { outline.setRoundRect(0, 0, view.width, view.height, 20f) } }; setBackgroundColor(Color.parseColor(if(currentTheme.isDark) "#2A2A2A" else "#E0E0E0")) }
            loadCoverListAsync(songPath, imgCover)
            val txtName = TextView(this).apply { text = songPath.substringAfterLast("/").substringBeforeLast(".").replace("_", " "); textSize = 15f; setTextColor(currentTheme.textPrimary); layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f) }
            val dotSelection = View(this).apply { layoutParams = LinearLayout.LayoutParams(40, 40).apply { setMargins(20, 0, 10, 0) }; background = GradientDrawable().apply { cornerRadius = 20f; setColor(Color.TRANSPARENT); setStroke(4, currentTheme.accentDark) }; visibility = View.GONE }

            songRow.setOnClickListener { 
                if (isSelectionMode) { if (selectedSongs.contains(songPath)) selectedSongs.remove(songPath) else selectedSongs.add(songPath); updateSelectionUI() } else { resetLoopState(); nombrePlaylistActual = folderName; playlistActual = lista.toMutableList(); currentIndex = idx; if(::queueAdapter.isInitialized) queueAdapter.updateList(playlistActual); reproducirCancionActual(); cambiarPestana(0) }
            }
            songRow.addView(imgCover); songRow.addView(txtName); songRow.addView(dotSelection); songsListVerticalContainer.addView(songRow)
        }
        updateSelectionUI(); scrollFoldersView.visibility = View.GONE; playlistSongsContainer.visibility = View.VISIBLE
    }

    private fun resetLoopState() {
        isLooping = false
        mediaPlayer?.isLooping = false
        (dotLoop.background as GradientDrawable).setColor(Color.TRANSPARENT)
        dotLoop.invalidate()
    }

    private fun cargarCaratula(rutaArchivo: String) { 
        imgVinyl.bitmap = null
        if (rutaArchivo.startsWith("LIVE_STREAM|")) {
            val urlImg = rutaArchivo.split("|").getOrNull(3) ?: ""
            if (urlImg.isNotEmpty()) {
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val conn = URL(urlImg).openConnection() as HttpURLConnection
                        conn.connectTimeout = 5000; conn.doInput = true; conn.connect()
                        if (conn.responseCode == 200) { val bmp = BitmapFactory.decodeStream(conn.inputStream); withContext(Dispatchers.Main) { imgVinyl.bitmap = bmp; actualizarNotificacion() } }
                        conn.disconnect()
                    } catch (e: Exception) {}
                }
            }
            return
        }
        lifecycleScope.launch(Dispatchers.IO) { 
            try { 
                val mmr = MediaMetadataRetriever()
                mmr.setDataSource(rutaArchivo)
                val pic = mmr.embeddedPicture
                if (pic != null) {
                    val bmp = BitmapFactory.decodeByteArray(pic, 0, pic.size)
                    withContext(Dispatchers.Main) { imgVinyl.bitmap = bmp; actualizarNotificacion() }
                } else {
                    val baseName = File(rutaArchivo).nameWithoutExtension
                    val dir = File(rutaArchivo).parentFile; val cover = dir?.listFiles()?.firstOrNull { it.name.startsWith(baseName) && (it.name.endsWith(".jpg") || it.name.endsWith(".png")) }
                    if (cover != null) withContext(Dispatchers.Main) { imgVinyl.bitmap = BitmapFactory.decodeFile(cover.absolutePath); actualizarNotificacion() }
                }
                mmr.release()
            } catch (e: Exception) { }
        } 
    }

    private fun reproducirCancionActual() {
        if (playlistActual.isEmpty() || currentIndex >= playlistActual.size || currentIndex < 0) return
        val rutaArchivo = playlistActual[currentIndex]
        
        if (rutaArchivo.startsWith("LIVE_STREAM|")) {
            val parts = rutaArchivo.split("|")
            val streamUrl = parts.getOrNull(1) ?: return
            val rawTitle = parts.getOrNull(2) ?: "Stream"
            txtTitle.text = rawTitle.replace("_", " ")
            txtArtist.text = "Playlist: $nombrePlaylistActual"
            
            val estaEnFavs = mapPlaylists["Mi favs"]?.any { it.contains(rawTitle, ignoreCase = true) } == true
            btnFavRadial.setLikedState(estaEnFavs)
            
            cargarCaratula(rutaArchivo)
            // FYP Eliminado
            
            try {
                mediaPlayer?.release()
                mediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).setUsage(AudioAttributes.USAGE_MEDIA).build())
                    setOnErrorListener { _, _, _ -> true }
                    setDataSource(streamUrl)
                    prepareAsync()
                    setOnPreparedListener { it.start(); btnPlayPause.setImageResource(android.R.drawable.ic_media_pause); wavySeekBar.isWaving = true; imgVinyl.isSpinning = true; txtTotalTime.text = formatTime(it.duration); actualizarBarraProgreso(); actualizarNotificacion() }
                    setOnCompletionListener { reproducirSiguiente() }
                }
            } catch (e: Exception) {}
            return
        }

        val nombreCorto = rutaArchivo.substringAfterLast("/")
        txtTitle.text = nombreCorto.substringBeforeLast(".").replace("_", " "); txtArtist.text = "Playlist: $nombrePlaylistActual"
        btnFavRadial.setLikedState(viewModel.isSongLiked(rutaArchivo)); cargarCaratula(rutaArchivo)
        // FYP Eliminado
        
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).setUsage(AudioAttributes.USAGE_MEDIA).build())
                setOnErrorListener { _, _, _ -> true }
                setDataSource(rutaArchivo)
                prepareAsync()
                setOnPreparedListener { it.start(); btnPlayPause.setImageResource(android.R.drawable.ic_media_pause); wavySeekBar.isWaving = true; imgVinyl.isSpinning = true; txtTotalTime.text = formatTime(it.duration); actualizarBarraProgreso(); actualizarNotificacion() }
                setOnCompletionListener { reproducirSiguiente() }
            }
        } catch (e: Exception) {}
    }

    private fun actualizarNotificacion() { 
        if (mediaPlayer == null) return
        try {
            val stateBuilder = PlaybackState.Builder().setActions(PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE or PlaybackState.ACTION_SKIP_TO_NEXT or PlaybackState.ACTION_SKIP_TO_PREVIOUS or PlaybackState.ACTION_SEEK_TO)
            stateBuilder.setState(if (mediaPlayer!!.isPlaying) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED, mediaPlayer!!.currentPosition.toLong(), 1f)
            mediaSession.setPlaybackState(stateBuilder.build())
            val metaBuilder = MediaMetadata.Builder().putString(MediaMetadata.METADATA_KEY_TITLE, txtTitle.text.toString()).putString(MediaMetadata.METADATA_KEY_ARTIST, txtArtist.text.toString()).putLong(MediaMetadata.METADATA_KEY_DURATION, mediaPlayer!!.duration.toLong())
            imgVinyl.bitmap?.let { metaBuilder.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, it) }
            mediaSession.setMetadata(metaBuilder.build())
            val notif = Notification.Builder(this, "SDM_CHANNEL").setSmallIcon(android.R.drawable.ic_media_play).setContentTitle(txtTitle.text).setContentText(txtArtist.text).setLargeIcon(imgVinyl.bitmap?.let { Icon.createWithBitmap(it) }).setStyle(Notification.MediaStyle().setMediaSession(mediaSession.sessionToken)).build()
            notificationManager.notify(1, notif)
        } catch (e: Exception) {}
    }
    
    private fun togglePlayPause() { try { mediaPlayer?.let { if (it.isPlaying) { it.pause(); wavySeekBar.isWaving = false; imgVinyl.isSpinning = false; btnPlayPause.setImageResource(android.R.drawable.ic_media_play) } else { it.start(); wavySeekBar.isWaving = true; imgVinyl.isSpinning = true; btnPlayPause.setImageResource(android.R.drawable.ic_media_pause) }; actualizarNotificacion() } ?: run { if (playlistActual.isNotEmpty()) reproducirCancionActual() } } catch (e:Exception){} }
    private fun reproducirSiguiente() { if (playlistActual.isEmpty()) return; resetLoopState(); currentIndex = (currentIndex + 1) % playlistActual.size; reproducirCancionActual(); if (::queueAdapter.isInitialized) queueAdapter.notifyDataSetChanged() }
    private fun reproducirAnterior() { if (playlistActual.isEmpty()) return; try { mediaPlayer?.let { if (it.currentPosition > 3000) { it.seekTo(0); actualizarNotificacion(); return } } } catch(e:Exception){}; resetLoopState(); currentIndex = if (currentIndex - 1 < 0) playlistActual.size - 1 else currentIndex - 1; reproducirCancionActual(); if (::queueAdapter.isInitialized) queueAdapter.notifyDataSetChanged() }
    
    private fun actualizarBarraProgreso() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                try {
                    mediaPlayer?.let { 
                        if (it.isPlaying && it.duration > 0) { 
                            wavySeekBar.progress = it.currentPosition.toFloat() / it.duration.toFloat(); txtCurrentTime.text = formatTime(it.currentPosition); actualizarNotificacion() 
                            if (isCrossfadeEnabled) { val timeRemaining = it.duration - it.currentPosition; if (timeRemaining in 1..4000) { val volume = timeRemaining / 4000f; it.setVolume(volume, volume) } else { it.setVolume(1f, 1f) } }
                        } 
                    }
                } catch (e: Exception) {}
                handler.postDelayed(this, 1000)
            }
        }, 1000)
    }
    private fun formatTime(ms: Int): String {
        val totalSecs = ms / 1000
        val h = totalSecs / 3600
        val m = (totalSecs % 3600) / 60
        val s = totalSecs % 60
        return if (h > 0) {
            String.format("%d:%02d:%02d", h, m, s)
        } else {
            String.format("%02d:%02d", m, s)
        }
    }
    override fun onDestroy() { super.onDestroy(); try{ mediaPlayer?.release() }catch(e:Exception){}; handler.removeCallbacksAndMessages(null) }

    fun showAddToPlaylistDialog(songPath: String) {
        val playlistsMap = viewModel.playlists.value
        val playlistNames = playlistsMap.keys.toMutableList()
        
        if (playlistNames.isEmpty()) {
            playlistNames.add("Mi favs")
            playlistNames.add("Para Entrenar")
            playlistNames.add("Música Relax")
        }

        android.app.AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("Agregar canción a...")
            .setItems(playlistNames.toTypedArray()) { _, which ->
                val selectedPlaylist = playlistNames[which]
                viewModel.addSongToPlaylist(songPath, selectedPlaylist) { success ->
                    if (success) {
                        android.widget.Toast.makeText(this, "✅ Agregada a $selectedPlaylist", android.widget.Toast.LENGTH_SHORT).show()
                        
                        // Lógica de Seguridad Auto-Like
                        if (selectedPlaylist.contains("fav", ignoreCase = true) || selectedPlaylist.contains("gustas", ignoreCase = true)) {
                            if (!viewModel.isSongLiked(songPath)) {
                                viewModel.toggleLike(songPath)
                                btnFavRadial.setLikedState(true)
                            }
                        }
                    } else {
                        android.widget.Toast.makeText(this, "❌ Error al agregar archivo", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }


    

    private fun findWaveFillView(view: android.view.View): WaveFillView? {
        if (view is WaveFillView) return view
        if (view is android.view.ViewGroup) {
            for (i in 0 until view.childCount) {
                val found = findWaveFillView(view.getChildAt(i))
                if (found != null) return found
            }
        }
        return null
    }
}
