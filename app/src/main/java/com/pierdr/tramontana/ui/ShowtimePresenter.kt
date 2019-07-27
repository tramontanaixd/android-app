package com.pierdr.tramontana.ui

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.OnLifecycleEvent
import android.util.Log
import com.pierdr.tramontana.model.Directive
import com.pierdr.tramontana.model.Dispatcher
import com.pierdr.tramontana.model.Event
import com.pierdr.tramontana.model.EventSink
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.launch
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import kotlin.coroutines.CoroutineContext

class ShowtimePresenter : LifecycleObserver, KoinComponent, CoroutineScope {
    private val tag = "DirectiveListener"
    private val eventSink: EventSink by inject()
    private val dispatcher: Dispatcher by inject()
    private var directivesSubscription: ReceiveChannel<Directive.NeedsUi>? = null

    private lateinit var job: Job
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    private var view: ShowtimeView? = null

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun onStart(owner: LifecycleOwner) {
        job = Job()
        view = owner as ShowtimeView

        launch {
            Log.d(tag, "waiting for directives")
            val subscription = dispatcher.produceUiDirectives()
            directivesSubscription = subscription
            for (directive in subscription) {
                val viewLocal = owner
                when (directive) {
                    is Directive.SetBrightness -> viewLocal.setBrightness(directive)
                    is Directive.SetColor -> {
                        contentToShow = ContentToShow.SolidColor
                        viewLocal.setColor(directive)
                    }
                    is Directive.TransitionColors -> {
                        contentToShow = ContentToShow.SolidColor
                        viewLocal.transitionColors(directive)
                    }
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
                }
            }
            Log.d(tag, "no more directives")
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun onStop() {
        directivesSubscription?.cancel()
        job.cancel()
    }

    fun onVideoEnded() {
        eventSink.onEvent(Event.VideoEnded)
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