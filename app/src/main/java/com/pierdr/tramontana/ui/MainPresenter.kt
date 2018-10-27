package com.pierdr.tramontana.ui

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.OnLifecycleEvent
import com.pierdr.tramontana.model.Dispatcher
import com.pierdr.tramontana.model.StartUi
import com.pierdr.tramontana.model.StopUi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import kotlin.coroutines.CoroutineContext


class MainPresenter : LifecycleObserver, KoinComponent, CoroutineScope {

    private val dispatcher: Dispatcher by inject()
    private var view: MainView? = null

    private lateinit var job: Job
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job


    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun start(owner: LifecycleOwner) {
        job = Job()
        view = owner as MainView
        view?.showReadyFragment()

        launch {
            dispatcher.start()
            for (uiDirective in dispatcher.produceUiDirectives()) {
                when (uiDirective) {
                    StartUi -> view?.showShowtimeFragment()
                    StopUi -> view?.showReadyFragment()
                }
            }
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun stop() {
        job.cancel()
        dispatcher.stop()
    }
}