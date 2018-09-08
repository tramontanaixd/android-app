package com.pierdr.tramontana.ui

import android.animation.ValueAnimator
import android.arch.lifecycle.LifecycleObserver
import android.support.v4.app.Fragment

class BrightnessController(
        private val fragment: Fragment
) : LifecycleObserver {
    fun set(brightness: Float) {
        val window = fragment.activity?.window
                ?: throw IllegalStateException("setBrightness on no activity")
        val lp = window.attributes
        lp?.screenBrightness = brightness
        window.attributes = lp
    }

    fun fade(from: Float, to: Float, duration: Int) {
        with(ValueAnimator.ofFloat(from, to)) {
            this.duration = duration.toLong()
            addUpdateListener { set(it.animatedValue as Float) }
            start()
        }
    }
}
