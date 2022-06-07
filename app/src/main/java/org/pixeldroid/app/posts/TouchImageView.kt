package org.pixeldroid.app.posts

import android.content.Context
import android.graphics.Matrix
import androidx.appcompat.widget.AppCompatImageView
import android.graphics.PointF
import android.view.ScaleGestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener
import android.util.AttributeSet
import android.util.Log
import kotlin.math.abs
import kotlin.math.min

// See https://stackoverflow.com/a/29030243

class TouchImageView(context: Context, attrs: AttributeSet? = null) :
    AppCompatImageView(context, attrs) {
    var touchMatrix: Matrix? = Matrix()
    var mode = NONE

    // Remember some things for zooming
    var last = PointF()
    var start = PointF()
    var minScale = 1f
    var maxScale = 3f
    var m: FloatArray = FloatArray(9)
    var viewWidth = 0
    var viewHeight = 0
    var saveScale = 1f
    private var origWidth = 0f
    private var origHeight = 0f
    var oldMeasuredWidth = 0
    var oldMeasuredHeight = 0
    var mScaleDetector: ScaleGestureDetector? = null

    init {
        super.setClickable(true)
        mScaleDetector = ScaleGestureDetector(context, ScaleListener())
        imageMatrix = touchMatrix
        scaleType = ScaleType.MATRIX
        setOnTouchListener { _, event ->
            mScaleDetector!!.onTouchEvent(event)
            val curr = PointF(event.x, event.y)
            when (event.action) {
                MotionEvent.ACTION_MOVE -> if (mode == DRAG) {
                    val deltaX = curr.x - last.x
                    val deltaY = curr.y - last.y
                    val fixTransX =
                        getFixDragTrans(deltaX, viewWidth.toFloat(), origWidth * saveScale)
                    val fixTransY =
                        getFixDragTrans(deltaY, viewHeight.toFloat(), origHeight * saveScale)
                    touchMatrix!!.postTranslate(fixTransX, fixTransY)
                    fixTrans()
                    last[curr.x] = curr.y
                    val transX = m[Matrix.MTRANS_X]
                    if ((getFixTrans(transX,
                            viewWidth.toFloat(),
                            origWidth * saveScale) + fixTransX).toInt() == 0
                    ) startInterceptEvent() else stopInterceptEvent()
                }
                MotionEvent.ACTION_DOWN -> {
                    last.set(curr)
                    start.set(last)
                    mode = DRAG
                    stopInterceptEvent()
                }
                MotionEvent.ACTION_UP -> {
                    mode = NONE
                    val xDiff = abs(curr.x - start.x).toInt()
                    val yDiff = abs(curr.y - start.y).toInt()
                    if (xDiff < CLICK && yDiff < CLICK) performClick()
                    startInterceptEvent()
                }
                MotionEvent.ACTION_POINTER_UP -> mode = NONE
            }
            imageMatrix = touchMatrix
            invalidate()
            true // indicate event was handled
        }
    }

    private fun startInterceptEvent() {
        parent.requestDisallowInterceptTouchEvent(false)
    }

    private fun stopInterceptEvent() {
        parent.requestDisallowInterceptTouchEvent(true)
    }

    fun setMaxZoom(x: Float) {
        maxScale = x
    }

    private inner class ScaleListener : SimpleOnScaleGestureListener() {
        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            mode = ZOOM
            return true
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            var mScaleFactor = detector.scaleFactor
            val origScale = saveScale
            saveScale *= mScaleFactor
            if (saveScale > maxScale) {
                saveScale = maxScale
                mScaleFactor = maxScale / origScale
            } else if (saveScale < minScale) {
                saveScale = minScale
                mScaleFactor = minScale / origScale
            }
            if (origWidth * saveScale <= viewWidth || origHeight * saveScale <= viewHeight) touchMatrix!!.postScale(
                mScaleFactor,
                mScaleFactor,
                (viewWidth / 2).toFloat(),
                (viewHeight / 2).toFloat()) else touchMatrix!!.postScale(mScaleFactor,
                mScaleFactor,
                detector.focusX,
                detector.focusY)
            fixTrans()
            return true
        }
    }

    fun fixTrans() {
        touchMatrix!!.getValues(m)
        val transX = m[Matrix.MTRANS_X]
        val transY = m[Matrix.MTRANS_Y]
        val fixTransX = getFixTrans(transX, viewWidth.toFloat(), origWidth * saveScale)
        val fixTransY = getFixTrans(transY, viewHeight.toFloat(), origHeight * saveScale)
        if (fixTransX != 0f || fixTransY != 0f) touchMatrix!!.postTranslate(fixTransX, fixTransY)
    }

    fun getFixTrans(trans: Float, viewSize: Float, contentSize: Float): Float {
        val minTrans: Float
        val maxTrans: Float
        if (contentSize <= viewSize) {
            minTrans = 0f
            maxTrans = viewSize - contentSize
        } else {
            minTrans = viewSize - contentSize
            maxTrans = 0f
        }
        if (trans < minTrans) return -trans + minTrans
        return if (trans > maxTrans) -trans + maxTrans else 0f
    }

    fun getFixDragTrans(delta: Float, viewSize: Float, contentSize: Float): Float {
        return if (contentSize <= viewSize) {
            0f
        } else delta
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        viewWidth = MeasureSpec.getSize(widthMeasureSpec)
        viewHeight = MeasureSpec.getSize(heightMeasureSpec)

        // Rescales image on rotation
        if (oldMeasuredHeight == viewWidth && oldMeasuredHeight == viewHeight || viewWidth == 0 || viewHeight == 0) return
        oldMeasuredHeight = viewHeight
        oldMeasuredWidth = viewWidth
        if (saveScale == 1f) {
            //Fit to screen.
            val scale: Float
            val drawable = drawable
            if (drawable == null || drawable.intrinsicWidth == 0 || drawable.intrinsicHeight == 0) return
            val bmWidth = drawable.intrinsicWidth
            val bmHeight = drawable.intrinsicHeight
            Log.d("bmSize", "bmWidth: $bmWidth bmHeight : $bmHeight")
            val scaleX = viewWidth.toFloat() / bmWidth.toFloat()
            val scaleY = viewHeight.toFloat() / bmHeight.toFloat()
            scale = min(scaleX, scaleY)
            touchMatrix!!.setScale(scale, scale)

            // Center the image
            var redundantYSpace = viewHeight.toFloat() - scale * bmHeight.toFloat()
            var redundantXSpace = viewWidth.toFloat() - scale * bmWidth.toFloat()
            redundantYSpace /= 2f
            redundantXSpace /= 2f
            touchMatrix!!.postTranslate(redundantXSpace, redundantYSpace)
            origWidth = viewWidth - 2 * redundantXSpace
            origHeight = viewHeight - 2 * redundantYSpace
            imageMatrix = touchMatrix
        }
        fixTrans()
    }

    companion object {
        // We can be in one of these 3 states
        const val NONE = 0
        const val DRAG = 1
        const val ZOOM = 2
        const val CLICK = 3
    }
}