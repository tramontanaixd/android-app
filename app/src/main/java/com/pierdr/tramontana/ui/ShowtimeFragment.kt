package com.pierdr.tramontana.ui

import android.content.Context
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Bundle
import android.os.Vibrator
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.danikula.videocache.HttpProxyCacheServer
import com.pierdr.pierluigidallarosa.myactivity.R
import com.pierdr.tramontana.io.Sensors
import com.pierdr.tramontana.model.Directive
import com.pierdr.tramontana.model.Event
import com.pierdr.tramontana.model.EventSink
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.fragment_showtime.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import processing.android.PFragment
import java.io.File

/**
 * Fragment to show when there's an active connection.
 *
 * Shows the Processing sketch by default; images and videos are handled outside the sketch.
 *
 * A user of this fragment *must* call set the [eventSink] before attaching it via a FragmentManager.
 */
class ShowtimeFragment : Fragment(), EventSink {
    private val TAG = javaClass.simpleName

    private enum class ContentToShow {
        SolidColor,
        Image,
        Video
    }

    private val applicationContext: Context
        get() = context!!.applicationContext

    private val vibrator by lazy { applicationContext.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator }
    private val cameraManager by lazy { applicationContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager }
    private val userReporter by lazy { ToastReporter(context!!) }
    private val sensors by lazy { Sensors(applicationContext, eventSink, userReporter) }
    private val sketch by lazy { Sketch(this) }

    private val videoProxy by lazy {
        HttpProxyCacheServer.Builder(applicationContext)
                .cacheDirectory(File(applicationContext.externalCacheDir, "video-cache"))
                .build()
    }

    lateinit var eventSink: EventSink

    private var contentToShow: ContentToShow = ContentToShow.SolidColor
        set(value) {
            Log.d(TAG, "set contentToShow current=$field new=$value")
            image.visibility = if (value == ContentToShow.Image) View.VISIBLE else View.INVISIBLE
            video.visibility = if (value == ContentToShow.Video) View.VISIBLE else View.INVISIBLE
            field = value
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

    override fun onEvent(event: Event) {
        eventSink.onEvent(event)
    }

    fun runDirective(directive: Directive) {
        Log.d(TAG, "would run directive $directive")
        launch(UI) {
            runDirectiveOnUiThread(directive)
        }
    }

    private fun runDirectiveOnUiThread(directive: Directive) {
        Log.d(TAG, "got directive $directive")
        when (directive) {
            is Directive.MakeVibrate -> vibrator.vibrate(100)
            is Directive.SetColor -> {
                contentToShow = ContentToShow.SolidColor
                sketch.setColor(
                        directive.red,
                        directive.green,
                        directive.blue)
                setBrightness(directive.alpha / 255.0f)
            }
            is Directive.SetLed -> setFlashLight(directive.intensity)
            is Directive.ShowImage -> onShowImage(directive)
            is Directive.PlayVideo -> onPlayVideo(directive)
            is Directive.RegisterTouch -> sketch.startTouchListening(directive.multi, directive.drag)
            is Directive.ReleaseTouch -> sketch.stopTouchListening()
            is Directive.RegisterDistance -> sensors.startSensor(Sensors.Type.PROXIMITY)
            is Directive.ReleaseDistance -> sensors.stopSensor(Sensors.Type.PROXIMITY)
            is Directive.RegisterAttitude -> sensors.startSensor(Sensors.Type.ROTATION, (1_000_000 / directive.updateRate).toInt())
            is Directive.ReleaseAttitude -> sensors.stopSensor(Sensors.Type.ROTATION)
            else -> throw UnsupportedOperationException("unsupported directive $directive")
        }.javaClass // .javaClass is added to make an "exhaustive when", see https://youtrack.jetbrains.com/issue/KT-12380#focus=streamItem-27-2727497-0-0
    }

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
            eventSink.onEvent(Event.VideoEnded)
        }
        val proxyUrl = videoProxy.getProxyUrl(url)
        video.setVideoPath(proxyUrl)
    }

    private fun setBrightness(brightness: Float) {
        val window = activity?.window ?: throw IllegalStateException("setBrightness on no activity")
        val lp = window.attributes
        lp?.screenBrightness = brightness
        window.attributes = lp
    }

    private fun setFlashLight(value: Float) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            userReporter.showWarning("Camera not available, unable to set flashlight")
            return
        } else {
            val cameraId: String = cameraManager.cameraIdList[0]
            cameraManager.setTorchMode(cameraId, value > 0)
        }
    }
}

interface UserReporter {
    fun showWarning(message: String)
}

class ToastReporter(
        private val context: Context
) : UserReporter {
    override fun showWarning(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
}