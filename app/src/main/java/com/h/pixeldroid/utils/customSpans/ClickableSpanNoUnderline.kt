package com.h.pixeldroid.utils.customSpans

import android.text.TextPaint
import android.text.style.ClickableSpan

abstract class ClickableSpanNoUnderline : ClickableSpan() {
    override fun updateDrawState(ds: TextPaint) {
        super.updateDrawState(ds)
        ds.isUnderlineText = false
    }
}