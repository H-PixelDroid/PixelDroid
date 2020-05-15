package com.h.pixeldroid.interfaces

interface EditImageFragmentListener {
    fun onBrightnessChange(brightness: Int)

    fun onSaturationChange(saturation: Float)

    fun onContrastChange(contrast: Float)

    fun onEditStarted()

    fun onEditCompleted()
}