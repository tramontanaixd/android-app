package com.pierdr.tramontana

import android.app.Application
import com.bugfender.sdk.Bugfender
import com.pierdr.pierluigidallarosa.myactivity.BuildConfig

class App : Application() {
    override fun onCreate() {
        super.onCreate()

        initBugfender()
    }

    @Suppress("SENSELESS_COMPARISON")
    fun initBugfender() {
        if (BuildConfig.BUGFENDER_API_KEY == null) {
            return
        }

        Bugfender.init(this, BuildConfig.BUGFENDER_API_KEY, BuildConfig.DEBUG)
        Bugfender.enableLogcatLogging()
        Bugfender.enableUIEventLogging(this)
    }
}