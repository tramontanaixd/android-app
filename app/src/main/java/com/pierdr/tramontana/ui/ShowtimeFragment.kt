package com.pierdr.tramontana.ui

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.OnLifecycleEvent
import android.content.Context
import android.os.Bundle
import android.os.Vibrator
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.danikula.videocache.HttpProxyCacheServer
import com.pierdr.pierluigidallarosa.myactivity.R
import com.pierdr.tramontana.io.*
import com.pierdr.tramontana.model.Directive
import com.pierdr.tramontana.model.Event
import com.pierdr.tramontana.model.EventSink
import com.pierdr.tramontana.model.Server
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.fragment_showtime.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.channels.SubscriptionReceiveChannel
import kotlinx.coroutines.experimental.launch
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import processing.android.PFragment

/**
 * Fragment to show when there's an active connection.
 *
 * Shows the Processing sketch by default; images and videos are handled outside the sketch.
 */
class ShowtimeFragment : Fragment(), KoinComponent {
    private val TAG = javaClass.simpleName

    private enum class ContentToShow {
        SolidColor,
        Image,
        Video
    }

    private val presenter = ShowtimePresenter()
    private val sketch by lazy { Sketch(presenter) }
    private val brightnessController = BrightnessController(this)
    private val videoProxy: HttpProxyCacheServer by inject()

    private var contentToShow: ContentToShow = ContentToShow.SolidColor
        set(value) {
            Log.d(TAG, "set contentToShow current=$field new=$value")
            image.visibility = if (value == ContentToShow.Image) View.VISIBLE else View.INVISIBLE
            video.visibility = if (value == ContentToShow.Video) View.VISIBLE else View.INVISIBLE
            field = value
        }

    init {
        lifecycle.addObserver(presenter)
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        lifecycle.addObserver(brightnessController)
    }

    override fun onDetach() {
        lifecycle.removeObserver(brightnessController)
        super.onDetach()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_showtime, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        childFragmentManager.beginTransaction()
                .add(R.id.sketch_container, PFragment(sketch))
                .commit()
    }

    private fun onSetColor(directive: Directive.SetColor) {
        contentToShow = ContentToShow.SolidColor
        sketch.setColor(directive.red, directive.green, directive.blue)
        brightnessController.set(directive.alpha / 255.0f)
    }

    private fun onSetBrightness(directive: Directive.SetBrightness) {
        brightnessController.set(directive.brightness)
    }

    private fun onTransitionColors(directive: Directive.TransitionColors) {
        contentToShow = ContentToShow.SolidColor
        sketch.transitionColors(
                directive.fromRed,
                directive.fromGreen,
                directive.fromBlue,
                directive.toRed,
                directive.toGreen,
                directive.toBlue,
                directive.duration
        )
        brightnessController.fade(
                directive.fromAlpha / 255.0f,
                directive.toAlpha / 255.0f,
                directive.duration)
    }

    private fun Float.toMicros() = (1_000_000 / this).toInt()

    private fun onShowImage(directive: Directive.ShowImage) {
        contentToShow = ContentToShow.Image
        Picasso.get()
                .load(directive.url)
                .into(image)
    }

    private fun onPlayVideo(directive: Directive.PlayVideo) {
        val url = directive.url

        video.setZOrderOnTop(true)
        contentToShow = ContentToShow.Video
        Log.d(TAG, "loading video $url")
        video.setOnPreparedListener {
            Log.d(TAG, "starting video $url")
            video.start()
        }
        video.setOnCompletionListener {
            presenter.onEvent(Event.VideoEnded)
        }
        val proxyUrl = videoProxy.getProxyUrl(url)
        video.setVideoPath(proxyUrl)
    }

    // TODO make this non-inner
    inner class ShowtimePresenter : LifecycleObserver, KoinComponent, EventSink {
        private val tag = "DirectiveListener"
        private val server: Server by inject()
        private val sensors: Sensors by inject()
        private val vibrator: Vibrator by inject()
        private val flashlight: Flashlight by inject()
        private var directivesSubscription: SubscriptionReceiveChannel<Directive>? = null

        @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
        fun onStart() {
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
            when (directive) {
                is Directive.MakeVibrate -> vibrator.vibrate(100)
                is Directive.SetColor -> onSetColor(directive)
                is Directive.TransitionColors -> onTransitionColors(directive)
                is Directive.SetBrightness -> onSetBrightness(directive)
                is Directive.SetLed -> flashlight.set(directive.intensity)
                is Directive.ShowImage -> onShowImage(directive)
                is Directive.PlayVideo -> onPlayVideo(directive)
                is Directive.RegisterTouch -> sketch.startTouchListening(directive.multi, directive.drag)
                is Directive.ReleaseTouch -> sketch.stopTouchListening()
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

    }
}
