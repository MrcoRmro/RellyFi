package com.sdm.music

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.graphics.Color
import android.view.TextureView
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LiveVideoThemeController(
    private val scope: CoroutineScope,
    private val onThemeUpdated: (primary: Int, secondary: Int, onPrimary: Int, onSecondary: Int) -> Unit
) {
    private var samplingJob: Job? = null
    private var colorAnimator: ValueAnimator? = null

    private var currentPrimary: Int = Color.parseColor("#121212")
    private var currentSecondary: Int = Color.parseColor("#1E1E1E")

    fun setInitialColors(primary: Int, secondary: Int) {
        currentPrimary = primary
        currentSecondary = secondary
        emitContrastAdjusted(primary, secondary)
    }

    fun startSampling(textureView: TextureView, intervalMs: Long = 1100L) {
        samplingJob?.cancel()
        samplingJob = scope.launch(Dispatchers.Main) {
            while (isActive) {
                if (textureView.isAvailable && textureView.width > 0 && textureView.height > 0) {
                    val sample = textureView.getBitmap(64, 64)
                    if (sample != null) {
                        val (nextPrimary, nextSecondary) = withContext(Dispatchers.Default) {
                            val palette = Palette.from(sample)
                                .maximumColorCount(8)
                                .generate()
                            sample.recycle()

                            val p = palette.vibrantSwatch?.rgb
                                ?: palette.dominantSwatch?.rgb
                                ?: currentPrimary
                            val s = palette.darkVibrantSwatch?.rgb
                                ?: palette.mutedSwatch?.rgb
                                ?: palette.darkMutedSwatch?.rgb
                                ?: p
                            Pair(p, s)
                        }
                        animateToColors(nextPrimary, nextSecondary)
                    }
                }
                delay(intervalMs)
            }
        }
    }

    fun stopAndRestore(staticPrimary: Int, staticSecondary: Int) {
        samplingJob?.cancel()
        samplingJob = null
        animateToColors(staticPrimary, staticSecondary)
    }

    private fun animateToColors(targetPrimary: Int, targetSecondary: Int, durationMs: Long = 750L) {
        if (targetPrimary == currentPrimary && targetSecondary == currentSecondary) return

        val startPrimary = currentPrimary
        val startSecondary = currentSecondary
        val evaluator = ArgbEvaluator()

        colorAnimator?.cancel()
        colorAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = durationMs
            addUpdateListener { anim ->
                val fraction = anim.animatedValue as Float
                val interpPrimary = evaluator.evaluate(fraction, startPrimary, targetPrimary) as Int
                val interpSecondary = evaluator.evaluate(fraction, startSecondary, targetSecondary) as Int
                currentPrimary = interpPrimary
                currentSecondary = interpSecondary
                emitContrastAdjusted(interpPrimary, interpSecondary)
            }
            start()
        }
    }

    private fun emitContrastAdjusted(primary: Int, secondary: Int) {
        val onPrimary = if (ColorUtils.calculateLuminance(primary) > 0.5) Color.BLACK else Color.WHITE
        val onSecondary = if (ColorUtils.calculateLuminance(secondary) > 0.5) Color.BLACK else Color.WHITE
        onThemeUpdated(primary, secondary, onPrimary, onSecondary)
    }
}
