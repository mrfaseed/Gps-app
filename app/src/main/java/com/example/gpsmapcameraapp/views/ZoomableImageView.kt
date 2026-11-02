package com.example.gpsmapcameraapp.views

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.appcompat.widget.AppCompatImageView

/**
 * High-performance, touch-interactive ImageView supporting:
 * - Multi-touch pinch to zoom with full zoom-out return to normal fit-center size
 * - Double-tap to zoom in (2.5x) or reset to normal size
 * - Smooth drag/pan when zoomed
 * - ViewPager2 friendly: disallows intercept when panning, allows swipe when at edge or 1.0x scale
 * - Single-tap listener for toggling gallery chrome
 */
class ZoomableImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    private val currentMatrix = Matrix()
    private val matrixValues = FloatArray(9)
    private val displayRect = RectF()

    private var baseScale = 1.0f
    private var minScale = 1.0f
    private var maxScale = 5.0f
    private var doubleTapScale = 2.5f

    var onPhotoTapListener: (() -> Unit)? = null

    private var zoomAnimator: ValueAnimator? = null

    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val scaleFactor = detector.scaleFactor
            if (scaleFactor.isNaN() || scaleFactor.isInfinite()) return false

            val current = getCurrentScale()
            // Allow slight elastic over/under zoom during active pinch
            val targetScale = (current * scaleFactor).coerceIn(minScale * 0.75f, maxScale * 1.25f)
            val actualFactor = targetScale / current

            currentMatrix.postScale(actualFactor, actualFactor, detector.focusX, detector.focusY)
            checkAndCenterMatrix()
            imageMatrix = currentMatrix

            // While actively zooming, prevent ViewPager from stealing touches
            parent?.requestDisallowInterceptTouchEvent(true)
            return true
        }

        override fun onScaleEnd(detector: ScaleGestureDetector) {
            val current = getCurrentScale()
            if (current < minScale) {
                // Snap back to normal fit-center size
                animateScale(current, minScale, width / 2f, height / 2f)
            } else if (current > maxScale) {
                animateScale(current, maxScale, detector.focusX, detector.focusY)
            }
        }
    })

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            onPhotoTapListener?.invoke()
            return true
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            val current = getCurrentScale()
            if (current > minScale * 1.15f) {
                // Zoomed in -> reset smoothly back to exact normal fit-center size!
                animateScale(current, minScale, width / 2f, height / 2f)
            } else {
                // At normal size -> zoom in to 2.5x centered at tap location
                animateScale(current, doubleTapScale, e.x, e.y)
            }
            return true
        }

        override fun onScroll(
            e1: MotionEvent?,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            if (getCurrentScale() > minScale * 1.05f) {
                currentMatrix.postTranslate(-distanceX, -distanceY)
                checkAndCenterMatrix()
                imageMatrix = currentMatrix

                // If image is still pannable in the direction of drag, disallow ViewPager swipe
                val canScrollH = canScrollHorizontally(if (distanceX > 0) 1 else -1)
                parent?.requestDisallowInterceptTouchEvent(canScrollH)
                return true
            }
            return false
        }
    })

    init {
        scaleType = ScaleType.MATRIX
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        fitToCenter()
    }

    override fun setImageDrawable(drawable: Drawable?) {
        super.setImageDrawable(drawable)
        fitToCenter()
    }

    /**
     * Resets the matrix to fit the entire image centered inside the view (equivalent to FIT_CENTER)
     * and sets baseScale, minScale, maxScale relative to this exact fit size.
     */
    fun fitToCenter() {
        val d = drawable ?: return
        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()
        if (viewWidth <= 0f || viewHeight <= 0f) return

        val dWidth = d.intrinsicWidth.toFloat()
        val dHeight = d.intrinsicHeight.toFloat()
        if (dWidth <= 0f || dHeight <= 0f) return

        currentMatrix.reset()
        baseScale = minOf(viewWidth / dWidth, viewHeight / dHeight)
        minScale = baseScale
        maxScale = baseScale * 5.0f
        doubleTapScale = baseScale * 2.5f

        val dx = (viewWidth - dWidth * baseScale) / 2f
        val dy = (viewHeight - dHeight * baseScale) / 2f

        currentMatrix.postScale(baseScale, baseScale)
        currentMatrix.postTranslate(dx, dy)
        imageMatrix = currentMatrix
    }

    fun resetZoom() {
        zoomAnimator?.cancel()
        fitToCenter()
    }

    fun getCurrentScale(): Float {
        currentMatrix.getValues(matrixValues)
        return matrixValues[Matrix.MSCALE_X]
    }

    private fun getDisplayRect(): RectF {
        val d = drawable ?: return displayRect.apply { setEmpty() }
        displayRect.set(0f, 0f, d.intrinsicWidth.toFloat(), d.intrinsicHeight.toFloat())
        currentMatrix.mapRect(displayRect)
        return displayRect
    }

    private fun checkAndCenterMatrix() {
        val rect = getDisplayRect()
        if (rect.isEmpty) return

        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()
        var deltaX = 0f
        var deltaY = 0f

        // Horizontal centering / clamping
        if (rect.width() <= viewWidth) {
            deltaX = (viewWidth - rect.width()) / 2f - rect.left
        } else if (rect.left > 0f) {
            deltaX = -rect.left
        } else if (rect.right < viewWidth) {
            deltaX = viewWidth - rect.right
        }

        // Vertical centering / clamping
        if (rect.height() <= viewHeight) {
            deltaY = (viewHeight - rect.height()) / 2f - rect.top
        } else if (rect.top > 0f) {
            deltaY = -rect.top
        } else if (rect.bottom < viewHeight) {
            deltaY = viewHeight - rect.bottom
        }

        currentMatrix.postTranslate(deltaX, deltaY)
    }

    private fun animateScale(fromScale: Float, toScale: Float, focusX: Float, focusY: Float) {
        zoomAnimator?.cancel()
        zoomAnimator = ValueAnimator.ofFloat(fromScale, toScale).apply {
            duration = 240
            interpolator = AccelerateDecelerateInterpolator()
            var prevScale = fromScale
            addUpdateListener { anim ->
                val current = anim.animatedValue as Float
                if (prevScale > 0f) {
                    val factor = current / prevScale
                    currentMatrix.postScale(factor, factor, focusX, focusY)
                    checkAndCenterMatrix()
                    imageMatrix = currentMatrix
                }
                prevScale = current
            }
            start()
        }
    }

    override fun canScrollHorizontally(direction: Int): Boolean {
        // If at normal fit size, allow ViewPager2 to swipe to adjacent photo
        if (getCurrentScale() <= minScale * 1.05f) return false
        val rect = getDisplayRect()
        if (rect.isEmpty) return false

        return if (direction > 0) {
            // Scrolling left: dragging finger left to see content on the right
            rect.right > width + 2f
        } else {
            // Scrolling right: dragging finger right to see content on the left
            rect.left < -2f
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Feed both gesture detectors
        val scaleResult = scaleDetector.onTouchEvent(event)
        val gestureResult = gestureDetector.onTouchEvent(event)

        if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) {
            val current = getCurrentScale()
            if (current < minScale) {
                // If pinched smaller than screen, snap back smoothly to normal fit size
                animateScale(current, minScale, width / 2f, height / 2f)
            } else if (current > maxScale) {
                animateScale(current, maxScale, width / 2f, height / 2f)
            }
        }

        return scaleResult || gestureResult || super.onTouchEvent(event)
    }
}
