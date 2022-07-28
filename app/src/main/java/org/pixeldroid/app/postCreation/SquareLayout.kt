package org.pixeldroid.app.postCreation

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout

internal class SquareLayout(context: Context, attrs: AttributeSet) :
    FrameLayout(context, attrs) {

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, widthMeasureSpec)
    }
}