package com.pierdr.pierluigidallarosa.myactivity.util

import android.view.View

import androidx.test.espresso.IdlingResource

class ViewVisibilityIdlingResource(
        private val view: View,
        private val expectedVisibility: Int = View.VISIBLE
) : IdlingResource {

    private var idle: Boolean = false
    private var resourceCallback: IdlingResource.ResourceCallback? = null

    override fun getName(): String {
        return ViewVisibilityIdlingResource::class.java.simpleName
    }

    override fun isIdleNow(): Boolean {
        idle = idle || view.visibility == expectedVisibility

        if (idle) {
            resourceCallback?.onTransitionToIdle()
        }

        return idle
    }

    override fun registerIdleTransitionCallback(resourceCallback: IdlingResource.ResourceCallback) {
        this.resourceCallback = resourceCallback
    }

}