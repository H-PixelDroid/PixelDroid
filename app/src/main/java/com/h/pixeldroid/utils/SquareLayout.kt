package com.h.pixeldroid.utils

import android.widget.RelativeLayout
import android.os.Build
import android.annotation.TargetApi
import android.content.Context
import android.util.AttributeSet

internal class SquareLayout(context: Context, attrs: AttributeSet) :
    RelativeLayout(context, attrs) {

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, widthMeasureSpec)
    }
}