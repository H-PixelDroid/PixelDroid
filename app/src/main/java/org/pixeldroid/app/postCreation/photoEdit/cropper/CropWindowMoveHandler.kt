package org.pixeldroid.app.postCreation.photoEdit.cropper

// Simplified version of https://github.com/ArthurHub/Android-Image-Cropper , which is
// licensed under the Apache License, Version 2.0. The modifications made to it for PixelDroid
// are under licensed under the GPLv3 or later, just like the rest of the PixelDroid project

import android.graphics.PointF
import android.graphics.RectF

/**
 * Handler to update crop window edges by the move type - Horizontal, Vertical, Corner or Center.
 */
internal class CropWindowMoveHandler(
    /** The type of crop window move that is handled.  */
    private val mType: Type,
    cropWindowHandler: CropWindowHandler, touchX: Float, touchY: Float
) {
    /** Minimum width in pixels that the crop window can get.  */
    private val mMinCropWidth: Float

    /** Minimum width in pixels that the crop window can get.  */
    private val mMinCropHeight: Float

    /** Maximum height in pixels that the crop window can get.  */
    private val mMaxCropWidth: Float

    /** Maximum height in pixels that the crop window can get.  */
    private val mMaxCropHeight: Float

    /**
     * Holds the x and y offset between the exact touch location and the exact handle location that is
     * activated. There may be an offset because we allow for some leeway (specified by mHandleRadius)
     * in activating a handle. However, we want to maintain these offset values while the handle is
     * being dragged so that the handle doesn't jump.
     */
    private val mTouchOffset = PointF()

    init {
        mMinCropWidth = cropWindowHandler.minCropWidth
        mMinCropHeight = cropWindowHandler.minCropHeight
        mMaxCropWidth = cropWindowHandler.maxCropWidth
        mMaxCropHeight = cropWindowHandler.maxCropHeight
        calculateTouchOffset(cropWindowHandler.rect, touchX, touchY)
    }

    /**
     * Updates the crop window by change in the touch location.
     * Move type handled by this instance, as initialized in creation, affects how the change in
     * touch location changes the crop window position and size.
     * After the crop window position/size is changed by touch move it may result in values that
     * violate constraints: outside the bounds of the shown bitmap, smaller/larger than min/max size or
     * mismatch in aspect ratio. So a series of fixes is executed on "secondary" edges to adjust it
     * by the "primary" edge movement.
     * Primary is the edge directly affected by move type, secondary is the other edge.
     * The crop window is changed by directly setting the Edge coordinates.
     *
     * @param x the new x-coordinate of this handle
     * @param y the new y-coordinate of this handle
     * @param bounds the bounding rectangle of the image
     * @param viewWidth The bounding image view width used to know the crop overlay is at view edges.
     * @param viewHeight The bounding image view height used to know the crop overlay is at view
     * edges.
     * @param snapMargin the maximum distance (in pixels) at which the crop window should snap to the
     * image
     */
    fun move(
        rect: RectF,
        x: Float,
        y: Float,
        bounds: RectF,
        viewWidth: Int,
        viewHeight: Int,
        snapMargin: Float
    ) {

        // Adjust the coordinates for the finger position's offset (i.e. the
        // distance from the initial touch to the precise handle location).
        // We want to maintain the initial touch's distance to the pressed
        // handle so that the crop window size does not "jump".
        val adjX = x + mTouchOffset.x
        val adjY = y + mTouchOffset.y
        if (mType == Type.CENTER) {
            moveCenter(rect, adjX, adjY, bounds, viewWidth, viewHeight, snapMargin)
        } else {
            changeSize(rect, adjX, adjY, bounds, viewWidth, viewHeight, snapMargin)
        }
    }
    // region: Private methods
    /**
     * Calculates the offset of the touch point from the precise location of the specified handle.<br></br>
     * Save these values in a member variable since we want to maintain this offset as we drag the
     * handle.
     */
    private fun calculateTouchOffset(rect: RectF, touchX: Float, touchY: Float) {
        var touchOffsetX = 0f
        var touchOffsetY = 0f
        when (mType) {
            Type.TOP_LEFT -> {
                touchOffsetX = rect.left - touchX
                touchOffsetY = rect.top - touchY
            }

            Type.TOP_RIGHT -> {
                touchOffsetX = rect.right - touchX
                touchOffsetY = rect.top - touchY
            }

            Type.BOTTOM_LEFT -> {
                touchOffsetX = rect.left - touchX
                touchOffsetY = rect.bottom - touchY
            }

            Type.BOTTOM_RIGHT -> {
                touchOffsetX = rect.right - touchX
                touchOffsetY = rect.bottom - touchY
            }

            Type.LEFT -> {
                touchOffsetX = rect.left - touchX
                touchOffsetY = 0f
            }

            Type.TOP -> {
                touchOffsetX = 0f
                touchOffsetY = rect.top - touchY
            }

            Type.RIGHT -> {
                touchOffsetX = rect.right - touchX
                touchOffsetY = 0f
            }

            Type.BOTTOM -> {
                touchOffsetX = 0f
                touchOffsetY = rect.bottom - touchY
            }

            Type.CENTER -> {
                touchOffsetX = rect.centerX() - touchX
                touchOffsetY = rect.centerY() - touchY
            }
        }
        mTouchOffset.x = touchOffsetX
        mTouchOffset.y = touchOffsetY
    }

    /** Center move only changes the position of the crop window without changing the size.  */
    private fun moveCenter(
        rect: RectF,
        x: Float,
        y: Float,
        bounds: RectF,
        viewWidth: Int,
        viewHeight: Int,
        snapRadius: Float
    ) {
        var dx = x - rect.centerX()
        var dy = y - rect.centerY()
        if (rect.left + dx < 0 || rect.right + dx > viewWidth || rect.left + dx < bounds.left || rect.right + dx > bounds.right) {
            dx /= 1.05f
            mTouchOffset.x -= dx / 2
        }
        if (rect.top + dy < 0 || rect.bottom + dy > viewHeight || rect.top + dy < bounds.top || rect.bottom + dy > bounds.bottom) {
            dy /= 1.05f
            mTouchOffset.y -= dy / 2
        }
        rect.offset(dx, dy)
        snapEdgesToBounds(rect, bounds, snapRadius)
    }

    /**
     * Change the size of the crop window on the required edge (or edges in the case of a corner)
     */
    private fun changeSize(
        rect: RectF,
        x: Float,
        y: Float,
        bounds: RectF,
        viewWidth: Int,
        viewHeight: Int,
        snapMargin: Float
    ) {
        when (mType) {
            Type.TOP_LEFT -> {
                adjustTop(rect, y, bounds, snapMargin)
                adjustLeft(rect, x, bounds, snapMargin)
            }

            Type.TOP_RIGHT -> {
                adjustTop(rect, y, bounds, snapMargin)
                adjustRight(rect, x, bounds, viewWidth, snapMargin)
            }

            Type.BOTTOM_LEFT -> {
                adjustBottom(rect, y, bounds, viewHeight, snapMargin)
                adjustLeft(rect, x, bounds, snapMargin)
            }

            Type.BOTTOM_RIGHT -> {
                adjustBottom(rect, y, bounds, viewHeight, snapMargin)
                adjustRight(rect, x, bounds, viewWidth, snapMargin)
            }

            Type.LEFT -> adjustLeft(rect, x, bounds, snapMargin)
            Type.TOP -> adjustTop(rect, y, bounds, snapMargin)
            Type.RIGHT -> adjustRight(rect, x, bounds, viewWidth, snapMargin)
            Type.BOTTOM -> adjustBottom(rect, y, bounds, viewHeight, snapMargin)
            else -> {}
        }
    }

    /** Check if edges have gone out of bounds (including snap margin), and fix if needed.  */
    private fun snapEdgesToBounds(edges: RectF, bounds: RectF, margin: Float) {
        if (edges.left < bounds.left + margin) {
            edges.offset(bounds.left - edges.left, 0f)
        }
        if (edges.top < bounds.top + margin) {
            edges.offset(0f, bounds.top - edges.top)
        }
        if (edges.right > bounds.right - margin) {
            edges.offset(bounds.right - edges.right, 0f)
        }
        if (edges.bottom > bounds.bottom - margin) {
            edges.offset(0f, bounds.bottom - edges.bottom)
        }
    }

    /**
     * Get the resulting x-position of the left edge of the crop window given the handle's position
     * and the image's bounding box and snap radius.
     *
     * @param left the position that the left edge is dragged to
     * @param bounds the bounding box of the image that is being notCropped
     * @param snapMargin the snap distance to the image edge (in pixels)
     */
    private fun adjustLeft(
        rect: RectF,
        left: Float,
        bounds: RectF,
        snapMargin: Float
    ) {
        var newLeft = left
        if (newLeft < 0) {
            newLeft /= 1.05f
            mTouchOffset.x -= newLeft / 1.1f
        }
        if (newLeft < bounds.left) {
            mTouchOffset.x -= (newLeft - bounds.left) / 2f
        }
        if (newLeft - bounds.left < snapMargin) {
            newLeft = bounds.left
        }

        // Checks if the window is too small horizontally
        if (rect.right - newLeft < mMinCropWidth) {
            newLeft = rect.right - mMinCropWidth
        }

        // Checks if the window is too large horizontally
        if (rect.right - newLeft > mMaxCropWidth) {
            newLeft = rect.right - mMaxCropWidth
        }
        if (newLeft - bounds.left < snapMargin) {
            newLeft = bounds.left
        }
        rect.left = newLeft
    }

    /**
     * Get the resulting x-position of the right edge of the crop window given the handle's position
     * and the image's bounding box and snap radius.
     *
     * @param right the position that the right edge is dragged to
     * @param bounds the bounding box of the image that is being notCropped
     * @param viewWidth
     * @param snapMargin the snap distance to the image edge (in pixels)
     */
    private fun adjustRight(
        rect: RectF,
        right: Float,
        bounds: RectF,
        viewWidth: Int,
        snapMargin: Float
    ) {
        var newRight = right
        if (newRight > viewWidth) {
            newRight = viewWidth + (newRight - viewWidth) / 1.05f
            mTouchOffset.x -= (newRight - viewWidth) / 1.1f
        }
        if (newRight > bounds.right) {
            mTouchOffset.x -= (newRight - bounds.right) / 2f
        }

        // If close to the edge
        if (bounds.right - newRight < snapMargin) {
            newRight = bounds.right
        }

        // Checks if the window is too small horizontally
        if (newRight - rect.left < mMinCropWidth) {
            newRight = rect.left + mMinCropWidth
        }

        // Checks if the window is too large horizontally
        if (newRight - rect.left > mMaxCropWidth) {
            newRight = rect.left + mMaxCropWidth
        }

        // If close to the edge
        if (bounds.right - newRight < snapMargin) {
            newRight = bounds.right
        }
        rect.right = newRight
    }

    /**
     * Get the resulting y-position of the top edge of the crop window given the handle's position and
     * the image's bounding box and snap radius.
     *
     * @param top the x-position that the top edge is dragged to
     * @param bounds the bounding box of the image that is being notCropped
     * @param snapMargin the snap distance to the image edge (in pixels)
     */
    private fun adjustTop(
        rect: RectF,
        top: Float,
        bounds: RectF,
        snapMargin: Float
    ) {
        var newTop = top
        if (newTop < 0) {
            newTop /= 1.05f
            mTouchOffset.y -= newTop / 1.1f
        }
        if (newTop < bounds.top) {
            mTouchOffset.y -= (newTop - bounds.top) / 2f
        }
        if (newTop - bounds.top < snapMargin) {
            newTop = bounds.top
        }

        // Checks if the window is too small vertically
        if (rect.bottom - newTop < mMinCropHeight) {
            newTop = rect.bottom - mMinCropHeight
        }

        // Checks if the window is too large vertically
        if (rect.bottom - newTop > mMaxCropHeight) {
            newTop = rect.bottom - mMaxCropHeight
        }
        if (newTop - bounds.top < snapMargin) {
            newTop = bounds.top
        }
        rect.top = newTop
    }

    /**
     * Get the resulting y-position of the bottom edge of the crop window given the handle's position
     * and the image's bounding box and snap radius.
     *
     * @param bottom     the position that the bottom edge is dragged to
     * @param bounds     the bounding box of the image that is being notCropped
     * @param viewHeight
     * @param snapMargin the snap distance to the image edge (in pixels)
     */
    private fun adjustBottom(
        rect: RectF,
        bottom: Float,
        bounds: RectF,
        viewHeight: Int,
        snapMargin: Float
    ) {
        var newBottom = bottom
        if (newBottom > viewHeight) {
            newBottom = viewHeight + (newBottom - viewHeight) / 1.05f
            mTouchOffset.y -= (newBottom - viewHeight) / 1.1f
        }
        if (newBottom > bounds.bottom) {
            mTouchOffset.y -= (newBottom - bounds.bottom) / 2f
        }
        if (bounds.bottom - newBottom < snapMargin) {
            newBottom = bounds.bottom
        }

        // Checks if the window is too small vertically
        if (newBottom - rect.top < mMinCropHeight) {
            newBottom = rect.top + mMinCropHeight
        }

        // Checks if the window is too small vertically
        if (newBottom - rect.top > mMaxCropHeight) {
            newBottom = rect.top + mMaxCropHeight
        }
        if (bounds.bottom - newBottom < snapMargin) {
            newBottom = bounds.bottom
        }
        rect.bottom = newBottom
    }
    // endregion

    /** The type of crop window move that is handled.  */
    enum class Type {
        TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, LEFT, TOP, RIGHT, BOTTOM, CENTER
    }
}