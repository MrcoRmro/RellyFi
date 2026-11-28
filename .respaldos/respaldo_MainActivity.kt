package com.sdm.music

import android.Manifest
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
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
import android.os.SystemClock
import android.text.Editable
import android.text.TextWatcher
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.graphics.ColorUtils
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.palette.graphics.Palette
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
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

fun TextView.applyContrastingText(isDark: Boolean) {
    if (isDark) {
        setTextColor(Color.WHITE)
        setShadowLayer(4f, 0f, 0f, Color.BLACK)
    } else {
        setTextColor(Color.BLACK)
        setShadowLayer(4f, 0f, 0f, Color.WHITE)
    }
}

fun EditText.applyContrastingInput(isDark: Boolean) {
    if (isDark) {
        setTextColor(Color.WHITE)
        setHintTextColor(Color.parseColor("#99FFFFFF"))
        setShadowLayer(4f, 0f, 0f, Color.BLACK)
    } else {
        setTextColor(Color.BLACK)
        setHintTextColor(Color.parseColor("#99000000"))
        setShadowLayer(4f, 0f, 0f, Color.WHITE)
    }
}

data class ThemeColors(val bgBase: Int, val w1c1: Int, val w1c2: Int, val w2c1: Int, val w2c2: Int, val w3c: Int, val textPrimary: Int, val textSecondary: Int, val accent: Int, val accentDark: Int, val accentLight: Int, val cardBg: Int, val navBg: Int, val isDark: Boolean)
val THEMES = mapOf(
    "Blanco con rojo" to ThemeColors(Color.parseColor("#FFF5F5"), Color.parseColor("#FF8A80"), Color.parseColor("#FF80AB"), Color.parseColor("#D50000"), Color.parseColor("#880E4F"), Color.parseColor("#FFCDD2"), Color.BLACK, Color.BLACK, Color.parseColor("#D50000"), Color.parseColor("#B71C1C"), Color.parseColor("#FFCDD2"), Color.parseColor("#FFFFFF"), Color.parseColor("#E6FFFFFF"), false),
    "Negro con rojo" to ThemeColors(Color.parseColor("#121212"), Color.parseColor("#880E4F"), Color.parseColor("#4A0000"), Color.parseColor("#D50000"), Color.parseColor("#220000"), Color.parseColor("#3E0000"), Color.WHITE, Color.WHITE, Color.parseColor("#D50000"), Color.parseColor("#B71C1C"), Color.parseColor("#880E4F"), Color.parseColor("#2A2A2A"), Color.parseColor("#CC121212"), true),
    "Blanco con negro" to ThemeColors(Color.parseColor("#FBFDFB"), Color.parseColor("#E0E0E0"), Color.parseColor("#BDBDBD"), Color.parseColor("#757575"), Color.parseColor("#424242"), Color.parseColor("#EEEEEE"), Color.BLACK, Color.BLACK, Color.parseColor("#424242"), Color.parseColor("#212121"), Color.parseColor("#E0E0E0"), Color.parseColor("#FFFFFF"), Color.parseColor("#E6FFFFFF"), false),
    "Negro con morado" to ThemeColors(Color.parseColor("#121212"), Color.parseColor("#4A148C"), Color.parseColor("#311B92"), Color.parseColor("#7B1FA2"), Color.parseColor("#12005E"), Color.parseColor("#2A0054"), Color.WHITE, Color.WHITE, Color.parseColor("#AB47BC"), Color.parseColor("#8E24AA"), Color.parseColor("#4A148C"), Color.parseColor("#2A2A2A"), Color.parseColor("#CC121212"), true),
    "Blanco con morado" to ThemeColors(Color.parseColor("#F4F0FF"), Color.parseColor("#B39DDB"), Color.parseColor("#CE93D8"), Color.parseColor("#6A1B9A"), Color.parseColor("#4A148C"), Color.parseColor("#E1BEE7"), Color.BLACK, Color.BLACK, Color.parseColor("#8E24AA"), Color.parseColor("#4A148C"), Color.parseColor("#E1BEE7"), Color.parseColor("#FFFFFF"), Color.parseColor("#E6FFFFFF"), false),
    "Negro con verde" to ThemeColors(Color.parseColor("#121212"), Color.parseColor("#1B5E20"), Color.parseColor("#003300"), Color.parseColor("#2E7D32"), Color.parseColor("#001100"), Color.parseColor("#002200"), Color.WHITE, Color.WHITE, Color.parseColor("#4CAF50"), Color.parseColor("#388E3C"), Color.parseColor("#1B5E20"), Color.parseColor("#2A2A2A"), Color.parseColor("#CC121212"), true),
    "Blanco con verde" to ThemeColors(Color.parseColor("#F0FDF4"), Color.parseColor("#A5D6A7"), Color.parseColor("#81C784"), Color.parseColor("#2E7D32"), Color.parseColor("#1B5E20"), Color.parseColor("#C8E6C9"), Color.BLACK, Color.BLACK, Color.parseColor("#388E3C"), Color.parseColor("#1B5E20"), Color.parseColor("#C8E6C9"), Color.parseColor("#FFFFFF"), Color.parseColor("#E6FFFFFF"), false)
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
        themeColors?.let { 
            val isDark = it.isDark
            val c = if (drawRadius > width / 3f) Color.WHITE else (if (isDark) Color.WHITE else Color.BLACK)
            textPaint.color = c
            textPaint.setShadowLayer(4f, 0f, 0f, if (c == Color.WHITE) Color.BLACK else Color.WHITE)
        }
        val textY = (height / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f)
        canvas.drawText("LIKE", width / 2f, textY, textPaint)
    }
}

