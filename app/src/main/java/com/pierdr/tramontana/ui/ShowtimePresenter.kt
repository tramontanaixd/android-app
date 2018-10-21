package com.pierdr.tramontana.ui

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.OnLifecycleEvent
import android.os.Vibrator
import android.util.Log
import com.pierdr.tramontana.io.OscSender
import com.pierdr.tramontana.io.PowerMonitor
import com.pierdr.tramontana.io.sensor.*
import com.pierdr.tramontana.model.Directive
import com.pierdr.tramontana.model.Event
import com.pierdr.tramontana.model.EventSink
import com.pierdr.tramontana.model.Server
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.launch
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import kotlin.math.roundToInt

class ShowtimePresenter : LifecycleObserver, KoinComponent {
    private val tag = "DirectiveListener"
    private val server: Server by inject()
    private val sensors: Sensors by inject()
    private val vibrator: Vibrator by inject()
    private val flashlight: Flashlight by inject()
    private val powerMonitor: PowerMonitor by inject()
    private val eventSink: EventSink by inject()
    private val oscSender: OscSender by inject()
    private var directivesSubscription: ReceiveChannel<Directive>? = null

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
        directivesSubscription?.cancel()
        sensors.stopAll()
    }

    private fun runDirectiveOnUiThread(directive: Directive) {
        val viewLocal = view ?: return
        when (directive) {
            is Directive.MakeVibrate -> vibrator.vibrate(100)
            is Directive.SetColor -> {
                contentToShow = ContentToShow.SolidColor
                viewLocal.setColor(directive)
            }
            is Directive.TransitionColors -> {
                contentToShow = ContentToShow.SolidColor
                viewLocal.transitionColors(directive)
            }
            is Directive.SetBrightness -> viewLocal.setBrightness(directive)
            is Directive.SetLed -> flashlight.set(directive.intensity)
            is Directive.PulseLed -> flashlight.pulse(directive.numberOfPulses, directive.durationMillis)
            is Directive.ShowImage -> {
                contentToShow = ContentToShow.Image
                viewLocal.showImage(directive)
            }
            is Directive.PlayVideo -> {
                contentToShow = ContentToShow.Video
                viewLocal.playVideo(directive.url)
            }
            is Directive.PlayAudio -> {
                contentToShow = ContentToShow.Video
                viewLocal.playVideo(directive.url)
            }
            is Directive.RegisterTouch -> viewLocal.startTouchListening(directive.multi, directive.drag)
            is Directive.ReleaseTouch -> viewLocal.stopTouchListening()
            is Directive.RegisterDistance -> sensors.startSensor(Proximity::class)
            is Directive.ReleaseDistance -> sensors.stopSensor(Proximity::class)
            is Directive.RegisterAttitude -> sensors.startSensor(Attitude::class, directive.updateRate.toMicros())
            is Directive.ReleaseAttitude -> sensors.stopSensor(Attitude::class)
            is Directive.RegisterOrientation -> sensors.startSensor(Orientation::class)
            is Directive.ReleaseOrientation -> sensors.stopSensor(Orientation::class)
            is Directive.RegisterMagnetometer -> sensors.startSensor(Magnetometer::class)
            is Directive.ReleaseMagnetometer -> sensors.stopSensor(Magnetometer::class)
            is Directive.GetBattery -> powerMonitor.sendBatteryLevel()
            is Directive.RegisterPowerSource -> sensors.startSensor(PowerSource::class)
            is Directive.ReleasePowerSource -> sensors.stopSensor(PowerSource::class)
            is Directive.RegisterAudioJack -> sensors.startSensor(AudioJack::class)
            is Directive.ReleaseAudioJack -> sensors.stopSensor(AudioJack::class)
            is Directive.SendAttitudeToOSC -> startAttitudeToOSC(directive)
            is Directive.StopAttitudeToOSC -> stopAttitudeToOSC()
            is Directive.SendTouchToOSC -> startTouchToOSC(directive, viewLocal)
            is Directive.StopTouchToOSC -> stopTouchToOSC(viewLocal)
        }.javaClass // .javaClass is added to make an "exhaustive when", see https://youtrack.jetbrains.com/issue/KT-12380#focus=streamItem-27-2727497-0-0
    }

    private fun Float.toMicros() = (1_000_000 / this).toInt()

    fun onVideoEnded() {
        eventSink.onEvent(Event.VideoEnded)
    }

    private fun startAttitudeToOSC(directive: Directive.SendAttitudeToOSC) {
        sensors.stopSensor(Attitude::class)
        oscSender.startAttitudeSend(directive.address, directive.port)
        sensors.startSensor(Attitude::class, (1_000_000f / directive.updateRate).roundToInt())
    }

    private fun stopAttitudeToOSC() {
        oscSender.stopAttitudeSend()
        sensors.stopSensor(Attitude::class)
    }

    private fun startTouchToOSC(directive: Directive.SendTouchToOSC, view: ShowtimeView) {
        view.startTouchListening(true, true)
        oscSender.startTouchSend(directive.address, directive.port)
    }

    private fun stopTouchToOSC(view: ShowtimeView) {
        oscSender.stopTouchSend()
        view.stopTouchListening()
    }

    private var contentToShow: ContentToShow = ContentToShow.SolidColor
        set(value) {
            view?.imageVisible = value == ContentToShow.Image
            view?.videoVisible = value == ContentToShow.Video
            field = value
        }

    private enum class ContentToShow {
        SolidColor,
        Image,
        Video
    }
}