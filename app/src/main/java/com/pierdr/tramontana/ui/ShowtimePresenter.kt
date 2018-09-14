package com.pierdr.tramontana.ui

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.OnLifecycleEvent
import android.os.Vibrator
import android.util.Log
import com.pierdr.tramontana.io.*
import com.pierdr.tramontana.model.Directive
import com.pierdr.tramontana.model.Event
import com.pierdr.tramontana.model.EventSink
import com.pierdr.tramontana.model.Server
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.channels.SubscriptionReceiveChannel
import kotlinx.coroutines.experimental.launch
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject

class ShowtimePresenter : LifecycleObserver, KoinComponent, EventSink {
    private val tag = "DirectiveListener"
    private val server: Server by inject()
    private val sensors: Sensors by inject()
    private val vibrator: Vibrator by inject()
    private val flashlight: Flashlight by inject()
    private var directivesSubscription: SubscriptionReceiveChannel<Directive>? = null

    var view: ShowtimeView? = null

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun onStart(owner: LifecycleOwner) {
        view = owner as ShowtimeView

        launch {
            Log.d(tag, "waiting for directives")
            // TODO handle no-current-session
            val subscription = server.currentClientSession!!.subscribeToDirectives()
            directivesSubscription = subscription
            for (directive in subscription) {
                launch(UI) {
                    runDirectiveOnUiThread(directive)
                }
            }
            Log.d(tag, "no more directives")
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun onStop() {
        directivesSubscription?.close()
        sensors.stopAll()
    }

    override fun onEvent(event: Event) {
        server.currentClientSession?.sendEvent(event)
    }

    private fun runDirectiveOnUiThread(directive: Directive) {
        val viewLocal = view ?: return
        when (directive) {
            is Directive.MakeVibrate -> vibrator.vibrate(100)
            is Directive.SetColor -> viewLocal.setColor(directive)
            is Directive.TransitionColors -> viewLocal.transitionColors(directive)
            is Directive.SetBrightness -> viewLocal.setBrightness(directive)
            is Directive.SetLed -> flashlight.set(directive.intensity)
            is Directive.PulseLed -> flashlight.pulse(directive.numberOfPulses, directive.durationMillis)
            is Directive.ShowImage -> viewLocal.showImage(directive)
            is Directive.PlayVideo -> viewLocal.playVideo(directive)
            is Directive.RegisterTouch -> viewLocal.startTouchListening(directive.multi, directive.drag)
            is Directive.ReleaseTouch -> viewLocal.stopTouchListening()
            is Directive.RegisterDistance -> sensors.startSensor(Proximity::class)
            is Directive.ReleaseDistance -> sensors.stopSensor(Proximity::class)
            is Directive.RegisterAttitude -> sensors.startSensor(Attitude::class, directive.updateRate.toMicros())
            is Directive.ReleaseAttitude -> sensors.stopSensor(Attitude::class)
            is Directive.RegisterOrientation -> sensors.startSensor(Orientation::class)
            is Directive.ReleaseOrientation -> sensors.stopSensor(Orientation::class)
            is Directive.RegisterMagnetometer -> sensors.startSensor(Magnetometer::class, directive.updateRate.toMicros())
            is Directive.ReleaseMagnetometer -> sensors.stopSensor(Magnetometer::class)
        }.javaClass // .javaClass is added to make an "exhaustive when", see https://youtrack.jetbrains.com/issue/KT-12380#focus=streamItem-27-2727497-0-0
    }

    private fun Float.toMicros() = (1_000_000 / this).toInt()
}