class SwipePagingScrollView(context: Context) : HorizontalScrollView(context) {
    var currentPage: Int = 0
    var onPageSelected: ((Int) -> Unit)? = null
    private var downX = 0f
    private var downY = 0f
    private var isDraggingHorizontally = false
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            if (e1 == null) return false
            val diffX = e2.x - e1.x
            val diffY = e2.y - e1.y
            if (kotlin.math.abs(diffX) > kotlin.math.abs(diffY) && kotlin.math.abs(diffX) > 80 && kotlin.math.abs(velocityX) > 250) {
                if (diffX < 0) {
                    goToPage(currentPage + 1)
                } else {
                    goToPage(currentPage - 1)
                }
                return true
            }
            return false
        }
    })

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(ev)
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = ev.x
                downY = ev.y
                isDraggingHorizontally = false
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = kotlin.math.abs(ev.x - downX)
                val dy = kotlin.math.abs(ev.y - downY)
                if (dx > touchSlop && dx > dy * 1.3f) {
                    isDraggingHorizontally = true
                    parent?.requestDisallowInterceptTouchEvent(true)
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDraggingHorizontally = false
            }
        }
        return isDraggingHorizontally
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (gestureDetector.onTouchEvent(ev)) return true

        when (ev.actionMasked) {
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val diffX = ev.x - downX
                val pageWidth = if (width > 0) width else resources.displayMetrics.widthPixels
                val threshold = pageWidth / 4f
                if (diffX < -threshold) {
                    goToPage(currentPage + 1)
                } else if (diffX > threshold) {
                    goToPage(currentPage - 1)
                } else {
                    goToPage(currentPage)
                }
                isDraggingHorizontally = false
                return true
            }
        }
        return true
    }

    fun goToPage(page: Int) {
        val target = page.coerceIn(0, 4)
        currentPage = target
        val pageWidth = if (width > 0) width else resources.displayMetrics.widthPixels
        post {
            smoothScrollTo(target * pageWidth, 0)
        }
        onPageSelected?.invoke(target)
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

class QueueAdapter(var songs: MutableList<String>, private var currentIndexProvider: () -> Int, private var themeProvider: () -> ThemeColors, private val onItemClick: (Int) -> Unit, private val onStartDrag: (RecyclerView.ViewHolder) -> Unit, private val getGlassDrawable: (Boolean) -> GradientDrawable, private val loadCover: (String, ImageView) -> Unit, private val onListUpdated: (List<String>) -> Unit = {}) : RecyclerView.Adapter<QueueAdapter.QueueViewHolder>() {
    fun updateList(newList: MutableList<String>) { this.songs = newList; onListUpdated(newList); notifyDataSetChanged() }
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
        
        if ((path.startsWith("LIVE_STREAM|") || path.startsWith("PENDING_YT|"))) {
            val parts = path.split("|")
            holder.txtSongName.text = parts.getOrNull(2)?.replace("_", " ") ?: "Live Audio"
        } else {
            holder.txtSongName.text = when { path.startsWith("LIVE_STREAM|") || path.startsWith("PENDING_YT|") -> path.split("|").getOrNull(2)?.replace("_", " ") ?: "Stream"; else -> path.substringAfterLast("/").substringBeforeLast(".") }.replace("_", " ")
        }
        
        holder.txtSongName.applyContrastingText(theme.isDark)
        holder.txtSongName.typeface = if (isCurrent) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        holder.txtHandle.applyContrastingText(theme.isDark)
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
    private lateinit var tabScrollView: SwipePagingScrollView
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
    private lateinit var mediaStageContainer: FrameLayout
    private lateinit var imgVinyl: VinylDiskView
    private lateinit var tvContainer: FrameLayout
    private lateinit var tvTextureView: TextureView
    private lateinit var tvStaticView: TvStaticView
    private lateinit var tvOverlayLayout: LinearLayout
    private lateinit var txtTvStatus: TextView
    private lateinit var btnDownloadClip: Button
    private lateinit var txtTvBadge: TextView
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
    
    private lateinit var btnAddToPlaylist: Button
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
    private lateinit var btnDownloadPlaylist: Button
    private lateinit var txtDownloadQueueStatus: TextView
    private lateinit var resultContainer: LinearLayout
    private lateinit var txtSettingsHeader: TextView
    private lateinit var switchCrossfade: Switch
    private lateinit var switchDynamicTheme: Switch
    private lateinit var btnOpcionesDescarga: Button
    private lateinit var btnSleepTimer: Button
    private lateinit var btnCambiarTema: Button
    
    private lateinit var bottomNavGlass: LinearLayout
    private lateinit var btnHomeNav: Button
    private lateinit var btnPlaylistsNav: Button
    private lateinit var btnListNav: Button
    private lateinit var btnSearchNav: Button
    private lateinit var btnSettingsNav: Button

    private var musicService: MusicPlaybackService? = null
    private var isServiceBound: Boolean = false
    private var pendingPlayRequest: Boolean = false
    private var videoMediaPlayer: MediaPlayer? = null
    private var videoSurface: Surface? = null
    private var isTvMode: Boolean = false
    private var isVideoPrepared: Boolean = false
    private var videoStreamJob: kotlinx.coroutines.Job? = null
    private data class CachedStream(val audioUrl: String?, val videoUrl: String?, val timestampMs: Long)
    private val streamCache = java.util.concurrent.ConcurrentHashMap<String, CachedStream>()
    private var isDownloadingClip: Boolean = false
    private var isAutoDownloadVideoEnabled: Boolean = true
    private var isStreamOnlyEnabled: Boolean = false
    private val activeVideoDownloads = ConcurrentHashMap.newKeySet<String>()
    private var liveVideoThemeController: LiveVideoThemeController? = null
    private var liveThemeAnimator: ValueAnimator? = null
    private var lastLiveThemeUpdateMs: Long = 0L
    private var hasPendingFullUiSync: Boolean = false
    private var isCrossfadeEnabled: Boolean = false
    private var mapPlaylists: MutableMap<String, List<String>> = mutableMapOf()
    private var playlistActual: MutableList<String> = mutableListOf()
    private var nombrePlaylistActual: String = "General"
    private var currentIndex: Int = 0
    private var isLooping: Boolean = false
    private var isDynamicThemeEnabled: Boolean = true
    private var savedStaticTheme: ThemeColors? = null
    private val handler = Handler(Looper.getMainLooper())

    private val playbackListener = object : MusicPlaybackService.PlaybackListener {
        override fun onTrackChanged(songPath: String, index: Int, title: String, playlistName: String) {
            resetBarraReproduccionCambioTema()
            currentIndex = index
            nombrePlaylistActual = playlistName
            txtTitle.text = title
            txtArtist.text = "Playlist: $playlistName"

            if (songPath.startsWith("LIVE_STREAM|") || songPath.startsWith("PENDING_YT|")) {
                val rawTitle = songPath.split("|").getOrNull(2).orEmpty()
                val estaEnFavs = mapPlaylists["Mi favs"]?.any { it.contains(rawTitle, ignoreCase = true) } == true
                btnFavRadial.setLikedState(estaEnFavs)
            } else {
                btnFavRadial.setLikedState(viewModel.isSongLiked(songPath))
            }

            if (isTvMode) {
                loadVideoForCurrentTrack()
            } else {
                releaseVideoPlayer()
            }

            viewModel.triggerFypForSong(songPath, title)
            if (::queueAdapter.isInitialized) queueAdapter.notifyDataSetChanged()
        }

        override fun onPlaybackStateChanged(isPlaying: Boolean) {
            if (isPlaying) {
                btnPlayPause.setImageResource(android.R.drawable.ic_media_pause)
                wavySeekBar.isWaving = true
                imgVinyl.isSpinning = true
                if (isTvMode && isVideoPrepared) {
                    try { videoMediaPlayer?.let { vp -> if (!vp.isPlaying) vp.start() } } catch (_: Exception) {}
                }
            } else {
                btnPlayPause.setImageResource(android.R.drawable.ic_media_play)
                wavySeekBar.isWaving = false
                imgVinyl.isSpinning = false
                if (isVideoPrepared) {
                    try { videoMediaPlayer?.pause() } catch (_: Exception) {}
                }
            }
        }

        override fun onDurationReady(durationMs: Int) {
            txtTotalTime.text = formatTime(durationMs)
        }

        override fun onAlbumArtLoaded(bitmap: Bitmap?) {
            imgVinyl.bitmap = bitmap
            if (bitmap != null && !isTvMode) {
                extraerTemaDinamicoDeCaratula(bitmap)
            }
        }

        override fun onQueueUpdated(newList: MutableList<String>, currentIndex: Int) {
            playlistActual = newList
            this@MainActivity.currentIndex = currentIndex
            if (::queueAdapter.isInitialized) queueAdapter.updateList(playlistActual) else precargarCaratulasCola(playlistActual)
        }

        override fun onPlayerStopped() {
            releaseVideoPlayer()
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
            if (isTvMode) loadVideoForCurrentTrack()
            if (::queueAdapter.isInitialized) queueAdapter.updateList(playlistActual) else precargarCaratulasCola(playlistActual)
        }
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as? MusicPlaybackService.LocalBinder ?: return
            musicService = binder.getService().also { srv ->
                isServiceBound = true
                srv.isCrossfadeEnabled = isCrossfadeEnabled
                srv.isLooping = isLooping
                srv.setPlaybackListener(playbackListener)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isServiceBound = false
            musicService = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            windowManager.defaultDisplay.supportedModes.maxByOrNull { it.refreshRate }?.let { maxMode ->
                val layoutParams = window.attributes
                layoutParams.preferredDisplayModeId = maxMode.modeId
                window.attributes = layoutParams
            }
        }
        
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { window.setDecorFitsSystemWindows(false) } 
        else { window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION }
        window.statusBarColor = Color.TRANSPARENT; window.navigationBarColor = Color.TRANSPARENT
        prefs = getSharedPreferences("SDM_SESSION", Context.MODE_PRIVATE)
        isCrossfadeEnabled = prefs.getBoolean("CROSSFADE", false)
        isAutoDownloadVideoEnabled = prefs.getBoolean("AUTO_DOWNLOAD_VIDEO", true)
        isStreamOnlyEnabled = prefs.getBoolean("STREAM_ONLY_MODE", false)
        val savedThemeName = prefs.getString("APP_THEME", "Blanco con rojo") ?: "Blanco con rojo"
        currentTheme = THEMES[savedThemeName] ?: THEMES["Blanco con rojo"]!!
        
        mainLayout = CoordinatorLayout(this)
        waveBackgroundView = WaveMeshGradientView(this).apply { theme = currentTheme }
        mainLayout.addView(waveBackgroundView, CoordinatorLayout.LayoutParams(CoordinatorLayout.LayoutParams.MATCH_PARENT, CoordinatorLayout.LayoutParams.MATCH_PARENT))
        
        crearPantallaReproductor()
        updateUIColors(currentTheme)
        setContentView(mainLayout)
        actualizarBarraProgreso()

        liveVideoThemeController = LiveVideoThemeController(lifecycleScope) { primary, secondary, _, _ ->
            if (isTvMode && isDynamicThemeEnabled && isVideoPrepared) {
                val now = SystemClock.elapsedRealtime()
                if (now - lastLiveThemeUpdateMs < 500L) return@LiveVideoThemeController
                lastLiveThemeUpdateMs = now

                val isDark = ColorUtils.calculateLuminance(secondary) < 0.35
                val bgBase = if (isDark) {
                    ColorUtils.blendARGB(secondary, Color.parseColor("#111111"), 0.75f)
                } else {
                    ColorUtils.blendARGB(secondary, Color.WHITE, 0.80f)
                }
                val cardBg = if (isDark) {
                    ColorUtils.blendARGB(secondary, Color.parseColor("#1A1A1A"), 0.65f)
                } else {
                    ColorUtils.blendARGB(Color.WHITE, primary, 0.07f)
                }
                val accentLight = if (isDark) {
                    ColorUtils.blendARGB(primary, Color.parseColor("#222222"), 0.60f)
                } else {
                    ColorUtils.blendARGB(primary, Color.WHITE, 0.65f)
                }
                val primaryLum = ColorUtils.calculateLuminance(primary)
                val bgLum = ColorUtils.calculateLuminance(bgBase)
                val textColor = if (bgLum > 0.4) Color.BLACK else Color.WHITE

                val liveTheme = ThemeColors(
                    bgBase = bgBase,
                    w1c1 = ColorUtils.blendARGB(primary, secondary, 0.3f),
                    w1c2 = ColorUtils.blendARGB(primary, bgBase, 0.4f),
                    w2c1 = secondary,
                    w2c2 = ColorUtils.blendARGB(secondary, bgBase, 0.5f),
                    w3c = ColorUtils.blendARGB(secondary, bgBase, 0.75f),
                    textPrimary = textColor,
                    textSecondary = textColor,
                    accent = primary,
                    accentDark = if (primaryLum > 0.5) ColorUtils.blendARGB(primary, Color.BLACK, 0.4f) else primary,
                    accentLight = accentLight,
                    cardBg = cardBg,
                    navBg = ColorUtils.setAlphaComponent(bgBase, 204),
                    isDark = isDark
                )
                animateLiveVideoTheme(liveTheme)
            }
        }

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
            viewModel.fypHeaderLabel.collect { label ->
                sectionHome.findViewWithTag<TextView>("FYP_HEADER")?.text = label
            }
        }
        lifecycleScope.launch {
            viewModel.fypRecommendations.collect { recs ->
                val esModoRadioEnVivo = (nombrePlaylistActual == "Recomendados" || nombrePlaylistActual == "Búsqueda YouTube") &&
                    playlistActual.any { it.startsWith("LIVE_STREAM|") || it.startsWith("PENDING_YT|") }

                if (esModoRadioEnVivo && recs.isNotEmpty()) {
                    val nuevosPendientes = recs.mapNotNull { item ->
                        val vid = item["id"].orEmpty()
                        val title = item["title"].orEmpty().replace("|", "-").trim()
                        val thumb = item["thumbnail"].orEmpty()
                        if (vid.isNotBlank() && title.isNotBlank()) {
                            "PENDING_YT|$vid|$title|$thumb|https://www.youtube.com/watch?v=$vid"
                        } else null
                    }
                    if (nuevosPendientes.isNotEmpty()) {
                        val srv = musicService
                        if (srv != null) {
                            srv.appendToQueue(nuevosPendientes)
                        } else {
                            for (p in nuevosPendientes) {
                                val vid = p.split("|").getOrNull(1)
                                if (!vid.isNullOrBlank() && playlistActual.none { it.contains(vid) }) {
                                    playlistActual.add(p)
                                }
                            }
                            if (::queueAdapter.isInitialized) queueAdapter.updateList(playlistActual) else precargarCaratulasCola(playlistActual)
                        }
                    }
                }
                fypContainer.removeAllViews()
                if (recs.isEmpty()) return@collect
                txtFypHeader.text = "Canciones recomendadas"
                txtFypHeader.tag = "FYP_HEADER"
                txtFypHeader.applyContrastingText(currentTheme.isDark)
                for (item in recs) {
                    val vid = item["id"] ?: ""
                    val vtitle = item["title"] ?: ""
                    val vthumb = item["thumbnail"] ?: ""
                    
                    val row = LinearLayout(this@MainActivity).apply { 
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        setPadding(25, 25, 25, 25)
                        background = getGlassDrawable(currentTheme.isDark)
                        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 8, 0, 8) } 
                    }
                    val imgC = ImageView(this@MainActivity).apply { 
                        layoutParams = LinearLayout.LayoutParams(130, 130).apply { setMargins(0, 0, 30, 0) }
                        scaleType = ImageView.ScaleType.CENTER_CROP
                        clipToOutline = true
                        outlineProvider = object : ViewOutlineProvider() { 
                            override fun getOutline(view: View, outline: Outline) { outline.setRoundRect(0, 0, view.width, view.height, 20f) } 
                        }
                        setBackgroundColor(Color.parseColor(if(currentTheme.isDark) "#2A2A2A" else "#E0E0E0")) 
                    }
                    loadExternalImageAsync(vthumb, imgC)
                    val txtN = TextView(this@MainActivity).apply { 
                        text = vtitle
                        textSize = 15f
                        maxLines = 2
                        ellipsize = android.text.TextUtils.TruncateAt.END
                        applyContrastingText(currentTheme.isDark)
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f) 
                    }
                    row.addView(imgC)
                    row.addView(txtN)
                    row.setOnClickListener {
                        reproducirDesdeYoutube(
                            vid = vid,
                            vtitle = vtitle,
                            vthumb = vthumb,
                            origen = "Recomendados",
                            irAInicio = false
                        )
                    }
                    fypContainer.addView(row)
                }
            }
        }

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

        lifecycleScope.launch {
            viewModel.isFypLoading.collect { isLoading ->
                if (isLoading) {
                    fypContainer.removeAllViews()
                    txtFypHeader.text = "Buscando recomendaciones..."
                    txtFypHeader.applyContrastingText(currentTheme.isDark)
                    fypContainer.addView(TextView(this@MainActivity).apply { 
                        text = "Analizando canción y buscando recomendaciones..."; 
                        gravity = Gravity.CENTER; 
                        setPadding(0, 40, 0, 40); 
                        applyContrastingText(currentTheme.isDark) 
                    })
                }
            }
        }

        lifecycleScope.launch {
            viewModel.pendingDownloadsCount.collect { count ->
                if (::txtDownloadQueueStatus.isInitialized) {
                    if (count > 0) {
                        txtDownloadQueueStatus.visibility = View.VISIBLE
                        txtDownloadQueueStatus.text = "Cola de descargas: $count canciones pendientes en segundo plano"
                    } else {
                        txtDownloadQueueStatus.visibility = View.GONE
                        viewModel.loadLocalMusic()
                    }
                }
            }
        }
            aplicarMargenBarraNavegacionInferior()
}

    private fun getGlassDrawable(isDark: Boolean): GradientDrawable { val glassColor = if (isDark) Color.parseColor("#4D000000") else Color.parseColor("#66FFFFFF"); val strokeColor = if (isDark) Color.parseColor("#33FFFFFF") else Color.parseColor("#33000000"); return GradientDrawable().apply { setColor(glassColor); cornerRadius = 30f; setStroke(2, strokeColor) } }

    private fun animateThemeTransition(newTheme: ThemeColors) {
        liveThemeAnimator?.cancel()
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

    private fun animateLiveVideoTheme(newTheme: ThemeColors) {
        liveThemeAnimator?.cancel()
        hasPendingFullUiSync = true
        val startTheme = currentTheme
        val eval = ArgbEvaluator()
        liveThemeAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 450L
            addUpdateListener { anim ->
                val f = anim.animatedFraction
                val stepTheme = ThemeColors(
                    bgBase = eval.evaluate(f, startTheme.bgBase, newTheme.bgBase) as Int,
                    w1c1 = eval.evaluate(f, startTheme.w1c1, newTheme.w1c1) as Int,
                    w1c2 = eval.evaluate(f, startTheme.w1c2, newTheme.w1c2) as Int,
                    w2c1 = eval.evaluate(f, startTheme.w2c1, newTheme.w2c1) as Int,
                    w2c2 = eval.evaluate(f, startTheme.w2c2, newTheme.w2c2) as Int,
                    w3c = eval.evaluate(f, startTheme.w3c, newTheme.w3c) as Int,
                    textPrimary = newTheme.textPrimary,
                    textSecondary = newTheme.textSecondary,
                    accent = eval.evaluate(f, startTheme.accent, newTheme.accent) as Int,
                    accentDark = eval.evaluate(f, startTheme.accentDark, newTheme.accentDark) as Int,
                    accentLight = eval.evaluate(f, startTheme.accentLight, newTheme.accentLight) as Int,
                    cardBg = eval.evaluate(f, startTheme.cardBg, newTheme.cardBg) as Int,
                    navBg = eval.evaluate(f, startTheme.navBg, newTheme.navBg) as Int,
                    isDark = newTheme.isDark
                )
                currentTheme = stepTheme
                updateLiveThemeColors(stepTheme)
            }
            start()
        }
    }

    private fun updateLiveThemeColors(t: ThemeColors) {
        waveBackgroundView.theme = t
        wavySeekBar.setColors(t.accentLight, t.accent, t.accentDark)

        (btnPrev.background as? GradientDrawable)?.setColor(t.accentLight)
        (btnPlayPause.background as? GradientDrawable)?.setColor(t.accent)
        (btnNext.background as? GradientDrawable)?.setColor(t.accentLight)
        (bottomNavGlass.background as? GradientDrawable)?.setColor(t.navBg)

        if (isLooping) {
            (dotLoop.background as? GradientDrawable)?.setColor(t.accent)
            dotLoop.invalidate()
        }

        val iconTint = if (t.isDark) Color.WHITE else Color.BLACK
        btnPrev.setColorFilter(iconTint)
        btnNext.setColorFilter(iconTint)

        txtPlayingFrom.setTextColor(t.textPrimary)
        txtTitle.setTextColor(t.textPrimary)
        txtArtist.setTextColor(t.textSecondary)
        txtCurrentTime.setTextColor(t.textPrimary)
        txtTotalTime.setTextColor(t.textPrimary)
        btnShuffle.setTextColor(t.textPrimary)
        btnLoop.setTextColor(t.textPrimary)
        btnHomeNav.setTextColor(t.textPrimary)
        btnPlaylistsNav.setTextColor(t.textPrimary)
        btnListNav.setTextColor(t.textPrimary)
        btnSearchNav.setTextColor(t.textPrimary)
        btnSettingsNav.setTextColor(t.textPrimary)

        if (::btnAddToPlaylist.isInitialized) {
            btnAddToPlaylist.setTextColor(t.textPrimary)
            (btnAddToPlaylist.background as? GradientDrawable)?.setColor(
                if (t.isDark) Color.parseColor("#4D000000") else Color.parseColor("#66FFFFFF")
            )
        }
        if (::txtFypHeader.isInitialized) {
            txtFypHeader.setTextColor(t.textPrimary)
        }

        btnFavRadial.themeColors = t
    }

    private fun updateUIColors(t: ThemeColors) {
        hasPendingFullUiSync = false
        waveBackgroundView.theme = t
        txtPlayingFrom.applyContrastingText(t.isDark); imgVinyl.holeColor = t.bgBase; txtTitle.applyContrastingText(t.isDark); txtArtist.applyContrastingText(t.isDark)
        wavySeekBar.setColors(t.accentLight, t.accent, t.accentDark); txtCurrentTime.applyContrastingText(t.isDark); txtTotalTime.applyContrastingText(t.isDark)
        btnPrev.background = GradientDrawable().apply { setColor(t.accentLight); cornerRadius = 80f }; btnPrev.setColorFilter(if (t.isDark) Color.WHITE else Color.BLACK)
        btnPlayPause.background = GradientDrawable().apply { setColor(t.accent); cornerRadius = 50f }; btnPlayPause.setColorFilter(Color.WHITE)
        btnNext.background = GradientDrawable().apply { setColor(t.accentLight); cornerRadius = 80f }; btnNext.setColorFilter(if (t.isDark) Color.WHITE else Color.BLACK)
        btnShuffle.applyContrastingText(t.isDark); btnLoop.applyContrastingText(t.isDark)
        (dotLoop.background as GradientDrawable).setColor(if (isLooping) t.accent else Color.TRANSPARENT); dotLoop.invalidate()
        btnFavRadial.themeColors = t
        
        if (::btnAddToPlaylist.isInitialized) {
            btnAddToPlaylist.applyContrastingText(t.isDark)
            btnAddToPlaylist.background = getGlassDrawable(t.isDark)
        }
        txtFypHeader.applyContrastingText(t.isDark)
        for(i in 0 until fypContainer.childCount){ 
            val row = fypContainer.getChildAt(i) as? LinearLayout ?: continue
            row.background = getGlassDrawable(t.isDark)
            (row.getChildAt(1) as? TextView)?.applyContrastingText(t.isDark) 
        }
        
        searchInput.applyContrastingInput(t.isDark); searchInput.background = getGlassDrawable(t.isDark)
        txtFoldersHeader.applyContrastingText(t.isDark); txtFolderTitle.applyContrastingText(t.isDark)
        btnBackFolder.applyContrastingText(t.isDark); btnSelectMode.applyContrastingText(t.isDark); btnDeleteSelected.setColorFilter(if (t.isDark) Color.WHITE else Color.BLACK)
        txtQueueHeader.applyContrastingText(t.isDark)
        txtSearchHeader.applyContrastingText(t.isDark)
        inputYtSearch.applyContrastingInput(t.isDark); inputYtSearch.background = getGlassDrawable(t.isDark)
        btnDoSearch.applyContrastingText(t.isDark); resultContainer.background = getGlassDrawable(t.isDark)
        if (::btnDownloadPlaylist.isInitialized) {
            btnDownloadPlaylist.applyContrastingText(t.isDark)
            btnDownloadPlaylist.background = getGlassDrawable(t.isDark)
        }
        if (::txtDownloadQueueStatus.isInitialized) {
            txtDownloadQueueStatus.applyContrastingText(t.isDark)
            txtDownloadQueueStatus.background = getGlassDrawable(t.isDark)
        }
        
        txtSettingsHeader.applyContrastingText(t.isDark)
        switchCrossfade.applyContrastingText(t.isDark)
        if (::switchDynamicTheme.isInitialized) {
            switchDynamicTheme.applyContrastingText(t.isDark)
        }
        if (::btnOpcionesDescarga.isInitialized) {
            btnOpcionesDescarga.background = getGlassDrawable(t.isDark)
            btnOpcionesDescarga.applyContrastingText(t.isDark)
        }
        btnSleepTimer.background = getGlassDrawable(t.isDark); btnSleepTimer.applyContrastingText(t.isDark)
        btnCrearPlaylist.background = getGlassDrawable(t.isDark); btnCrearPlaylist.applyContrastingText(t.isDark)
        btnCambiarTema.background = getGlassDrawable(t.isDark); btnCambiarTema.applyContrastingText(t.isDark)
        bottomNavGlass.background = GradientDrawable().apply { setColor(t.navBg); cornerRadius = 80f; setStroke(2, Color.parseColor(if(t.isDark) "#44FFFFFF" else "#44000000")) }
        btnHomeNav.applyContrastingText(t.isDark); btnPlaylistsNav.applyContrastingText(t.isDark); btnListNav.applyContrastingText(t.isDark); btnSearchNav.applyContrastingText(t.isDark); btnSettingsNav.applyContrastingText(t.isDark)
        if (::tabScrollView.isInitialized) {
            actualizarNavActivo(tabScrollView.currentPage)
        }
        
        if (::queueAdapter.isInitialized) queueAdapter.notifyDataSetChanged()
        for (i in 0 until playlistFoldersContainer.childCount) { 
            val child = playlistFoldersContainer.getChildAt(i)
            if (child is FrameLayout && child.tag != null) { 
                child.background = getGlassDrawable(t.isDark)
                (child.getChildAt(0) as? WaveFillView)?.themeColor = t.accent
                val row = child.getChildAt(1) as LinearLayout
                (row.getChildAt(0) as TextView).applyContrastingText(t.isDark)
                (row.getChildAt(1) as ImageButton).setColorFilter(if (t.isDark) Color.WHITE else Color.BLACK)
            } 
        }
        for (i in 0 until songsListVerticalContainer.childCount) { 
            val row = songsListVerticalContainer.getChildAt(i) as? LinearLayout ?: continue
            row.background = getGlassDrawable(t.isDark)
            (row.getChildAt(1) as? TextView)?.applyContrastingText(t.isDark) 
        }
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

    private val queueThumbMemoryCache = java.util.concurrent.ConcurrentHashMap<String, android.graphics.Bitmap>()
    private var queuePreloadJob: kotlinx.coroutines.Job? = null

    private fun extractStreamThumbInfo(entry: String): Pair<String, String>? {
        if (!entry.startsWith("LIVE_STREAM|") && !entry.startsWith("PENDING_YT|")) return null
        val parts = entry.split("|")
        val ytUrl = parts.getOrNull(4).orEmpty()
        val vid = when {
            entry.startsWith("PENDING_YT|") -> parts.getOrNull(1).orEmpty()
            else -> ytUrl.substringAfter("v=", "").substringBefore("&")
        }
        val rawThumb = parts.getOrNull(3).orEmpty()
        val thumbUrl = when {
            rawThumb.startsWith("http://") || rawThumb.startsWith("https://") -> rawThumb
            vid.isNotBlank() -> "https://i.ytimg.com/vi/$vid/hqdefault.jpg"
            else -> ""
        }
        val key = vid.ifBlank { thumbUrl.hashCode().toString() }
        if (thumbUrl.isBlank()) return null
        return Pair(key, thumbUrl)
    }

    private suspend fun getOrFetchQueueThumbnail(entry: String): android.graphics.Bitmap? = withContext(Dispatchers.IO) {
        val (key, thumbUrl) = extractStreamThumbInfo(entry) ?: return@withContext null
        queueThumbMemoryCache[key]?.let { return@withContext it }

        val tempDir = File(cacheDir, "queue_thumbs").apply { mkdirs() }
        val tempFile = File(tempDir, "$key.jpg")

        if (tempFile.exists() && tempFile.length() > 0L) {
            android.graphics.BitmapFactory.decodeFile(tempFile.absolutePath)?.let { bmp ->
                queueThumbMemoryCache[key] = bmp
                return@withContext bmp
            }
        }

        try {
            val conn = (java.net.URL(thumbUrl).openConnection() as java.net.HttpURLConnection).apply {
                connectTimeout = 5000
                readTimeout = 5000
                doInput = true
                connect()
            }
            val bytes = conn.inputStream.use { it.readBytes() }
            conn.disconnect()
            if (bytes.isNotEmpty()) {
                tempFile.writeBytes(bytes)
                val bmp = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                if (bmp != null) {
                    queueThumbMemoryCache[key] = bmp
                    return@withContext bmp
                }
            }
        } catch (_: Exception) { }
        null
    }

    private fun precargarCaratulasCola(lista: List<String>) {
        val pendientes = lista.mapIndexedNotNull { idx, item ->
            val info = extractStreamThumbInfo(item)
            if (info != null && !queueThumbMemoryCache.containsKey(info.first)) idx to item else null
        }
        if (pendientes.isEmpty()) return

        queuePreloadJob?.cancel()
        queuePreloadJob = lifecycleScope.launch(Dispatchers.IO) {
            val semaforo = kotlinx.coroutines.sync.Semaphore(4)
            pendientes.forEach { (idx, item) ->
                launch {
                    semaforo.acquire()
                    try {
                        val bmp = getOrFetchQueueThumbnail(item)
                        if (bmp != null) {
                            withContext(Dispatchers.Main) {
                                if (::queueAdapter.isInitialized && idx < playlistActual.size && playlistActual[idx] == item) {
                                    queueAdapter.notifyItemChanged(idx)
                                }
                            }
                        }
                    } finally {
                        semaforo.release()
                    }
                }
            }
        }
    }

    private fun resetBarraReproduccionCambioTema() {
        try { wavySeekBar.progress = 0f; wavySeekBar.isWaving = false } catch (_: Exception) { }
        try { txtCurrentTime.text = "00:00" } catch (_: Exception) { }
        try { txtTotalTime.text = "00:00" } catch (_: Exception) { }
        try { btnPlayPause.setImageResource(android.R.drawable.ic_media_play) } catch (_: Exception) { }
    }

    private fun loadCoverListAsync(rutaArchivo: String, imageView: ImageView) {
        if (rutaArchivo.startsWith("LIVE_STREAM|") || rutaArchivo.startsWith("PENDING_YT|")) {
            val info = extractStreamThumbInfo(rutaArchivo)
            val memBmp = info?.first?.let { queueThumbMemoryCache[it] }
            if (memBmp != null) {
                imageView.setImageBitmap(memBmp)
                return
            }
            imageView.tag = rutaArchivo
            lifecycleScope.launch {
                val bmp = getOrFetchQueueThumbnail(rutaArchivo)
                if (bmp != null && imageView.tag == rutaArchivo) {
                    imageView.setImageBitmap(bmp)
                }
            }
            return
        }

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
        tabScrollView = SwipePagingScrollView(this).apply { 
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT).apply { setMargins(0,0,0,220) }
            overScrollMode = View.OVER_SCROLL_NEVER
            isVerticalScrollBarEnabled = false; isHorizontalScrollBarEnabled = false 
            onPageSelected = { page ->
                actualizarNavActivo(page)
                if (page != 0 && hasPendingFullUiSync) {
                    updateUIColors(currentTheme)
                } else if (page == 2) {
                    queueAdapter.notifyDataSetChanged()
                }
            }
        }
        pagesContainer = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val screenWidth = resources.displayMetrics.widthPixels; val lpPage = LinearLayout.LayoutParams(screenWidth, LinearLayout.LayoutParams.MATCH_PARENT)

        // --- HOME (FYP INCORPORADO) ---
        sectionHome = ScrollView(this).apply { layoutParams = lpPage; isFillViewport = true; overScrollMode = View.OVER_SCROLL_NEVER
            isVerticalScrollBarEnabled = false; isVerticalScrollBarEnabled = false }
        val homeContent = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL; setPadding(40, 140, 40, 40) }
        
        txtPlayingFrom = TextView(this).apply { text = "REPRODUCIENDO"; textSize = 14f; typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.CENTER; setPadding(0, 0, 0, 20) }
        homeContent.addView(txtPlayingFrom)

        mediaStageContainer = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 600).apply {
                setMargins(0, 0, 0, 20)
            }
        }

        imgVinyl = VinylDiskView(this).apply {
            layoutParams = FrameLayout.LayoutParams(600, 600, Gravity.CENTER)
            setOnClickListener { toggleVinylTvMode() }
        }

        tvContainer = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, 520, Gravity.CENTER)
            visibility = View.GONE
            elevation = 12f
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#141414"))
                cornerRadius = 38f
                setStroke(4, Color.parseColor("#33FFFFFF"))
            }
            clipToOutline = true
            outlineProvider = ViewOutlineProvider.BACKGROUND
            setOnClickListener { toggleVinylTvMode() }
        }

        tvTextureView = TextureView(this).apply {
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                    videoSurface?.release()
                    videoSurface = Surface(surface)
                    try {
                        videoMediaPlayer?.setSurface(videoSurface)
                    } catch (_: Exception) {}
                    if (isTvMode && !isVideoPrepared) {
                        loadVideoForCurrentTrack()
                    }
                }

                override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
                    adjustVideoAspectRatio(width, height)
                }

                override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                    liveThemeAnimator?.cancel()
                    liveVideoThemeController?.stopAndRestore(currentTheme.accent, currentTheme.w2c1)
                    try { videoMediaPlayer?.setSurface(null) } catch (_: Exception) {}
                    videoSurface?.release()
                    videoSurface = null
                    return true
                }

                override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
            }
        }

        tvStaticView = TvStaticView(this).apply {
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            visibility = View.GONE
        }

        tvOverlayLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(40, 40, 40, 40)
            setBackgroundColor(Color.parseColor("#80000000"))
            visibility = View.GONE
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        }

        txtTvStatus = TextView(this).apply {
            text = "SIN SEÑAL"
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            setShadowLayer(6f, 0f, 0f, Color.BLACK)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 20)
        }

        btnDownloadClip = Button(this).apply {
            text = "Descargar videoclip"
            isAllCaps = false
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            setPadding(36, 18, 36, 18)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#CC1E1E1E"))
                cornerRadius = 40f
                setStroke(2, Color.parseColor("#66FFFFFF"))
            }
            setOnClickListener { triggerClipDownloadFromTv() }
        }

        tvOverlayLayout.addView(txtTvStatus)
        tvOverlayLayout.addView(btnDownloadClip)

        txtTvBadge = TextView(this).apply {
            text = "TV • TOCA PARA VINILO"
            textSize = 10f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            setPadding(22, 10, 22, 10)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#88000000"))
                cornerRadius = 24f
            }
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.TOP or Gravity.END
            ).apply { setMargins(0, 18, 18, 0) }
        }

        tvContainer.addView(tvTextureView)
        tvContainer.addView(tvStaticView)
        tvContainer.addView(tvOverlayLayout)
        tvContainer.addView(txtTvBadge)

        mediaStageContainer.addView(imgVinyl)
        mediaStageContainer.addView(tvContainer)
        homeContent.addView(mediaStageContainer)

        txtTitle = TextView(this).apply { text = "Selecciona una pista"; textSize = 20f; typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.START; layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT) }
        txtArtist = TextView(this).apply { text = "Lista de Reproduccion"; textSize = 14f; gravity = Gravity.START; setPadding(0, 5, 0, 20); layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT) }
        wavySeekBar = WavySeekBar(this).apply { layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 50).apply { setMargins(0, 5, 0, 5) } }
        wavySeekBar.onProgressChanged = { prog, fromUser ->
            if (fromUser) {
                val dur = musicService?.getDuration() ?: 0
                if (dur > 0) {
                    val targetMs = (prog * dur).toInt()
                    musicService?.seekTo(targetMs)
                    if (isVideoPrepared) {
                        try {
                            videoMediaPlayer?.let { vp ->
                                if (vp.duration > 0) vp.seekTo(targetMs.coerceAtMost(vp.duration))
                            }
                        } catch (_: Exception) {}
                    }
                }
            }
        }
        val timeLayout = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT); setPadding(0, 0, 0, 10) }
        txtCurrentTime = TextView(this).apply { text = "00:00"; textSize = 11f }; txtTotalTime = TextView(this).apply { text = "00:00"; textSize = 11f; gravity = Gravity.END; layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f) }
        timeLayout.addView(txtCurrentTime); timeLayout.addView(txtTotalTime)

        val controlsRow1 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER; setPadding(0, 0, 0, 10) }
        btnPrev = ImageButton(this).apply { setImageResource(android.R.drawable.ic_media_previous); layoutParams = LinearLayout.LayoutParams(130, 130).apply { setMargins(15, 0, 15, 0) }; setOnClickListener { reproducirAnterior() } }
        btnPlayPause = ImageButton(this).apply { setImageResource(android.R.drawable.ic_media_play); layoutParams = LinearLayout.LayoutParams(160, 150).apply { setMargins(15, 0, 15, 0) }; setOnClickListener { togglePlayPause() } }
        btnNext = ImageButton(this).apply { setImageResource(android.R.drawable.ic_media_next); layoutParams = LinearLayout.LayoutParams(130, 130).apply { setMargins(15, 0, 15, 0) }; setOnClickListener { reproducirSiguiente() } }
        controlsRow1.addView(btnPrev); controlsRow1.addView(btnPlayPause); controlsRow1.addView(btnNext)

        val controlsRow2 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER; setPadding(0, 5, 0, 10) }
        btnShuffle = Button(this).apply { text = "MEZCLAR"; textSize = 11f; setBackgroundColor(Color.TRANSPARENT); layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f); setOnClickListener { if (playlistActual.isNotEmpty()) { resetLoopState(); musicService?.shuffleQueue() ?: run { musicService?.shuffleQueue() ?: run { playlistActual.shuffle(); currentIndex = 0; reproducirCancionActual() } }; val pulse = ValueAnimator.ofArgb(currentTheme.accentLight, Color.TRANSPARENT); pulse.duration = 400; pulse.addUpdateListener { btnShuffle.setBackgroundColor(it.animatedValue as Int) }; pulse.start() } } }
        btnFavRadial = RadialLikeButton(this).apply { layoutParams = LinearLayout.LayoutParams(0, 120, 1f).apply { setMargins(10,0,10,0) }; onClickAction = { toggleFavCurrentSong() } }
        val wrapLoop = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER; layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f) }
        btnLoop = Button(this).apply { text = "REPETIR"; textSize = 11f; setBackgroundColor(Color.TRANSPARENT); setPadding(0,0,0,0) }
        dotLoop = View(this).apply { layoutParams = LinearLayout.LayoutParams(15, 15).apply { setMargins(5,0,0,0) }; background = GradientDrawable().apply { cornerRadius=15f; setColor(Color.TRANSPARENT) } }
        wrapLoop.addView(btnLoop); wrapLoop.addView(dotLoop)
        btnLoop.setOnClickListener { isLooping = !isLooping; musicService?.isLooping = isLooping; videoMediaPlayer?.isLooping = isLooping; (dotLoop.background as GradientDrawable).setColor(if (isLooping) currentTheme.accent else Color.TRANSPARENT); dotLoop.invalidate() }

        controlsRow2.addView(btnShuffle); controlsRow2.addView(btnFavRadial); controlsRow2.addView(wrapLoop)
        
        homeContent.addView(txtTitle); homeContent.addView(txtArtist); homeContent.addView(wavySeekBar); homeContent.addView(timeLayout); homeContent.addView(controlsRow1); homeContent.addView(controlsRow2)
        
        val divider = View(this).apply { layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 2).apply { setMargins(0, 80, 0, 40) }; setBackgroundColor(Color.parseColor("#33888888")) }
        btnAddToPlaylist = Button(this).apply { 
            text = "Agregar canción a Playlist"
            isAllCaps = false
            setOnClickListener { 
                if (playlistActual.isNotEmpty() && currentIndex >= 0 && currentIndex < playlistActual.size) {
                    showAddToPlaylistDialog(playlistActual[currentIndex])
                } else {
                    android.widget.Toast.makeText(this@MainActivity, "Reproduce una canción primero", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 30, 0, 30)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 20, 0, 20) }
        }
        txtFypHeader = TextView(this).apply { 
            text = "Canciones recomendadas"
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 20, 0, 15)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        fypContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT) }
        homeContent.addView(divider); homeContent.addView(btnAddToPlaylist); homeContent.addView(txtFypHeader); homeContent.addView(fypContainer)
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
                            val txtName = TextView(this@MainActivity).apply { text = songPath.substringAfterLast("/").substringBeforeLast(".").replace("_", " "); textSize = 15f; applyContrastingText(currentTheme.isDark); layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f) }
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
        queueAdapter = QueueAdapter(playlistActual, { currentIndex }, { currentTheme }, { pos -> resetLoopState(); resetBarraReproduccionCambioTema(); currentIndex = pos; reproducirCancionActual(); queueAdapter.notifyDataSetChanged() }, { vh -> itemTouchHelper.startDrag(vh) }, { isDark -> getGlassDrawable(isDark) }, { path, imgView -> loadCoverListAsync(path, imgView) }, { lista -> precargarCaratulasCola(lista) })
        recyclerViewQueue.adapter = queueAdapter
        val callback = object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {
            override fun onMove(r: RecyclerView, vh: RecyclerView.ViewHolder, tgt: RecyclerView.ViewHolder): Boolean {
                val fP = vh.adapterPosition
                val tP = tgt.adapterPosition
                if (fP == currentIndex) currentIndex = tP
                else if (fP < currentIndex && tP >= currentIndex) currentIndex--
                else if (fP > currentIndex && tP <= currentIndex) currentIndex++
                if (musicService != null) {
                    musicService?.moveQueueItem(fP, tP)
                    queueAdapter.notifyItemMoved(fP, tP)
                } else {
                    queueAdapter.onItemMove(fP, tP)
                }
                return true
            }
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
        btnDownloadPlaylist = Button(this).apply {
            text = "Descargar Playlist por Enlace"
            isAllCaps = false
            textSize = 13f
            setPadding(30, 20, 30, 20)
            setOnClickListener { mostrarDialogoDescargaPlaylist() }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, 10) }
        }
        txtDownloadQueueStatus = TextView(this).apply {
            textSize = 13f
            gravity = Gravity.CENTER
            setPadding(30, 20, 30, 20)
            visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, 10) }
        }
        resultContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 20, 0, 40) }; visibility = View.GONE }
        sectionSearch.addView(txtSearchHeader)
        sectionSearch.addView(inputYtSearch)
        sectionSearch.addView(btnDoSearch)
        sectionSearch.addView(btnDownloadPlaylist)
        sectionSearch.addView(txtDownloadQueueStatus)
        val searchScroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
            overScrollMode = View.OVER_SCROLL_NEVER
            isVerticalScrollBarEnabled = false
        }
        searchScroll.addView(resultContainer)
        sectionSearch.addView(searchScroll)

        btnDoSearch.setOnClickListener {
            val q = inputYtSearch.text.toString().trim()
            if (q.isEmpty()) return@setOnClickListener

            if (q.contains("list=", ignoreCase = true) || q.contains("playlist", ignoreCase = true)) {
                mostrarDialogoDescargaPlaylist(q)
                return@setOnClickListener
            }

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
                            applyContrastingText(currentTheme.isDark)
                            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(0,0,15,0) }
                            maxLines = 2
                            ellipsize = android.text.TextUtils.TruncateAt.END 
                        }
                        val btnDl = ImageButton(this@MainActivity).apply { 
                            setImageResource(android.R.drawable.stat_sys_download)
                            background = null
                            setPadding(20,0,20,0)
                            setColorFilter(if (currentTheme.isDark) Color.WHITE else Color.BLACK) 
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
                                    autoDownloadMatchingVideo(ytUrl, destFolder)
                                    android.widget.Toast.makeText(this@MainActivity, "¡Descarga completa!", android.widget.Toast.LENGTH_SHORT).show()
                                } else {
                                    android.widget.Toast.makeText(this@MainActivity, "Error en descarga", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        row.setOnClickListener {
                            val ytId = data["id"] ?: ""
                            if (ytId.isNotEmpty()) {
                                reproducirDesdeYoutube(
                                    vid = ytId,
                                    vtitle = titleText,
                                    vthumb = thumbUrl,
                                    origen = "Búsqueda YouTube",
                                    irAInicio = true
                                )
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
        switchCrossfade = Switch(this).apply { text = "Fundido Suave (Fade Out)"; textSize = 16f; isChecked = isCrossfadeEnabled; setPadding(40,40,40,40); setOnCheckedChangeListener { _, isChecked -> isCrossfadeEnabled = isChecked; musicService?.isCrossfadeEnabled = isChecked; musicService?.isCrossfadeEnabled = isChecked; prefs.edit().putBoolean("CROSSFADE", isChecked).apply() } }
        btnSleepTimer = Button(this).apply { text = "Temporizador de Apagado"; setOnClickListener { mostrarDialogoSleepTimer() }; layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 20, 0, 20) } }

        switchDynamicTheme = Switch(this).apply {
            text = "Tema Dinámico (desde carátula)"
            textSize = 16f
            isChecked = isDynamicThemeEnabled
            setPadding(40, 40, 40, 40)
            setOnCheckedChangeListener { _, isChecked ->
                isDynamicThemeEnabled = isChecked
                if (!isChecked) {
                    liveThemeAnimator?.cancel()
                    liveVideoThemeController?.stopAndRestore(currentTheme.accent, currentTheme.w2c1)
                    val savedName = prefs.getString("APP_THEME", "Blanco con rojo") ?: "Blanco con rojo"
                    animateThemeTransition(THEMES[savedName] ?: THEMES["Blanco con rojo"]!!)
                } else {
                    if (isTvMode && isVideoPrepared && tvTextureView.isAvailable) {
                        liveVideoThemeController?.setInitialColors(currentTheme.accent, currentTheme.w2c1)
                        liveVideoThemeController?.startSampling(tvTextureView)
                    } else {
                        imgVinyl.bitmap?.let { extraerTemaDinamicoDeCaratula(it) }
                    }
                }
            }
        }

        btnOpcionesDescarga = Button(this).apply {
            text = "Opciones de descargas"
            setOnClickListener { mostrarDialogoOpcionesDescarga() }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 10, 0, 20) }
        }

        sectionSettings.addView(txtSettingsHeader); sectionSettings.addView(btnCambiarTema); sectionSettings.addView(txtHeaderPlayback); sectionSettings.addView(switchCrossfade); sectionSettings.addView(switchDynamicTheme); sectionSettings.addView(btnOpcionesDescarga); sectionSettings.addView(btnSleepTimer)
        
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

    private fun mostrarDialogoOpcionesDescarga() {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 30, 50, 20)
        }

        val swStreamOnly = Switch(this).apply {
            text = "Reproducir sin descargar"
            textSize = 16f
            isChecked = isStreamOnlyEnabled
            setPadding(10, 25, 10, 10)
            setOnCheckedChangeListener { _, isChecked ->
                isStreamOnlyEnabled = isChecked
                prefs.edit().putBoolean("STREAM_ONLY_MODE", isChecked).apply()
            }
        }

        val descStreamOnly = TextView(this).apply {
            text = "Activado: reproduce solo por internet sin guardar archivos. Desactivado: reproduce al instante y descarga en segundo plano."
            textSize = 12f
            alpha = 0.75f
            setPadding(10, 0, 10, 25)
        }

        val swAutoVideo = Switch(this).apply {
            text = "Descargar videoclip automáticamente"
            textSize = 16f
            isChecked = isAutoDownloadVideoEnabled
            setPadding(10, 20, 10, 20)
            setOnCheckedChangeListener { _, isChecked ->
                isAutoDownloadVideoEnabled = isChecked
                prefs.edit().putBoolean("AUTO_DOWNLOAD_VIDEO", isChecked).apply()
            }
        }

        container.addView(swStreamOnly)
        container.addView(descStreamOnly)
        container.addView(swAutoVideo)

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Opciones de descargas")
            .setView(container)
            .setPositiveButton("Listo", null)
            .show()
    }

    private fun getCachedStreams(ytUrl: String): CachedStream? {
        val entry = streamCache[ytUrl] ?: return null
        val tresHorasMs = 3L * 60L * 60L * 1000L
        return if (System.currentTimeMillis() - entry.timestampMs < tresHorasMs) {
            entry
        } else {
            streamCache.remove(ytUrl)
            null
        }
    }

    private suspend fun resolveDirectAudioStreamUrl(ytUrl: String): String? = withContext(Dispatchers.IO) {
        getCachedStreams(ytUrl)?.audioUrl?.let { return@withContext it }

        try {
            val req = YoutubeDLRequest(ytUrl).apply {
                addOption("--extractor-args", "youtube:player_client=android,web")
                addOption("--no-playlist")
                addOption("--no-warnings")
                addOption("-f", "bestaudio[ext=m4a]/bestaudio/best")
                addOption("--get-url")
            }
            val response = YoutubeDL.getInstance().execute(req, null, null)
            val audioUrl = response.out
                ?.lineSequence()
                ?.map { it.trim() }
                ?.firstOrNull { it.startsWith("http://") || it.startsWith("https://") }

            if (audioUrl != null) {
                val prev = getCachedStreams(ytUrl)
                streamCache[ytUrl] = CachedStream(
                    audioUrl = audioUrl,
                    videoUrl = prev?.videoUrl,
                    timestampMs = System.currentTimeMillis()
                )
            }
            audioUrl
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun resolveDirectVideoStreamUrl(ytUrl: String): String? = withContext(Dispatchers.IO) {
        getCachedStreams(ytUrl)?.videoUrl?.let { return@withContext it }

        try {
            val req = YoutubeDLRequest(ytUrl).apply {
                addOption("--extractor-args", "youtube:player_client=android,web")
                addOption("--no-playlist")
                addOption("--no-warnings")
                addOption("-f", "bestvideo[height<=480][ext=mp4]/bestvideo[height<=480]/best[height<=480]/best")
                addOption("--get-url")
            }
            val response = YoutubeDL.getInstance().execute(req, null, null)
            val videoUrl = response.out
                ?.lineSequence()
                ?.map { it.trim() }
                ?.firstOrNull { it.startsWith("http://") || it.startsWith("https://") }

            if (videoUrl != null) {
                val prev = getCachedStreams(ytUrl)
                streamCache[ytUrl] = CachedStream(
                    audioUrl = prev?.audioUrl,
                    videoUrl = videoUrl,
                    timestampMs = System.currentTimeMillis()
                )
            }
            videoUrl
        } catch (_: Exception) {
            null
        }
    }

    

    private fun reproducirDesdeYoutube(
        vid: String,
        vtitle: String,
        vthumb: String,
        origen: String,
        irAInicio: Boolean
    ) {
        if (vid.isEmpty()) return
        val ytUrl = "https://www.youtube.com/watch?v=$vid"
        val soloInternet = isStreamOnlyEnabled

        Toast.makeText(
            this,
            if (soloInternet) "Cargando audio en streaming..." else "Reproduciendo y descargando en segundo plano...",
            Toast.LENGTH_SHORT
        ).show()

        lifecycleScope.launch {
            val streamUrl = resolveDirectAudioStreamUrl(ytUrl)
            if (streamUrl != null) {
                lifecycleScope.launch { resolveDirectVideoStreamUrl(ytUrl) }
            }
            var liveEntry: String? = null

            if (streamUrl != null) {
                val safeTitle = vtitle.replace("|", "-").trim()
                liveEntry = "LIVE_STREAM|$streamUrl|$safeTitle|$vthumb|$ytUrl"
                resetLoopState()
                nombrePlaylistActual = origen
                val colaInicial = mutableListOf(liveEntry)
                for (rec in viewModel.fypRecommendations.value) {
                    val rId = rec["id"].orEmpty()
                    val rTitle = rec["title"].orEmpty().replace("|", "-").trim()
                    val rThumb = rec["thumbnail"].orEmpty()
                    if (rId.isNotBlank() && rId != vid && rTitle.isNotBlank()) {
                        colaInicial.add("PENDING_YT|$rId|$rTitle|$rThumb|https://www.youtube.com/watch?v=$rId")
                    }
                }
                playlistActual = colaInicial
                currentIndex = 0
                if (::queueAdapter.isInitialized) queueAdapter.updateList(playlistActual) else precargarCaratulasCola(playlistActual)
                reproducirCancionActual()
                if (irAInicio) {
                    cambiarPestana(0)
                } else {
                    sectionHome.smoothScrollTo(0, 0)
                }
            } else if (soloInternet) {
                Toast.makeText(this@MainActivity, "No se pudo obtener el audio en streaming", Toast.LENGTH_SHORT).show()
                return@launch
            }

            if (!soloInternet) {
                val destFolder = File(getExternalFilesDir(null), "Base-Musica/Recien agregados").apply { mkdirs() }
                viewModel.loadLocalMusic()
                viewModel.downloadSingleTrack(ytUrl, "Recien agregados") { success ->
                    if (success) {
                        autoDownloadMatchingVideo(ytUrl, destFolder)
                        viewModel.loadLocalMusic()

                        val audioFiles = destFolder.listFiles { file ->
                            file.isFile && (file.extension.equals("m4a", true) ||
                                file.extension.equals("mp3", true) ||
                                file.extension.equals("webm", true))
                        }?.sortedByDescending { it.lastModified() }

                        val downloadedFile = audioFiles?.firstOrNull()
                        if (downloadedFile != null) {
                            if (liveEntry == null) {
                                resetLoopState()
                                nombrePlaylistActual = "Recien agregados"
                                playlistActual = audioFiles.map { it.absolutePath }.toMutableList()
                                currentIndex = playlistActual.indexOf(downloadedFile.absolutePath).coerceAtLeast(0)
                                if (::queueAdapter.isInitialized) queueAdapter.updateList(playlistActual) else precargarCaratulasCola(playlistActual)
                                reproducirCancionActual()
                                if (irAInicio) cambiarPestana(0) else sectionHome.smoothScrollTo(0, 0)
                            } else {
                                val idxInQueue = playlistActual.indexOf(liveEntry)
                                if (idxInQueue >= 0) {
                                    playlistActual[idxInQueue] = downloadedFile.absolutePath
                                    if (::queueAdapter.isInitialized) queueAdapter.notifyItemChanged(idxInQueue)
                                }
                                Toast.makeText(this@MainActivity, "Descarga guardada en 'Recien agregados'", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        Toast.makeText(this@MainActivity, "Error al descargar canción", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun autoDownloadMatchingVideo(ytUrl: String, destFolder: File) {
        if (!isAutoDownloadVideoEnabled || ytUrl.isEmpty()) return

        val latestAudio = destFolder.listFiles { file ->
            file.isFile && (file.extension.equals("m4a", true) ||
                            file.extension.equals("mp3", true) ||
                            file.extension.equals("webm", true))
        }?.maxByOrNull { it.lastModified() } ?: return

        val exactBaseName = latestAudio.nameWithoutExtension
        val videosDir = File(getExternalFilesDir(null), "Base-Videos").apply { mkdirs() }
        val outputFile = File(videosDir, "$exactBaseName.mp4")

        if (outputFile.exists() && outputFile.length() > 0L) return
        if (!activeVideoDownloads.add(exactBaseName)) return

        if (isTvMode) {
            handler.post { loadVideoForCurrentTrack() }
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val req = YoutubeDLRequest(ytUrl).apply {
                    addOption("--extractor-args", "youtube:player_client=android,web")
                    addOption("--no-playlist")
                    addOption("-f", "bestvideo[height<=480][ext=mp4]+bestaudio[ext=m4a]/best[height<=480][ext=mp4]/best[ext=mp4]")
                    addOption("--merge-output-format", "mp4")
                    addOption("--concurrent-fragments", "4")
                    addOption("-o", outputFile.absolutePath)
                }
                YoutubeDL.getInstance().execute(req, null, null)
            } catch (_: Exception) {
                if (outputFile.exists() && outputFile.length() == 0L) outputFile.delete()
            } finally {
                activeVideoDownloads.remove(exactBaseName)
                withContext(Dispatchers.Main) {
                    val currentPlayingBase = playlistActual.getOrNull(currentIndex)
                        ?.takeIf { !it.startsWith("LIVE_STREAM|") }
                        ?.let { File(it).nameWithoutExtension }

                    if (isTvMode && currentPlayingBase == exactBaseName && outputFile.exists()) {
                        loadVideoForCurrentTrack()
                    }
                }
            }
        }
    }

    private fun toggleVinylTvMode() {
        isTvMode = !isTvMode
        if (isTvMode) {
            imgVinyl.animate().alpha(0f).scaleX(0.92f).scaleY(0.92f).setDuration(180).withEndAction {
                imgVinyl.visibility = View.GONE
                imgVinyl.alpha = 1f
                imgVinyl.scaleX = 1f
                imgVinyl.scaleY = 1f
            }.start()

            tvContainer.alpha = 0f
            tvContainer.visibility = View.VISIBLE
            tvContainer.animate().alpha(1f).setDuration(220).start()
            loadVideoForCurrentTrack()
        } else {
            releaseVideoPlayer()
            tvContainer.animate().alpha(0f).setDuration(180).withEndAction {
                tvContainer.visibility = View.GONE
                tvContainer.alpha = 1f
            }.start()

            imgVinyl.alpha = 0f
            imgVinyl.visibility = View.VISIBLE
            imgVinyl.animate().alpha(1f).setDuration(220).start()

            if (isDynamicThemeEnabled) {
                imgVinyl.bitmap?.let { extraerTemaDinamicoDeCaratula(it) }
            }
        }
    }

    private fun findLocalVideoForTrack(songPath: String): File? {
        val videoExts = setOf("mp4", "mkv", "mov", "3gp")
        val videosDir = File(getExternalFilesDir(null), "Base-Videos")

        if (songPath.startsWith("LIVE_STREAM|")) {
            val rawTitle = songPath.split("|").getOrNull(2).orEmpty()
            val safeName = rawTitle.replace(Regex("[^a-zA-Z0-9áéíóúÁÉÍÓÚñÑ _-]"), "").trim()
            if (safeName.isNotEmpty() && videosDir.exists()) {
                videosDir.listFiles()?.firstOrNull {
                    it.isFile && it.nameWithoutExtension.equals(safeName, ignoreCase = true) && it.extension.lowercase() in videoExts
                }?.let { return it }
            }
            return null
        }

        val audioFile = File(songPath)
        val baseName = audioFile.nameWithoutExtension
        val parentDir = audioFile.parentFile

        parentDir?.listFiles()?.firstOrNull {
            it.isFile &&
                it.absolutePath != audioFile.absolutePath &&
                it.nameWithoutExtension.equals(baseName, ignoreCase = true) &&
                (it.extension.lowercase() in videoExts || it.extension.equals("webm", ignoreCase = true))
        }?.let { return it }

        if (videosDir.exists()) {
            videosDir.listFiles()?.firstOrNull {
                it.isFile &&
                    it.nameWithoutExtension.equals(baseName, ignoreCase = true) &&
                    (it.extension.lowercase() in videoExts || it.extension.equals("webm", ignoreCase = true))
            }?.let { return it }
        }

        return null
    }

    private fun loadVideoForCurrentTrack() {
        releaseVideoPlayer()
        if (!isTvMode) return

        tvStaticView.showStatic()
        if (playlistActual.isEmpty() || currentIndex !in playlistActual.indices) {
            tvOverlayLayout.visibility = View.VISIBLE
            txtTvStatus.text = "REPRODUCE UNA PISTA"
            btnDownloadClip.visibility = View.GONE
            return
        }

        val currentPath = playlistActual[currentIndex]
        val videoFile = findLocalVideoForTrack(currentPath)
        if (videoFile != null) {
            iniciarReproduccionVideoEnTv(videoFile.absolutePath)
            return
        }

        if (currentPath.startsWith("LIVE_STREAM|")) {
            val ytUrl = currentPath.split("|").getOrNull(4).orEmpty()
            if (ytUrl.isNotEmpty()) {
                val cachedVideoUrl = getCachedStreams(ytUrl)?.videoUrl
                if (cachedVideoUrl != null) {
                    iniciarReproduccionVideoEnTv(cachedVideoUrl)
                    return
                }

                tvOverlayLayout.visibility = View.VISIBLE
                txtTvStatus.text = "CARGANDO VIDEOCLIP..."
                btnDownloadClip.visibility = View.GONE

                videoStreamJob?.cancel()
                videoStreamJob = lifecycleScope.launch {
                    val streamVideoUrl = resolveDirectVideoStreamUrl(ytUrl)
                    if (streamVideoUrl != null) {
                        if (isTvMode && playlistActual.getOrNull(currentIndex) == currentPath) {
                            iniciarReproduccionVideoEnTv(streamVideoUrl)
                        }
                    } else if (isTvMode && playlistActual.getOrNull(currentIndex) == currentPath) {
                        tvOverlayLayout.visibility = View.VISIBLE
                        txtTvStatus.text = "SIN SEÑAL DE VIDEO"
                        btnDownloadClip.visibility = View.VISIBLE
                    }
                }
                return
            }
        }

        val currentBaseName = if (currentPath.startsWith("LIVE_STREAM|")) "" else File(currentPath).nameWithoutExtension
        val isDownloadingThis = isDownloadingClip || (currentBaseName.isNotEmpty() && activeVideoDownloads.contains(currentBaseName))

        tvOverlayLayout.visibility = View.VISIBLE
        txtTvStatus.text = if (isDownloadingThis) "DESCARGANDO VIDEOCLIP..." else "SIN SEÑAL DE VIDEO"
        btnDownloadClip.visibility = if (isDownloadingThis) View.GONE else View.VISIBLE
    }

    private fun iniciarReproduccionVideoEnTv(videoSource: String) {
        if (!isTvMode) return
        tvOverlayLayout.visibility = View.GONE
        if (!tvTextureView.isAvailable) return

        try {
            if (videoSurface == null && tvTextureView.surfaceTexture != null) {
                videoSurface = Surface(tvTextureView.surfaceTexture)
            }
            videoMediaPlayer?.release()
            videoMediaPlayer = MediaPlayer().apply {
                setSurface(videoSurface)
                setVolume(0f, 0f)
                isLooping = this@MainActivity.isLooping
                setDataSource(videoSource)
                setOnVideoSizeChangedListener { _, _, _ ->
                    adjustVideoAspectRatio(tvTextureView.width, tvTextureView.height)
                }
                setOnPreparedListener { vp ->
                    isVideoPrepared = true
                    adjustVideoAspectRatio(tvTextureView.width, tvTextureView.height)
                    val audioPos = musicService?.getCurrentPosition() ?: 0
                    if (audioPos > 0 && vp.duration > 0) {
                        vp.seekTo(audioPos.coerceAtMost((vp.duration - 200).coerceAtLeast(0)))
                    }
                    if (musicService?.isPlaying() == true) {
                        vp.start()
                    }
                    tvStaticView.fadeOutStatic(380L)
                    if (isDynamicThemeEnabled) {
                        liveVideoThemeController?.setInitialColors(currentTheme.accent, currentTheme.w2c1)
                        liveVideoThemeController?.startSampling(tvTextureView)
                    }
                }
                setOnErrorListener { _, _, _ ->
                    isVideoPrepared = false
                    tvStaticView.showStatic()
                    tvOverlayLayout.visibility = View.VISIBLE
                    txtTvStatus.text = "ERROR AL REPRODUCIR CLIP"
                    btnDownloadClip.visibility = View.VISIBLE
                    true
                }
                prepareAsync()
            }
        } catch (_: Exception) {
            tvStaticView.showStatic()
            tvOverlayLayout.visibility = View.VISIBLE
        }
    }

    private fun adjustVideoAspectRatio(viewWidth: Int, viewHeight: Int) {
        val vp = videoMediaPlayer ?: return
        if (viewWidth <= 0 || viewHeight <= 0) return
        val videoWidth = try { vp.videoWidth } catch (_: Exception) { 0 }
        val videoHeight = try { vp.videoHeight } catch (_: Exception) { 0 }
        if (videoWidth <= 0 || videoHeight <= 0) return

        val viewRatio = viewWidth.toFloat() / viewHeight.toFloat()
        val videoRatio = videoWidth.toFloat() / videoHeight.toFloat()

        var scaleX = 1f
        var scaleY = 1f
        if (videoRatio > viewRatio) {
            scaleX = videoRatio / viewRatio
        } else {
            scaleY = viewRatio / videoRatio
        }

        val matrix = Matrix().apply {
            setScale(scaleX, scaleY, viewWidth / 2f, viewHeight / 2f)
        }
        tvTextureView.setTransform(matrix)
    }

    private fun triggerClipDownloadFromTv() {
        if (isDownloadingClip || playlistActual.isEmpty() || currentIndex !in playlistActual.indices) return
        val currentPath = playlistActual[currentIndex]
        val title = txtTitle.text.toString()

        isDownloadingClip = true
        btnDownloadClip.visibility = View.GONE
        txtTvStatus.text = "BUSCANDO Y DESCARGANDO (0%)..."

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val videosDir = File(getExternalFilesDir(null), "Base-Videos").apply { mkdirs() }
                val baseName = if (currentPath.startsWith("LIVE_STREAM|")) {
                    title.replace(Regex("[^a-zA-Z0-9áéíóúÁÉÍÓÚñÑ _-]"), "").trim().ifEmpty { "clip_stream" }
                } else {
                    File(currentPath).nameWithoutExtension
                }
                val outputFile = File(videosDir, "$baseName.mp4")

                val targetUrl = if (currentPath.startsWith("LIVE_STREAM|")) {
                    currentPath.split("|").getOrNull(4).orEmpty()
                } else {
                    val cleanQuery = title
                        .replace(Regex("\\s*[-–_]\\s*(Topic|VEVO|Official|Oficial|Music|Records|Entertainment|Channel|TV|HD|4K|Audio|Video|Lyrics)(\\s|$)", RegexOption.IGNORE_CASE), "")
                        .replace(Regex("\\(.*?\\)", RegexOption.DOT_MATCHES_ALL), "")
                        .replace(Regex("\\[.*?\\]", RegexOption.DOT_MATCHES_ALL), "")
                        .replace("_", " ")
                        .replace(Regex("\\s{2,}"), " ")
                        .trim()
                    val found = YtDlpRepository(this@MainActivity).searchYoutube("$cleanQuery official video", 1)
                    val vid = found.firstOrNull()?.get("id").orEmpty()
                    if (vid.isNotEmpty()) "https://www.youtube.com/watch?v=$vid" else ""
                }

                if (targetUrl.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        isDownloadingClip = false
                        txtTvStatus.text = "NO SE ENCONTRÓ VIDEOCLIP"
                        btnDownloadClip.visibility = View.VISIBLE
                    }
                    return@launch
                }

                val req = YoutubeDLRequest(targetUrl).apply {
                    addOption("--extractor-args", "youtube:player_client=android,web")
                    addOption("--no-playlist")
                    addOption("-f", "bestvideo[height<=480][ext=mp4]+bestaudio[ext=m4a]/best[height<=480][ext=mp4]/best[ext=mp4]")
                    addOption("--merge-output-format", "mp4")
                    addOption("--concurrent-fragments", "4")
                    addOption("-o", outputFile.absolutePath)
                }

                YoutubeDL.getInstance().execute(req, null) { progress, _, _ ->
                    if (progress >= 0f) {
                        val pct = progress.toInt().coerceIn(0, 100)
                        handler.post {
                            txtTvStatus.text = "DESCARGANDO VIDEOCLIP ($pct%)..."
                        }
                    }
                }

                val success = outputFile.exists() && outputFile.length() > 0L
                withContext(Dispatchers.Main) {
                    isDownloadingClip = false
                    if (success) {
                        Toast.makeText(this@MainActivity, "Videoclip listo", Toast.LENGTH_SHORT).show()
                        if (isTvMode) loadVideoForCurrentTrack()
                    } else {
                        txtTvStatus.text = "NO SE PUDO DESCARGAR"
                        btnDownloadClip.visibility = View.VISIBLE
                    }
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    isDownloadingClip = false
                    txtTvStatus.text = "ERROR AL DESCARGAR CLIP"
                    btnDownloadClip.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun releaseVideoPlayer() {
        videoStreamJob?.cancel()
        videoStreamJob = null
        liveThemeAnimator?.cancel()
        liveVideoThemeController?.stopAndRestore(currentTheme.accent, currentTheme.w2c1)
        isVideoPrepared = false
        try {
            videoMediaPlayer?.stop()
        } catch (_: Exception) {}
        try {
            videoMediaPlayer?.release()
        } catch (_: Exception) {}
        videoMediaPlayer = null
    }

    private fun mostrarDialogoSleepTimer() {
        val options = arrayOf("Desactivar", "15 minutos", "30 minutos", "45 minutos", "60 minutos")
        androidx.appcompat.app.AlertDialog.Builder(this).setTitle("Apagar música en...").setItems(options) { _, w ->
            handler.removeCallbacksAndMessages("SLEEP_TIMER")
            if (w == 0) { Toast.makeText(this, "Temporizador desactivado", Toast.LENGTH_SHORT).show(); return@setItems }
            val mins = w * 15; handler.postDelayed({ if (musicService?.isPlaying() == true) musicService?.pausePlayback(); Toast.makeText(this, "Música apagada", Toast.LENGTH_LONG).show() }, "SLEEP_TIMER", (mins * 60 * 1000).toLong())
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
                val destFolder = File(getExternalFilesDir(null), "Base-Musica/Mi favs").apply { mkdirs() }
                viewModel.downloadSingleTrack(ytUrl, "Mi favs") { success ->
                    if (success) {
                        autoDownloadMatchingVideo(ytUrl, destFolder)
                        viewModel.loadLocalMusic()
                    }
                }
            } else {
                Toast.makeText(this, "Para quitar el Like, bórrala desde la carpeta 'Mi favs'", Toast.LENGTH_LONG).show()
                btnFavRadial.setLikedState(true) 
            }
            return
        }

        val isNowLiked = viewModel.toggleLike(rutaArchivo)
        btnFavRadial.setLikedState(isNowLiked)
    }

    private fun cambiarPestana(index: Int) { 
        tabScrollView.goToPage(index) 
    }

    private fun actualizarNavActivo(activePage: Int) {
        if (!::btnHomeNav.isInitialized) return
        val navButtons = listOf(btnHomeNav, btnPlaylistsNav, btnListNav, btnSearchNav, btnSettingsNav)
        navButtons.forEachIndexed { idx, btn ->
            btn.alpha = if (idx == activePage) 1.0f else 0.55f
        }
    }

    private fun renderizarListaDeCarpetas() {
        playlistFoldersContainer.removeAllViews(); playlistFoldersContainer.addView(btnCrearPlaylist); playlistFoldersContainer.addView(txtFoldersHeader)
        mapPlaylists.keys.forEach { folderName ->
            val folderWrapper = FrameLayout(this).apply { layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 10, 0, 10) }; background = getGlassDrawable(currentTheme.isDark); outlineProvider = ViewOutlineProvider.BACKGROUND; clipToOutline = true; tag = folderName }
            val waveView = WaveFillView(this).apply { themeColor = currentTheme.accent; layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT) }
            val folderRow = LinearLayout(this).apply { layoutParams = LinearLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT); orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(40, 40, 40, 40); setOnClickListener { abrirCarpetaPlaylist(folderName) } }
            val txtFolder = TextView(this).apply { text = folderName; textSize = 16f; typeface = Typeface.DEFAULT_BOLD; applyContrastingText(currentTheme.isDark); layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f) }
            val btnEdit = ImageButton(this).apply { setImageResource(android.R.drawable.ic_menu_edit); background = null; setColorFilter(if (currentTheme.isDark) Color.WHITE else Color.BLACK); setPadding(10,10,10,10); setOnClickListener { mostrarDialogoEditar(folderName) } }
            folderRow.addView(txtFolder)
            folderRow.addView(btnEdit)
            folderWrapper.addView(waveView); folderWrapper.addView(folderRow); playlistFoldersContainer.addView(folderWrapper)
        }
    }

    private fun mostrarDialogoDescargaPlaylist(initialUrl: String = "") {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 40)
        }
        val inputUrl = EditText(this).apply {
            hint = "Enlace de la playlist (YouTube)"
            applyContrastingInput(currentTheme.isDark)
            if (initialUrl.isNotEmpty()) setText(initialUrl)
        }
        val inputFolder = EditText(this).apply {
            hint = "Nombre de carpeta destino (opcional)"
            applyContrastingInput(currentTheme.isDark)
        }
        container.addView(inputUrl)
        container.addView(inputFolder)

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Descargar Playlist Inteligente")
            .setMessage("Se registrarán las canciones en la cola persistente sin descargar audio de inmediato. Luego WorkManager las descargará una a una en segundo plano.")
            .setView(container)
            .setPositiveButton("Registrar y Descargar") { _, _ ->
                val url = inputUrl.text.toString().trim()
                val folder = inputFolder.text.toString().trim().takeIf { it.isNotBlank() }
                if (url.isEmpty()) {
                    Toast.makeText(this, "Por favor ingresa un enlace válido", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val progressDialog = androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Analizando Playlist")
                    .setMessage("Extrayendo lista de canciones sin descargar...")
                    .setCancelable(false)
                    .create()
                progressDialog.show()

                viewModel.registerPlaylistForDownload(
                    playlistUrl = url,
                    folderName = folder,
                    onStatus = { status ->
                        progressDialog.setMessage(status)
                    },
                    onFinished = { count, folderName ->
                        try { if (progressDialog.isShowing) progressDialog.dismiss() } catch (e: Exception) {}
                        if (count > 0) {
                            androidx.appcompat.app.AlertDialog.Builder(this)
                                .setTitle("Playlist Registrada")
                                .setMessage("Se añadieron $count canciones a la cola de SQLite.\n\nLa descarga se procesará una a una en segundo plano con notificación activa. Si se corta internet o se cierra la app, se reanudará automáticamente.")
                                .setPositiveButton("Entendido", null)
                                .show()
                            viewModel.loadLocalMusic()
                        } else {
                            Toast.makeText(this, "$folderName", Toast.LENGTH_LONG).show()
                        }
                    }
                )
            }
            .setNegativeButton("Cancelar", null)
            .show()
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
                        if (nombrePlaylistActual == folderName) {
                            nombrePlaylistActual = nuevo
                            txtArtist.text = "Playlist: $nombrePlaylistActual"
                            musicService?.updatePlaylistName(folderName, nuevo)
                        }
                    } 
                } 
            }
            .setNegativeButton("Borrar") { _, _ -> 
                val f = File(getExternalFilesDir(null), "Base-Musica/$folderName")
                if(f.exists()){ 
                    f.listFiles()?.forEach { audioFile ->
                        if (audioFile.isFile) {
                            File(getExternalFilesDir(null), "Base-Videos/${audioFile.nameWithoutExtension}.mp4").delete()
                        }
                    }
                    f.deleteRecursively()
                    viewModel.loadLocalMusic() 
                    
                    musicService?.handleDeletedFolder(folderName)
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
            
            for (song in selectedSongs) {
                val audioFile = java.io.File(song)
                java.io.File(getExternalFilesDir(null), "Base-Videos/${audioFile.nameWithoutExtension}.mp4").delete()
                audioFile.delete()
            }
            musicService?.handleDeletedSongs(selectedSongs)
            
            if (::queueAdapter.isInitialized) queueAdapter.updateList(playlistActual) else precargarCaratulasCola(playlistActual)
            isSelectionMode = false; selectedSongs.clear(); btnBackFolder.performClick(); viewModel.loadLocalMusic()
        }

        lista.forEachIndexed { idx, songPath ->
            val songRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(25, 25, 25, 25); background = getGlassDrawable(currentTheme.isDark); layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 8, 0, 8) }; tag = songPath }
            val imgCover = ImageView(this).apply { layoutParams = LinearLayout.LayoutParams(130, 130).apply { setMargins(0, 0, 30, 0) }; scaleType = ImageView.ScaleType.CENTER_CROP; clipToOutline = true; outlineProvider = object : ViewOutlineProvider() { override fun getOutline(view: View, outline: Outline) { outline.setRoundRect(0, 0, view.width, view.height, 20f) } }; setBackgroundColor(Color.parseColor(if(currentTheme.isDark) "#2A2A2A" else "#E0E0E0")) }
            loadCoverListAsync(songPath, imgCover)
            val txtName = TextView(this).apply { text = songPath.substringAfterLast("/").substringBeforeLast(".").replace("_", " "); textSize = 15f; applyContrastingText(currentTheme.isDark); layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f) }
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
        musicService?.isLooping = false
        videoMediaPlayer?.isLooping = false
        (dotLoop.background as GradientDrawable).setColor(Color.TRANSPARENT)
        dotLoop.invalidate()
    }

    private fun extraerTemaDinamicoDeCaratula(bitmap: Bitmap) {
        if (!isDynamicThemeEnabled) return
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val scaledBmp = Bitmap.createScaledBitmap(bitmap, 150, 150, true)
                val palette = Palette.from(scaledBmp).generate()

                val vibrantSwatch = palette.vibrantSwatch
                val dominantSwatch = palette.dominantSwatch
                val primarySwatch = vibrantSwatch ?: dominantSwatch

                val mutedSwatch = palette.mutedSwatch
                val darkVibrantSwatch = palette.darkVibrantSwatch
                val secondarySwatch = mutedSwatch ?: darkVibrantSwatch

                if (primarySwatch == null) return@launch

                val primaryColor = primarySwatch.rgb
                val secondaryColor = secondarySwatch?.rgb ?: primaryColor

                val primaryLuminance = ColorUtils.calculateLuminance(primaryColor)
                val secondaryLuminance = ColorUtils.calculateLuminance(secondaryColor)

                val isDark = secondaryLuminance < 0.35

                val bgBase = if (isDark) {
                    ColorUtils.blendARGB(secondaryColor, Color.parseColor("#111111"), 0.75f)
                } else {
                    ColorUtils.blendARGB(secondaryColor, Color.WHITE, 0.80f)
                }

                val cardBg = if (isDark) {
                    ColorUtils.blendARGB(secondaryColor, Color.parseColor("#1A1A1A"), 0.65f)
                } else {
                    ColorUtils.blendARGB(Color.WHITE, primaryColor, 0.07f)
                }

                val navBg = ColorUtils.setAlphaComponent(bgBase, 204)

                val accentLight = if (isDark) {
                    ColorUtils.blendARGB(primaryColor, Color.parseColor("#222222"), 0.60f)
                } else {
                    ColorUtils.blendARGB(primaryColor, Color.WHITE, 0.65f)
                }

                val w1c1 = ColorUtils.blendARGB(primaryColor, secondaryColor, 0.3f)
                val w1c2 = ColorUtils.blendARGB(primaryColor, bgBase, 0.4f)
                val w2c1 = secondaryColor
                val w2c2 = ColorUtils.blendARGB(secondaryColor, bgBase, 0.5f)
                val w3c = ColorUtils.blendARGB(secondaryColor, bgBase, 0.75f)

                val bgLuminance = ColorUtils.calculateLuminance(bgBase)
                val textPrimary = if (bgLuminance > 0.4) Color.BLACK else Color.WHITE
                val textSecondary = if (bgLuminance > 0.4) Color.BLACK else Color.WHITE

                val dynamicTheme = ThemeColors(
                    bgBase = bgBase,
                    w1c1 = w1c1,
                    w1c2 = w1c2,
                    w2c1 = w2c1,
                    w2c2 = w2c2,
                    w3c = w3c,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    accent = primaryColor,
                    accentDark = if (primaryLuminance > 0.5) {
                        ColorUtils.blendARGB(primaryColor, Color.BLACK, 0.4f)
                    } else {
                        primaryColor
                    },
                    accentLight = accentLight,
                    cardBg = cardBg,
                    navBg = navBg,
                    isDark = isDark
                )

                withContext(Dispatchers.Main) {
                    animateThemeTransition(dynamicTheme)
                }
            } catch (e: Exception) {
            }
        }
    }

    private fun cargarCaratula(rutaArchivo: String) {
        // Gestionado por MusicPlaybackService via onAlbumArtLoaded
    }

    private fun reproducirCancionActual() {
        if (playlistActual.isEmpty() || currentIndex !in playlistActual.indices) return
        val srv = musicService
        if (srv != null) {
            if (srv.playlist !== playlistActual || srv.playlistName != nombrePlaylistActual) {
                srv.setQueueAndPlay(playlistActual, currentIndex, nombrePlaylistActual)
            } else {
                srv.playIndex(currentIndex)
            }
        } else {
            pendingPlayRequest = true
            val intent = Intent(this, MusicPlaybackService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    private fun actualizarNotificacion() {
        // Delegado a MusicPlaybackService
    }
    
    private fun togglePlayPause() {
        if (musicService != null) {
            musicService?.togglePlayPause()
        } else if (playlistActual.isNotEmpty()) {
            reproducirCancionActual()
        }
    }

    private fun reproducirSiguiente() {
        if (playlistActual.isEmpty()) return
        resetLoopState()
        musicService?.playNext()
    }

    private fun reproducirAnterior() {
        if (playlistActual.isEmpty()) return
        musicService?.playPrevious()
    }
    
    private fun actualizarBarraProgreso() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                try {
                    val srv = musicService
                    if (srv != null && srv.isPlaying()) {
                        val dur = srv.getDuration()
                        val pos = srv.getCurrentPosition()
                        if (dur > 0) {
                            wavySeekBar.progress = pos.toFloat() / dur.toFloat()
                            txtCurrentTime.text = formatTime(pos)
                            if (isTvMode && isVideoPrepared) {
                                videoMediaPlayer?.let { vp ->
                                    if (vp.isPlaying && vp.duration > 0 && kotlin.math.abs(vp.currentPosition - pos) > 650) {
                                        vp.seekTo(pos.coerceAtMost(vp.duration))
                                    }
                                }
                            }
                        }
                    }
                } catch (_: Exception) {}
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

    override fun onStart() {
        super.onStart()
        val intent = Intent(this, MusicPlaybackService::class.java)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onStop() {
        super.onStop()
        if (isServiceBound) {
            musicService?.setPlaybackListener(null)
            unbindService(serviceConnection)
            isServiceBound = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseVideoPlayer()
        try { videoSurface?.release() } catch (_: Exception) {}
        videoSurface = null
        handler.removeCallbacksAndMessages(null)
    }

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
                        android.widget.Toast.makeText(this, "Agregada a $selectedPlaylist", android.widget.Toast.LENGTH_SHORT).show()
                        
                        if (selectedPlaylist.contains("fav", ignoreCase = true) || selectedPlaylist.contains("gustas", ignoreCase = true)) {
                            if (!viewModel.isSongLiked(songPath)) {
                                viewModel.toggleLike(songPath)
                                btnFavRadial.setLikedState(true)
                            }
                        }
                    } else {
                        android.widget.Toast.makeText(this, "Error al agregar archivo", android.widget.Toast.LENGTH_SHORT).show()
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

    private fun aplicarMargenBarraNavegacionInferior() {
        val rootView = findViewById<android.view.View>(android.R.id.content) ?: return
        val baseLeft = rootView.paddingLeft
        val baseTop = rootView.paddingTop
        val baseRight = rootView.paddingRight
        val baseBottom = rootView.paddingBottom
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            rootView.setOnApplyWindowInsetsListener { v, insets ->
                val navBottom = insets.getInsets(android.view.WindowInsets.Type.navigationBars()).bottom
                v.setPadding(baseLeft, baseTop, baseRight, baseBottom + navBottom)
                insets
            }
            rootView.requestApplyInsets()
        } else {
            @Suppress("DEPRECATION")
            rootView.setOnApplyWindowInsetsListener { v, insets ->
                val navBottom = insets.systemWindowInsetBottom
                v.setPadding(baseLeft, baseTop, baseRight, baseBottom + navBottom)
                insets
            }
            rootView.requestApplyInsets()
        }
    }
}
