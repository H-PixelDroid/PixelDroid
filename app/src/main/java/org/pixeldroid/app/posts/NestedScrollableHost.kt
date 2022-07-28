package org.pixeldroid.app.posts

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.GestureDetectorCompat
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.ORIENTATION_HORIZONTAL
import org.pixeldroid.app.utils.api.objects.Attachment
import kotlin.math.absoluteValue
import kotlin.math.sign

/**
 * Layout to wrap a scrollable component inside a ViewPager2. Provided as a solution to the problem
 * where pages of ViewPager2 have nested scrollable elements that scroll in the same direction as
 * ViewPager2. The scrollable element needs to be the immediate and only child of this host layout.
 *
 * This solution has limitations when using multiple levels of nested scrollable elements
 * (e.g. a horizontal RecyclerView in a vertical RecyclerView in a horizontal ViewPager2).
 */
class NestedScrollableHost(context: Context, attrs: AttributeSet? = null) :
    ConstraintLayout(context, attrs) {

    private var mDetector: GestureDetectorCompat
    private var touchSlop = 0
    private val parentViewPager: ViewPager2?
        get() {
            var v: View? = parent as? View
            while (v != null && v !is ViewPager2) {
                v = v.parent as? View
            }
            return v as? ViewPager2
        }


    var images: ArrayList<Attachment> = ArrayList()
    var doubleTapCallback: (() -> Unit)? = null

    private val child: View? get() = if (childCount > 0) getChildAt(0) else null

    init {
        touchSlop = ViewConfiguration.get(context).scaledTouchSlop
        mDetector = GestureDetectorCompat(context, MyGestureListener())
    }

    private fun canChildScroll(orientation: Int, delta: Float): Boolean {
        val direction = -delta.sign.toInt()
        return when (orientation) {
            0 -> child?.canScrollHorizontally(direction) ?: false
            1 -> child?.canScrollVertically(direction) ?: false
            else -> throw IllegalArgumentException()
        }
    }

    override fun onInterceptTouchEvent(e: MotionEvent): Boolean {
        mDetector.onTouchEvent(e)
        return super.onInterceptTouchEvent(e)
    }

    private inner class MyGestureListener : GestureDetector.SimpleOnGestureListener() {

        override fun onDown(e: MotionEvent): Boolean {
            val orientation = parentViewPager?.orientation ?: return true

            if (!canChildScroll(orientation, -1f) && !canChildScroll(orientation, 1f)) {
                return true
            }

            parent.requestDisallowInterceptTouchEvent(true)

            return true
        }

        override fun onDoubleTap(e: MotionEvent?): Boolean {
            doubleTapCallback?.invoke()
            return super.onDoubleTap(e)
        }

        override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
            // Disable opening AlbumActivity if the only image is a video (let the video open directly)
            if(images.size == 1 && images.first().type == Attachment.AttachmentType.video){
                return super.onSingleTapConfirmed(e)
            }
            val intent = Intent(context, AlbumActivity::class.java)

            intent.putExtra("images", images)
            intent.putExtra("index", (child as ViewPager2).currentItem)

            context.startActivity(intent)

            return super.onSingleTapConfirmed(e)
        }
        override fun onScroll(
            e1: MotionEvent,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            val orientation = parentViewPager?.orientation ?: return true

            val dx = e2.x - e1.x
            val dy = e2.y - e1.y
            val isVpHorizontal = orientation == ORIENTATION_HORIZONTAL

            // assuming ViewPager2 touch-slop is 2x touch-slop of child
            val scaledDx = dx.absoluteValue * if (isVpHorizontal) .5f / touchSlopModifier else 1f
            val scaledDy = dy.absoluteValue * if (isVpHorizontal) 1f else .5f / touchSlopModifier

            if (scaledDx > touchSlop || scaledDy > touchSlop) {

                if (isVpHorizontal == (scaledDy > scaledDx)) {
                    // Gesture is perpendicular, allow all parents to intercept
                    parent.requestDisallowInterceptTouchEvent(false)
                } else {
                    // Gesture is parallel, query child if movement in that direction is possible
                    if (canChildScroll(orientation, if (isVpHorizontal) dx else dy)) {
                        // Child can scroll, disallow all parents to intercept
                        parent.requestDisallowInterceptTouchEvent(true)
                    } else {
                        // Child cannot scroll, allow all parents to intercept
                        parent.requestDisallowInterceptTouchEvent(false)
                    }
                }
            }
            return super.onScroll(e1, e2, distanceX, distanceY)
        }
    }

    companion object {
        const val touchSlopModifier = 2
    }
}