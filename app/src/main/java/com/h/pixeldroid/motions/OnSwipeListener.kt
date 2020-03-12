package com.h.pixeldroid.motions

import android.content.Context
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import kotlin.math.abs

const val SWIPE_DISTANCE_THRESHOLD: Int = 100
const val SWIPE_VELOCITY_THRESHOLD: Int = 100

/**
 * Detects left and right swipes across a view.
 *
 * inspired from https://stackoverflow.com/questions/4139288/android-how-to-handle-right-to-left-swipe-gestures
 */
open class OnSwipeListener(context: Context?) : OnTouchListener {

    private val gestureDetector: GestureDetector = GestureDetector(context, GestureListener())

    override fun onTouch(v: View?, event: MotionEvent?): Boolean =
        gestureDetector.onTouchEvent(event)

    // redefining gesture listener to call our custom functions
    private inner class GestureListener : SimpleOnGestureListener() {

        override fun onDown(e: MotionEvent): Boolean = true

        override fun onFling(
            e1: MotionEvent,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            val distanceX = e2.x - e1.x
            val distanceY = e2.y - e1.y
            if (abs(distanceX) > abs(distanceY) // swipe on the side and not up or down
                && abs(distanceX) > SWIPE_DISTANCE_THRESHOLD
                && abs(velocityX) > SWIPE_VELOCITY_THRESHOLD
            ) {
                if (distanceX > 0) onSwipeRight() else onSwipeLeft()
                return true
            }
            return false
        }

    }

    open fun onSwipeLeft() {}

    open fun onSwipeRight() {}
}