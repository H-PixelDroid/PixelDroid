package org.pixeldroid.media_editor.photoEdit.cropper

// Simplified version of https://github.com/ArthurHub/Android-Image-Cropper , which is
// licensed under the Apache License, Version 2.0. The modifications made to it for PixelDroid
// are under licensed under the GPLv3 or later, just like the rest of the PixelDroid project


import android.content.Context
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import org.pixeldroid.media_editor.photoEdit.VideoEditActivity.RelativeCropPosition
import kotlin.math.max
import kotlin.math.min

/** A custom View representing the crop window and the shaded background outside the crop window.  */
class CropOverlayView  // endregion
@JvmOverloads constructor(context: Context?, attrs: AttributeSet? = null) : View(context, attrs) {
    // region: Fields and Consts
    /** Handler from crop window stuff, moving and knowing position.  */
    private val mCropWindowHandler = CropWindowHandler()

    /** The Paint used to draw the white rectangle around the crop area.  */
    private var mBorderPaint: Paint? = null

    /** The Paint used to draw the corners of the Border  */
    private var mBorderCornerPaint: Paint? = null

    /** The Paint used to draw the guidelines within the crop area when pressed.  */
    private var mGuidelinePaint: Paint? = null

    /** The bounding box around the Bitmap that we are cropping.  */
    private val mCalcBounds = RectF()

    /** The bounding image view width used to know the crop overlay is at view edges.  */
    private var mViewWidth = 0

    /** The bounding image view height used to know the crop overlay is at view edges.  */
    private var mViewHeight = 0

    /** The Handle that is currently pressed; null if no Handle is pressed.  */
    private var mMoveHandler: CropWindowMoveHandler? = null

    /** the initial crop window rectangle to set  */
    private val mInitialCropWindowRect = Rect()

    /** Whether the Crop View has been initialized for the first time  */
    private var initializedCropWindow = false
    /** Get the left/top/right/bottom coordinates of the crop window.  */
    /** Set the left/top/right/bottom coordinates of the crop window.  */
    var cropWindowRect: RectF
        get() = mCropWindowHandler.rect
        set(rect) {
            mCropWindowHandler.rect = rect
        }

    /**
     * Informs the CropOverlayView of the image's position relative to the ImageView. This is
     * necessary to call in order to draw the crop window.
     *
     * @param viewWidth The bounding image view width.
     * @param viewHeight The bounding image view height.
     */
    fun setBounds(viewWidth: Int, viewHeight: Int) {
        mViewWidth = viewWidth
        mViewHeight = viewHeight
        val cropRect = mCropWindowHandler.rect
        if (cropRect.width() == 0f || cropRect.height() == 0f) {
            initCropWindow()
        }
    }

    /** Resets the crop overlay view.  */
    fun resetCropOverlayView() {
        if (initializedCropWindow) {
            cropWindowRect = RectF()
            initCropWindow()
            invalidate()
        }
    }

    /**
     * Set the max width/height and scale factor of the shown image to original image to scale the
     * limits appropriately.
     */
    fun setCropWindowLimits(maxWidth: Float, maxHeight: Float) {
        mCropWindowHandler.setCropWindowLimits(maxWidth, maxHeight)
    }
    /** Get crop window initial rectangle.  */
    /** Set crop window initial rectangle to be used instead of default.  */
    var initialCropWindowRect: Rect
        get() = mInitialCropWindowRect
        set(rect) {
            mInitialCropWindowRect.set(rect)
            if (initializedCropWindow) {
                initCropWindow()
                invalidate()
            }
        }

    fun setRecordedCropWindowRect(relativeCropPosition: RelativeCropPosition) {
        val rect = RectF(
            mInitialCropWindowRect.left + relativeCropPosition.relativeX * mInitialCropWindowRect.width(),
            mInitialCropWindowRect.top + relativeCropPosition.relativeY * mInitialCropWindowRect.height(),
            relativeCropPosition.relativeWidth * mInitialCropWindowRect.width() + mInitialCropWindowRect.left + relativeCropPosition.relativeX * mInitialCropWindowRect.width(),
            relativeCropPosition.relativeHeight * mInitialCropWindowRect.height() + mInitialCropWindowRect.top + relativeCropPosition.relativeY * mInitialCropWindowRect.height()
        )
        mCropWindowHandler.rect = rect
    }

    /** Reset crop window to initial rectangle.  */
    fun resetCropWindowRect() {
        if (initializedCropWindow) {
            initCropWindow()
            invalidate()
        }
    }

    /**
     * Sets all initial values, but does not call initCropWindow to reset the views.<br></br>
     * Used once at the very start to initialize the attributes.
     */
    fun setInitialAttributeValues() {
        val dm = Resources.getSystem().displayMetrics
        mBorderPaint = getNewPaintOfThickness(
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3f, dm),
            Color.argb(170, 255, 255, 255)
        )
        mBorderCornerPaint = getNewPaintOfThickness(
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2f, dm),
            Color.WHITE
        )
        mGuidelinePaint = getNewPaintOfThickness(
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1f, dm),
            Color.argb(170, 255, 255, 255)
        )
    }
    // region: Private methods
    /**
     * Set the initial crop window size and position. This is dependent on the size and position of
     * the image being cropped.
     */
    private fun initCropWindow() {
        val rect = RectF()

        // Tells the attribute functions the crop window has already been initialized
        initializedCropWindow = true
        if (mInitialCropWindowRect.width() > 0 && mInitialCropWindowRect.height() > 0) {
            // Get crop window position relative to the displayed image.
            rect.left = mInitialCropWindowRect.left.toFloat()
            rect.top = mInitialCropWindowRect.top.toFloat()
            rect.right = rect.left + mInitialCropWindowRect.width()
            rect.bottom = rect.top + mInitialCropWindowRect.height()
        }
        fixCropWindowRectByRules(rect)
        mCropWindowHandler.rect = rect
    }

    /** Fix the given rect to fit into bitmap rect and follow min, max and aspect ratio rules.  */
    private fun fixCropWindowRectByRules(rect: RectF) {
        if (rect.width() < mCropWindowHandler.minCropWidth) {
            val adj = (mCropWindowHandler.minCropWidth - rect.width()) / 2
            rect.left -= adj
            rect.right += adj
        }
        if (rect.height() < mCropWindowHandler.minCropHeight) {
            val adj = (mCropWindowHandler.minCropHeight - rect.height()) / 2
            rect.top -= adj
            rect.bottom += adj
        }
        if (rect.width() > mCropWindowHandler.maxCropWidth) {
            val adj = (rect.width() - mCropWindowHandler.maxCropWidth) / 2
            rect.left += adj
            rect.right -= adj
        }
        if (rect.height() > mCropWindowHandler.maxCropHeight) {
            val adj = (rect.height() - mCropWindowHandler.maxCropHeight) / 2
            rect.top += adj
            rect.bottom -= adj
        }
        setBounds()
        if (mCalcBounds.width() > 0 && mCalcBounds.height() > 0) {
            val leftLimit = max(mCalcBounds.left, 0f)
            val topLimit = max(mCalcBounds.top, 0f)
            val rightLimit = min(mCalcBounds.right, width.toFloat())
            val bottomLimit = min(mCalcBounds.bottom, height.toFloat())
            if (rect.left < leftLimit) {
                rect.left = leftLimit
            }
            if (rect.top < topLimit) {
                rect.top = topLimit
            }
            if (rect.right > rightLimit) {
                rect.right = rightLimit
            }
            if (rect.bottom > bottomLimit) {
                rect.bottom = bottomLimit
            }
        }
    }

    /**
     * Draw crop overview by drawing background over image not in the cropping area, then borders and
     * guidelines.
     */
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw translucent background for the notCropped area.
        drawBackground(canvas)
        if (mCropWindowHandler.showGuidelines()) {
            // Determines whether guidelines should be drawn or not
            if (mMoveHandler != null) {
                // Draw only when resizing
                drawGuidelines(canvas)
            }
        }
        drawBorders(canvas)
        drawCorners(canvas)
    }

    /** Draw shadow background over the image not including the crop area.  */
    private fun drawBackground(canvas: Canvas) {
        val rect = mCropWindowHandler.rect
        val background = getNewPaint(Color.argb(119, 0, 0, 0))
        canvas.drawRect(
            mInitialCropWindowRect.left.toFloat(),
            mInitialCropWindowRect.top.toFloat(),
            rect.left,
            mInitialCropWindowRect.bottom.toFloat(),
            background
        )
        canvas.drawRect(
            rect.left,
            rect.bottom,
            mInitialCropWindowRect.right.toFloat(),
            mInitialCropWindowRect.bottom.toFloat(),
            background
        )
        canvas.drawRect(
            rect.right,
            mInitialCropWindowRect.top.toFloat(),
            mInitialCropWindowRect.right.toFloat(),
            rect.bottom,
            background
        )
        canvas.drawRect(
            rect.left,
            mInitialCropWindowRect.top.toFloat(),
            rect.right,
            rect.top,
            background
        )
    }

    /**
     * Draw 2 vertical and 2 horizontal guidelines inside the cropping area to split it into 9 equal
     * parts.
     */
    private fun drawGuidelines(canvas: Canvas) {
        if (mGuidelinePaint != null) {
            val sw: Float = if (mBorderPaint != null) mBorderPaint!!.strokeWidth else 0f
            val rect = mCropWindowHandler.rect
            rect.inset(sw, sw)
            val oneThirdCropWidth = rect.width() / 3
            val oneThirdCropHeight = rect.height() / 3

            // Draw vertical guidelines.
            val x1 = rect.left + oneThirdCropWidth
            val x2 = rect.right - oneThirdCropWidth
            canvas.drawLine(x1, rect.top, x1, rect.bottom, mGuidelinePaint!!)
            canvas.drawLine(x2, rect.top, x2, rect.bottom, mGuidelinePaint!!)

            // Draw horizontal guidelines.
            val y1 = rect.top + oneThirdCropHeight
            val y2 = rect.bottom - oneThirdCropHeight
            canvas.drawLine(rect.left, y1, rect.right, y1, mGuidelinePaint!!)
            canvas.drawLine(rect.left, y2, rect.right, y2, mGuidelinePaint!!)
        }
    }

    /** Draw borders of the crop area.  */
    private fun drawBorders(canvas: Canvas) {
        if (mBorderPaint != null) {
            val w = mBorderPaint!!.strokeWidth
            val rect = mCropWindowHandler.rect
            // Make the rectangle a bit smaller to accommodate for the border
            rect.inset(w / 2, w / 2)

            // Draw rectangle crop window border.
            canvas.drawRect(rect, mBorderPaint!!)
        }
    }

    /** Draw the corner of crop overlay.  */
    private fun drawCorners(canvas: Canvas) {
        val dm = Resources.getSystem().displayMetrics
        if (mBorderCornerPaint != null) {
            val lineWidth: Float = if (mBorderPaint != null) mBorderPaint!!.strokeWidth else 0f
            val cornerWidth = mBorderCornerPaint!!.strokeWidth

            // The corners should be a bit offset from the borders
            val w = (cornerWidth / 2
                    + TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5f, dm))
            val rect = mCropWindowHandler.rect
            rect.inset(w, w)
            val cornerOffset = (cornerWidth - lineWidth) / 2
            val cornerExtension = cornerWidth / 2 + cornerOffset

            /* the length of the border corner to draw */
            val mBorderCornerLength =
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 14f, dm)

            // Top left
            canvas.drawLine(
                rect.left - cornerOffset,
                rect.top - cornerExtension,
                rect.left - cornerOffset,
                rect.top + mBorderCornerLength,
                mBorderCornerPaint!!
            )
            canvas.drawLine(
                rect.left - cornerExtension,
                rect.top - cornerOffset,
                rect.left + mBorderCornerLength,
                rect.top - cornerOffset,
                mBorderCornerPaint!!
            )

            // Top right
            canvas.drawLine(
                rect.right + cornerOffset,
                rect.top - cornerExtension,
                rect.right + cornerOffset,
                rect.top + mBorderCornerLength,
                mBorderCornerPaint!!
            )
            canvas.drawLine(
                rect.right + cornerExtension,
                rect.top - cornerOffset,
                rect.right - mBorderCornerLength,
                rect.top - cornerOffset,
                mBorderCornerPaint!!
            )

            // Bottom left
            canvas.drawLine(
                rect.left - cornerOffset,
                rect.bottom + cornerExtension,
                rect.left - cornerOffset,
                rect.bottom - mBorderCornerLength,
                mBorderCornerPaint!!
            )
            canvas.drawLine(
                rect.left - cornerExtension,
                rect.bottom + cornerOffset,
                rect.left + mBorderCornerLength,
                rect.bottom + cornerOffset,
                mBorderCornerPaint!!
            )

            // Bottom left
            canvas.drawLine(
                rect.right + cornerOffset,
                rect.bottom + cornerExtension,
                rect.right + cornerOffset,
                rect.bottom - mBorderCornerLength,
                mBorderCornerPaint!!
            )
            canvas.drawLine(
                rect.right + cornerExtension,
                rect.bottom + cornerOffset,
                rect.right - mBorderCornerLength,
                rect.bottom + cornerOffset,
                mBorderCornerPaint!!
            )
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // If this View is not enabled, don't allow for touch interactions.
        return if (isEnabled) {
            /* Boolean to see if multi touch is enabled for the crop rectangle */
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    onActionDown(event.x, event.y)
                    true
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    parent.requestDisallowInterceptTouchEvent(false)
                    onActionUp()
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    onActionMove(event.x, event.y)
                    parent.requestDisallowInterceptTouchEvent(true)
                    true
                }

                else -> false
            }
        } else {
            false
        }
    }

    /**
     * On press down start crop window movement depending on the location of the press.<br></br>
     * if press is far from crop window then no move handler is returned (null).
     */
    private fun onActionDown(x: Float, y: Float) {
        val dm = Resources.getSystem().displayMetrics
        mMoveHandler = mCropWindowHandler.getMoveHandler(
            x,
            y,
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24f, dm)
        )
        if (mMoveHandler != null) {
            invalidate()
        }
    }

    /** Clear move handler starting in [.onActionDown] if exists.  */
    private fun onActionUp() {
        if (mMoveHandler != null) {
            mMoveHandler = null
            invalidate()
        }
    }

    /**
     * Handle move of crop window using the move handler created in [.onActionDown].<br></br>
     * The move handler will do the proper move/resize of the crop window.
     */
    private fun onActionMove(x: Float, y: Float) {
        if (mMoveHandler != null) {
            val rect = mCropWindowHandler.rect
            setBounds()
            val dm = Resources.getSystem().displayMetrics
            val snapRadius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3f, dm)
            mMoveHandler!!.move(
                rect,
                x,
                y,
                mCalcBounds,
                mViewWidth,
                mViewHeight,
                snapRadius
            )
            mCropWindowHandler.rect = rect
            invalidate()
        }
    }

    /**
     * Calculate the bounding rectangle for current crop window
     * The bounds rectangle is the bitmap rectangle
     */
    private fun setBounds() {
        mCalcBounds.set(mInitialCropWindowRect)
    }

    companion object {
        /** Creates the Paint object for drawing.  */
        private fun getNewPaint(color: Int): Paint {
            val paint = Paint()
            paint.color = color
            return paint
        }

        /** Creates the Paint object for given thickness and color  */
        private fun getNewPaintOfThickness(thickness: Float, color: Int): Paint {
            val borderPaint = Paint()
            borderPaint.color = color
            borderPaint.strokeWidth = thickness
            borderPaint.style = Paint.Style.STROKE
            borderPaint.isAntiAlias = true
            return borderPaint
        }
    }
}