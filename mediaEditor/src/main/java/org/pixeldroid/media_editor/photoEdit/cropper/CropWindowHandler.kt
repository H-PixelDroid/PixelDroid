package org.pixeldroid.media_editor.photoEdit.cropper

// Simplified version of https://github.com/ArthurHub/Android-Image-Cropper , which is
// licensed under the Apache License, Version 2.0. The modifications made to it for PixelDroid
// are under licensed under the GPLv3 or later, just like the rest of the PixelDroid project


import android.content.res.Resources
import android.graphics.RectF
import android.util.TypedValue
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** Handler from crop window stuff, moving and knowing position.  */
internal class CropWindowHandler {
    /** The 4 edges of the crop window defining its coordinates and size  */
    private val mEdges = RectF()

    /**
     * Rectangle used to return the edges rectangle without ability to change it and without
     * creating new all the time.
     */
    private val mGetEdges = RectF()

    /** Maximum width in pixels that the crop window can CURRENTLY get.  */
    private var mMaxCropWindowWidth = 0f

    /** Maximum height in pixels that the crop window can CURRENTLY get.  */
    private var mMaxCropWindowHeight = 0f

    /** The left/top/right/bottom coordinates of the crop window.  */
    var rect: RectF
        get() {
            mGetEdges.set(mEdges)
            return mGetEdges
        }
        set(rect) {
            mEdges.set(rect)
        }

