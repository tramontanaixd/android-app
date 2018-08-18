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
import com.pierdr.pierluigidallarosa.myactivity.R
import com.pierdr.pierluigidallarosa.myactivity.Sketch
import com.pierdr.tramontana.model.Directive
import com.pierdr.tramontana.model.Event
import com.pierdr.tramontana.model.EventSink
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import processing.android.PFragment

/**
 * Fragment to show when there's an active connection.
 *
 * Shows the Processing sketch by default; video are handled outside the sketch.
 *
 * A user of this fragment *must* call set the [eventSink] for events coming from the sketch.
 */
class ShowtimeFragment : Fragment(), EventSink {
    private val TAG = javaClass.simpleName

    private lateinit var sketch: Sketch
    private lateinit var userReporter: UserReporter
    private lateinit var vibrator: Vibrator
    private lateinit var cameraManager: CameraManager

    lateinit var eventSink: EventSink

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_showtime, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val applicationContext = context!!.applicationContext
        vibrator = applicationContext.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        cameraManager = applicationContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager

        userReporter = ToastReporter(context!!)

        sketch = Sketch(this, userReporter)
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
                sketch.setColor(
                        directive.red,
                        directive.green,
                        directive.blue)
                setBrightness(directive.alpha / 255.0f)
            }
            is Directive.SetLed -> setFlashLight(directive.intensity)
            is Directive.ShowImage -> sketch.showImage(directive.url)
//            is Directive.PlayVideo -> sketch.playVideo(directive.url)
            is Directive.RegisterTouch -> sketch.startTouchListening(directive.multi, directive.drag)
            is Directive.ReleaseTouch -> sketch.stopTouchListening()
            is Directive.RegisterDistance -> sketch.startDistanceSensing()
            is Directive.ReleaseDistance -> sketch.stopDistanceSensing()
            is Directive.RegisterAttitude -> sketch.startAttitudeSensing(directive.updateRate)
            is Directive.ReleaseAttitude -> sketch.stopAttitudeSensing()
            else -> throw UnsupportedOperationException("unsupported directive $directive")
        }.javaClass // .javaClass is added to make an "exhaustive when", see https://youtrack.jetbrains.com/issue/KT-12380#focus=streamItem-27-2727497-0-0
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