package com.pierdr.tramontana

import android.app.Application
import android.content.Context
import android.hardware.camera2.CameraManager
import android.os.Vibrator
import com.bugfender.sdk.Bugfender
import com.danikula.videocache.HttpProxyCacheServer
import com.pierdr.pierluigidallarosa.myactivity.BuildConfig
import com.pierdr.tramontana.io.OscSender
import com.pierdr.tramontana.io.PowerMonitor
import com.pierdr.tramontana.io.sensor.Sensors
import com.pierdr.tramontana.io.websocket.WebsocketServer
import com.pierdr.tramontana.model.Event
import com.pierdr.tramontana.model.EventSink
import com.pierdr.tramontana.model.Server
import com.pierdr.tramontana.model.UserReporter
import com.pierdr.tramontana.ui.Flashlight
import com.pierdr.tramontana.ui.MainPresenter
import com.pierdr.tramontana.ui.ToastReporter
import org.koin.android.ext.android.startKoin
import org.koin.dsl.module.module
import java.io.File

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
    single { get<Context>().getSystemService(Context.CAMERA_SERVICE) as CameraManager }
    single { MainPresenter() }
    single<Server> { WebsocketServer() }
    single<UserReporter> { ToastReporter(get()) }
    single { Flashlight() }
    single { Sensors() }
    single {
        val applicationContext = get<Context>()
        HttpProxyCacheServer.Builder(applicationContext)
                .cacheDirectory(File(applicationContext.externalCacheDir, "video-cache"))
                .build()
    }
    single { PowerMonitor() }

    single { OscSender() }

    single<EventSink> {
        val server = get<Server>()
        object : EventSink {
            override fun onEvent(event: Event) {
                server.currentClientSession?.sendEvent(event)
                get<OscSender>().onEvent(event)
            }
        }
    }
}