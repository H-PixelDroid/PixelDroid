package com.h.pixeldroid.postCreation

import android.content.Context
import android.util.AttributeSet
import android.widget.RelativeLayout

internal class SquareLayout(context: Context, attrs: AttributeSet) :
    RelativeLayout(context, attrs) {

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, widthMeasureSpec)
    }
}