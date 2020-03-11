package com.h.pixeldroid

import com.h.pixeldroid.motions.OnSwipeListener
import org.junit.Assert.assertEquals
import org.junit.Test

class OnSwipeListenerUnitTest {

    @Test
    fun undefinedSwipeRightDoesNothingTest() {
        val swipeListener = OnSwipeListener(null)
        assertEquals(Unit, swipeListener.onSwipeRight())
    }

    @Test
    fun undefinedSwipeLeftDoesNothingTest() {
        val swipeListener = OnSwipeListener(null)
        assertEquals(Unit, swipeListener.onSwipeLeft())
    }
}