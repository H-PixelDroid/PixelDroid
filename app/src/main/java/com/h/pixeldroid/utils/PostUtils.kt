package com.h.pixeldroid.utils

import android.graphics.ColorMatrix

abstract class PostUtils {
    companion object {

        fun censorColorMatrix(): ColorMatrix {
            val array: FloatArray = floatArrayOf( 0f, 0f, 0f, 0f, 0f,  0f, 0f, 0f, 0f, 0f,  0f, 0f, 0f, 0f, 0f,  0f, 0f, 0f, 1f, 0f )
            return ColorMatrix(array)
        }

        fun uncensorColorMatrix(): ColorMatrix {
            return ColorMatrix()
        }
    }
}