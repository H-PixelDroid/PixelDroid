package com.h.pixeldroid.interfaces

interface EditImageFragmentListener {
    fun onBrightnessChange(brightness: Int)

    fun onSaturationChange(saturation: Int)

    fun onContrastChange(contrast: Int)

    fun onEditStarted()

    fun onEditCompleted()
}