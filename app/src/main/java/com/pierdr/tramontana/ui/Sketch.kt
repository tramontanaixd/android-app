package com.pierdr.tramontana.ui

import com.pierdr.tramontana.model.EventSink
import com.pierdr.tramontana.ui.touch.*
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import processing.core.PApplet
import java.util.*

class Sketch : PApplet(), KoinComponent {
    private val eventSink: EventSink by inject()
    private var backgroundColor = color(255)
    private var touchHandler: TouchHandler = NoTouchHandler()
    private val preDrawTasks = HashSet<Runnable>()

    override fun settings() {
        fullScreen()
    }

    override fun draw() {
        for (preDrawTask in ArrayList(preDrawTasks)) {
            preDrawTask.run()
        }

        background(backgroundColor)
    }

    fun setColor(red: Int, green: Int, blue: Int) {
        backgroundColor = color(red, green, blue)
    }

    fun transitionColors(fromRed: Int, fromGreen: Int, fromBlue: Int, toRed: Int, toGreen: Int, toBlue: Int, duration: Int) {
        clearPendingBackgroundTransitionTasks()
        preDrawTasks.add(BackgroundTransitionTask(color(fromRed, fromGreen, fromBlue), color(toRed, toGreen, toBlue), duration))
    }

    private fun clearPendingBackgroundTransitionTasks() {
        val preDrawTaskIterator = preDrawTasks.iterator()
        while (preDrawTaskIterator.hasNext()) {
            val preDrawTask = preDrawTaskIterator.next()
            if (preDrawTask is BackgroundTransitionTask) {
                preDrawTaskIterator.remove()
            }
        }
    }

    fun startTouchListening(multi: Boolean, drag: Boolean) {
        touchHandler = if (!multi && !drag) {
            SingleTouchHandler(eventSink)
        } else if (multi && drag) {
            MultiDragTouchHandler(eventSink)
        } else if (multi) {
            MultiTouchHandler(eventSink)
        } else {
            SingleDragTouchHandler(eventSink)
        }
    }

    fun stopTouchListening() {
        touchHandler = NoTouchHandler()
    }

    override fun touchStarted() {
        touchHandler.onTouchStart(touches)
    }

    override fun touchMoved() {
        touchHandler.onTouchMove(touches)
    }

    override fun touchEnded() {
        touchHandler.onTouchEnd(touches)
    }

    private inner class BackgroundTransitionTask internal constructor(private val fromColor: Int, private val toColor: Int, private val duration: Int) : Runnable {
        private val startTime: Long = millis().toLong()

        override fun run() {
            val timeAlpha = PApplet.map(millis().toFloat(), startTime.toFloat(), (startTime + duration).toFloat(), 0f, 1f)
            val colorAlpha = PApplet.constrain(timeAlpha, 0f, 1f)
            backgroundColor = lerpColor(fromColor, toColor, colorAlpha)

            if (timeAlpha > 1) {
                preDrawTasks.remove(this)
            }
        }
    }
}
