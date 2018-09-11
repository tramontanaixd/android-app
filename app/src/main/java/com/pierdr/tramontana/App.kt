package com.pierdr.tramontana

import android.app.Application
import android.content.Context
import android.os.Vibrator
import com.bugfender.sdk.Bugfender
import com.pierdr.pierluigidallarosa.myactivity.BuildConfig
import com.pierdr.tramontana.io.Sensors
import com.pierdr.tramontana.io.websocket.WebsocketServer
import com.pierdr.tramontana.model.Server
import com.pierdr.tramontana.model.UserReporter
import com.pierdr.tramontana.ui.MainPresenter
import com.pierdr.tramontana.ui.ToastReporter
import org.koin.android.ext.android.startKoin
import org.koin.dsl.module.module

class App : Application() {
    override fun onCreate() {
        super.onCreate()

        initBugfender()

        startKoin(this, listOf(appModule()))
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

fun appModule() = module {
    single { get<Context>().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator }

    single { MainPresenter() }

    single<Server> { WebsocketServer() }

    single<UserReporter> { ToastReporter(get()) }

    single { Sensors() }
}