    /** Minimum width in pixels that the crop window can get.  */
    val minCropWidth: Float
        get() {
            val dm = Resources.getSystem().displayMetrics
            val mMinCropResultWidth = 40f
            return max(
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 42f, dm).toInt().toFloat(),
                mMinCropResultWidth
            )
        }

    /** Minimum height in pixels that the crop window can get.  */
    val minCropHeight: Float
        get() {
            val dm = Resources.getSystem().displayMetrics
            val mMinCropResultHeight = 40f
            return max(
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 42f, dm).toInt().toFloat(),
                mMinCropResultHeight
            )
        }

    /** Maximum width in pixels that the crop window can get.  */
    val maxCropWidth: Float
        get() {
            val mMaxCropResultWidth = 99999f
            return min(mMaxCropWindowWidth, mMaxCropResultWidth)
        }

    /** Maximum height in pixels that the crop window can get.  */
    val maxCropHeight: Float
        get() {
            val mMaxCropResultHeight = 99999f
            return min(mMaxCropWindowHeight, mMaxCropResultHeight)
        }

    /**
     * Set the max width/height of the shown image to original image to scale the limits appropriately
     */
    fun setCropWindowLimits(maxWidth: Float, maxHeight: Float) {
        mMaxCropWindowWidth = maxWidth
        mMaxCropWindowHeight = maxHeight
    }

    /**
     * Indicates whether the crop window is small enough that the guidelines should be shown. Public
     * because this function is also used to determine if the center handle should be focused.
     *
     * @return boolean Whether the guidelines should be shown or not
     */
    fun showGuidelines(): Boolean {
        return !(mEdges.width() < 100 || mEdges.height() < 100)
    }

    /**
     * Determines which, if any, of the handles are pressed given the touch coordinates, the bounding
     * box, and the touch radius.
     *
     * @param x the x-coordinate of the touch point
     * @param y the y-coordinate of the touch point
     * @param targetRadius the target radius in pixels
     * @return the Handle that was pressed; null if no Handle was pressed
     */
    fun getMoveHandler(x: Float, y: Float, targetRadius: Float): CropWindowMoveHandler? {
        val type = getRectanglePressedMoveType(x, y, targetRadius)
        return if (type != null) CropWindowMoveHandler(type, this, x, y) else null
    }
    // region: Private methods
    /**
     * Determines which, if any, of the handles are pressed given the touch coordinates, the bounding
     * box, and the touch radius.
     *
     * @param x the x-coordinate of the touch point
     * @param y the y-coordinate of the touch point
     * @param targetRadius the target radius in pixels
     * @return the Handle that was pressed; null if no Handle was pressed
     */
    private fun getRectanglePressedMoveType(
        x: Float, y: Float, targetRadius: Float
    ): CropWindowMoveHandler.Type? {
        var moveType: CropWindowMoveHandler.Type? = null

        // Note: corner-handles take precedence, then side-handles, then center.
        if (isInCornerTargetZone(x, y, mEdges.left, mEdges.top, targetRadius)) {
            moveType = CropWindowMoveHandler.Type.TOP_LEFT
        } else if (isInCornerTargetZone(
                x, y, mEdges.right, mEdges.top, targetRadius
            )
        ) {
            moveType = CropWindowMoveHandler.Type.TOP_RIGHT
        } else if (isInCornerTargetZone(
                x, y, mEdges.left, mEdges.bottom, targetRadius
            )
        ) {
            moveType = CropWindowMoveHandler.Type.BOTTOM_LEFT
        } else if (isInCornerTargetZone(
                x, y, mEdges.right, mEdges.bottom, targetRadius
            )
        ) {
            moveType = CropWindowMoveHandler.Type.BOTTOM_RIGHT
        } else if (isInCenterTargetZone(
                x, y, mEdges.left, mEdges.top, mEdges.right, mEdges.bottom
            )
            && focusCenter()
        ) {
            moveType = CropWindowMoveHandler.Type.CENTER
        } else if (isInHorizontalTargetZone(
                x, y, mEdges.left, mEdges.right, mEdges.top, targetRadius
            )
        ) {
            moveType = CropWindowMoveHandler.Type.TOP
        } else if (isInHorizontalTargetZone(
                x, y, mEdges.left, mEdges.right, mEdges.bottom, targetRadius
            )
        ) {
            moveType = CropWindowMoveHandler.Type.BOTTOM
        } else if (isInVerticalTargetZone(
                x, y, mEdges.left, mEdges.top, mEdges.bottom, targetRadius
            )
        ) {
            moveType = CropWindowMoveHandler.Type.LEFT
        } else if (isInVerticalTargetZone(
                x, y, mEdges.right, mEdges.top, mEdges.bottom, targetRadius
            )
        ) {
            moveType = CropWindowMoveHandler.Type.RIGHT
        } else if (isInCenterTargetZone(
                x, y, mEdges.left, mEdges.top, mEdges.right, mEdges.bottom
            )
            && !focusCenter()
        ) {
            moveType = CropWindowMoveHandler.Type.CENTER
        }
        return moveType
    }

    /**
     * Determines if the cropper should focus on the center handle or the side handles. If it is a
     * small image, focus on the center handle so the user can move it. If it is a large image, focus
     * on the side handles so user can grab them. Corresponds to the appearance of the
     * RuleOfThirdsGuidelines.
     *
     * @return true if it is small enough such that it should focus on the center; less than
     * show_guidelines limit
     */
    private fun focusCenter(): Boolean = !showGuidelines()

    // endregion

    companion object {
        /**
         * Determines if the specified coordinate is in the target touch zone for a corner handle.
         *
         * @param x the x-coordinate of the touch point
         * @param y the y-coordinate of the touch point
         * @param handleX the x-coordinate of the corner handle
         * @param handleY the y-coordinate of the corner handle
         * @param targetRadius the target radius in pixels
         * @return true if the touch point is in the target touch zone; false otherwise
         */
        private fun isInCornerTargetZone(
            x: Float, y: Float, handleX: Float, handleY: Float, targetRadius: Float
        ): Boolean {
            return abs(x - handleX) <= targetRadius && abs(y - handleY) <= targetRadius
        }

        /**
         * Determines if the specified coordinate is in the target touch zone for a horizontal bar handle.
         *
         * @param x the x-coordinate of the touch point
         * @param y the y-coordinate of the touch point
         * @param handleXStart the left x-coordinate of the horizontal bar handle
         * @param handleXEnd the right x-coordinate of the horizontal bar handle
         * @param handleY the y-coordinate of the horizontal bar handle
         * @param targetRadius the target radius in pixels
         * @return true if the touch point is in the target touch zone; false otherwise
         */
        private fun isInHorizontalTargetZone(
            x: Float,
            y: Float,
            handleXStart: Float,
            handleXEnd: Float,
            handleY: Float,
            targetRadius: Float
        ): Boolean {
            return x > handleXStart && x < handleXEnd && abs(y - handleY) <= targetRadius
        }

        /**
         * Determines if the specified coordinate is in the target touch zone for a vertical bar handle.
         *
         * @param x the x-coordinate of the touch point
         * @param y the y-coordinate of the touch point
         * @param handleX the x-coordinate of the vertical bar handle
         * @param handleYStart the top y-coordinate of the vertical bar handle
         * @param handleYEnd the bottom y-coordinate of the vertical bar handle
         * @param targetRadius the target radius in pixels
         * @return true if the touch point is in the target touch zone; false otherwise
         */
        private fun isInVerticalTargetZone(
            x: Float,
            y: Float,
            handleX: Float,
            handleYStart: Float,
            handleYEnd: Float,
            targetRadius: Float
        ): Boolean {
            return abs(x - handleX) <= targetRadius && y > handleYStart && y < handleYEnd
        }

        /**
         * Determines if the specified coordinate falls anywhere inside the given bounds.
         *
         * @param x the x-coordinate of the touch point
         * @param y the y-coordinate of the touch point
         * @param left the x-coordinate of the left bound
         * @param top the y-coordinate of the top bound
         * @param right the x-coordinate of the right bound
         * @param bottom the y-coordinate of the bottom bound
         * @return true if the touch point is inside the bounding rectangle; false otherwise
         */
        private fun isInCenterTargetZone(
            x: Float, y: Float, left: Float, top: Float, right: Float, bottom: Float
        ): Boolean {
            return x > left && x < right && y > top && y < bottom
        }
    }
}