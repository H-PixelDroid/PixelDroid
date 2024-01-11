package org.pixeldroid.app.stories

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable

/**
 * Copied & adapted from AntennaPod's EchoProgress class because it looked great and is very simple
 * AntennaPod/ui/echo/src/main/java/de/danoeh/antennapod/ui/echo/EchoProgress.java
 */
class StoryProgress(private val numStories: Int) : Drawable() {
    private val paint: Paint = Paint().apply {
        flags = Paint.ANTI_ALIAS_FLAG
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        color = -0x1
    }

    var progress = 0f
    var currentStory: Int = 0

    override fun draw(canvas: Canvas) {
        paint.strokeWidth = 0.5f * bounds.height()
        val y = 0.5f * bounds.height()
        val sectionWidth = 1.0f * bounds.width() / numStories
        val sectionPadding = 0.03f * sectionWidth
        // Iterate over stories
        for (i in 0 until numStories) {
            if (i  < currentStory) {
                // If current drawing position is smaller than current story, the paint we will use
                // should be opaque: this story is already "seen"
                paint.alpha = 255
            } else {
                // Otherwise it should be somewhat transparent, denoting it is not yet seen
                paint.alpha = 100
            }
            // Draw an entire line with the paint, for now ignoring partial progress within the
            // current story
            canvas.drawLine(
                i * sectionWidth + sectionPadding,
                y,
                (i + 1) * sectionWidth - sectionPadding,
                y,
                paint
            )
            // If current position is equal to progress, we are drawing the current story. Thus we
            // should account for partial progress and paint the beginning of the line opaquely
            if (i == currentStory) {
                paint.alpha = 255
                canvas.drawLine(
                    currentStory * sectionWidth + sectionPadding,
                    y,
                    currentStory * sectionWidth + sectionPadding + progress * (sectionWidth - 2 * sectionPadding),
                    y,
                    paint
                )
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int {
        return PixelFormat.TRANSLUCENT
    }

    override fun setAlpha(alpha: Int) {}
    override fun setColorFilter(cf: ColorFilter?) {}
}

