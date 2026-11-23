package com.sdm.music

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import kotlin.random.Random

class TvStaticView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val noiseWidth = 160
    private val noiseHeight = 120
    private val pixels = IntArray(noiseWidth * noiseHeight)
    private val noiseBitmap = Bitmap.createBitmap(noiseWidth, noiseHeight, Bitmap.Config.ARGB_8888)
    private val destRect = Rect()

    private val bitmapPaint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val scanlinePaint = Paint().apply {
        color = Color.argb(55, 255, 255, 255)
        style = Paint.Style.FILL
    }

    private var scanlineOffset = 0f
    private var isAnimatingNoise = false

    fun showStatic() {
        animate().cancel()
        alpha = 1f
        visibility = VISIBLE
        isAnimatingNoise = true
        postInvalidateOnAnimation()
    }

    fun fadeOutStatic(durationMs: Long = 380L) {
        animate().cancel()
        animate()
            .alpha(0f)
            .setDuration(durationMs)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    isAnimatingNoise = false
                    visibility = GONE
                }
            })
            .start()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (visibility != VISIBLE) return

        for (i in pixels.indices) {
            val shade = Random.nextInt(30, 235)
            pixels[i] = (0xFF shl 24) or (shade shl 16) or (shade shl 8) or shade
        }
        noiseBitmap.setPixels(pixels, 0, noiseWidth, 0, 0, noiseWidth, noiseHeight)

        destRect.set(0, 0, width, height)
        canvas.drawBitmap(noiseBitmap, null, destRect, bitmapPaint)

        // Barrido horizontal analógico
        val bandHeight = height * 0.08f
        scanlineOffset = (scanlineOffset + height * 0.045f) % height.coerceAtLeast(1)
        canvas.drawRect(0f, scanlineOffset, width.toFloat(), scanlineOffset + bandHeight, scanlinePaint)

        if (isAnimatingNoise) {
            postInvalidateOnAnimation()
        }
    }

    override fun onDetachedFromWindow() {
        isAnimatingNoise = false
        super.onDetachedFromWindow()
    }
}
