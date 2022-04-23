package com.pierdr.tramontana.ui

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.danikula.videocache.HttpProxyCacheServer
import com.pierdr.pierluigidallarosa.myactivity.R
import com.pierdr.tramontana.model.Directive
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.fragment_showtime.*
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import processing.android.PFragment

/**
 * Fragment to show when there's an active connection.
 *
 * Shows the Processing sketch by default; images and videos are handled outside the sketch.
 */
class ShowtimeFragment : Fragment(), ShowtimeView, KoinComponent {
    private val TAG = javaClass.simpleName

    private val presenter = ShowtimePresenter()
    private val sketch by lazy { Sketch() }
    private val brightnessController = BrightnessController(this)
    private val videoProxy: HttpProxyCacheServer by inject()

    init {
        lifecycle.addObserver(presenter)
    }

    override fun onAttach(context: Context) {
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

    override var imageVisible: Boolean = false
        set(value) {
            image.visibleOrInvisible = value
        }
    override var videoVisible: Boolean = false
        set(value) {
            video.visibleOrInvisible = value
        }

    private var View.visibleOrInvisible
        get() = visibility == View.VISIBLE
        set(value) {
            visibility = if (value) View.VISIBLE else View.INVISIBLE
        }

    override fun setColor(directive: Directive.SetColor) {
        sketch.setColor(directive.red, directive.green, directive.blue)
        brightnessController.set(directive.alpha / 255.0f)
    }

    override fun setBrightness(directive: Directive.SetBrightness) {
        brightnessController.set(directive.brightness)
    }

    override fun transitionColors(directive: Directive.TransitionColors) {
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

    override fun showImage(directive: Directive.ShowImage) {
        Picasso.get()
                .load(directive.url)
                .into(image)
    }

    override fun playVideo(url: String) {
        video.setZOrderOnTop(true)
        Log.d(TAG, "loading video $url")
        video.setOnPreparedListener {
            Log.d(TAG, "starting video $url")
            video.start()
        }
        video.setOnCompletionListener {
            presenter.onVideoEnded()
        }
        val proxyUrl = videoProxy.getProxyUrl(url)
        video.setVideoPath(proxyUrl)
    }

    override fun startTouchListening(multi: Boolean, drag: Boolean) = sketch.startTouchListening(multi, drag)
    override fun stopTouchListening() = sketch.stopTouchListening()
}

interface ShowtimeView {
    var imageVisible: Boolean
    var videoVisible: Boolean
    fun setColor(directive: Directive.SetColor)
    fun transitionColors(directive: Directive.TransitionColors)
    fun setBrightness(directive: Directive.SetBrightness)
    fun startTouchListening(multi: Boolean, drag: Boolean)
    fun stopTouchListening()
    fun showImage(directive: Directive.ShowImage)
    fun playVideo(url: String)
}

