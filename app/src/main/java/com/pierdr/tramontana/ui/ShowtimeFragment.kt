package com.pierdr.tramontana.ui

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.danikula.videocache.HttpProxyCacheServer
import com.pierdr.pierluigidallarosa.myactivity.R
import com.pierdr.tramontana.model.Directive
import com.pierdr.tramontana.model.Event
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

    override fun setColor(directive: Directive.SetColor) {
        contentToShow = ContentToShow.SolidColor
        sketch.setColor(directive.red, directive.green, directive.blue)
        brightnessController.set(directive.alpha / 255.0f)
    }

    override fun setBrightness(directive: Directive.SetBrightness) {
        brightnessController.set(directive.brightness)
    }

    override fun transitionColors(directive: Directive.TransitionColors) {
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

    override fun showImage(directive: Directive.ShowImage) {
        contentToShow = ContentToShow.Image
        Picasso.get()
                .load(directive.url)
                .into(image)
    }

    override fun playVideo(directive: Directive.PlayVideo) {
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

    override fun startTouchListening(multi: Boolean, drag: Boolean) = sketch.startTouchListening(multi, drag)
    override fun stopTouchListening() = sketch.stopTouchListening()
}

interface ShowtimeView {
    fun setColor(directive: Directive.SetColor)
    fun transitionColors(directive: Directive.TransitionColors)
    fun setBrightness(directive: Directive.SetBrightness)
    fun startTouchListening(multi: Boolean, drag: Boolean)
    fun stopTouchListening()
    fun showImage(directive: Directive.ShowImage)
    fun playVideo(directive: Directive.PlayVideo)
}

