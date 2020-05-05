package com.h.pixeldroid.interfaces

import android.content.Context
import androidx.fragment.app.Fragment

interface EditImageFragmentListener {
    fun onBrightnessChange(brightness: Int)

    fun onSaturationChange(saturation: Float)

    fun onContrastChange(contrast: Float)

    fun startCrop(frag: Fragment, context: Context)

    fun onEditStarted()

    fun onEditCompleted()
}