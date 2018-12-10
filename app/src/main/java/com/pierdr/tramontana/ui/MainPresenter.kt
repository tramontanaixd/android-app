package com.pierdr.tramontana.ui

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.OnLifecycleEvent
import android.util.Log
import com.pierdr.tramontana.model.ClientSession
import com.pierdr.tramontana.model.Server
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import kotlin.coroutines.CoroutineContext


class MainPresenter : LifecycleObserver, KoinComponent, CoroutineScope {
    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    private val TAG = javaClass.simpleName
    private val server: Server by inject()
    private var view: MainView? = null

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun start(owner: LifecycleOwner) = launch {
        view = owner as MainView
        view?.showReadyFragment()

        server.start()
        Log.d(TAG, "waiting for client sessions")
        for (clientSession in server.produceClientSessions()) {
            handleClientSession(clientSession)
        }
        Log.d(TAG, "no more client sessions")
    }

    private fun handleClientSession(session: ClientSession) {
        Log.i(TAG, "got client session $session")
        view?.showShowtimeFragment() ?: return

        launch {
            Log.d(TAG, "waiting for directives")
            val directiveSubscription = session.subscribeToDirectives()
            for (directive in directiveSubscription) {
                // just wait for the channel to close, to show the ready fragment again
            }
            Log.d(TAG, "session or view closed")
            view?.showReadyFragment()
        }

    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun stop() {
        job.cancel()
        server.stop()
    }
}