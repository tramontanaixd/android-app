package com.pierdr.tramontana

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.OnLifecycleEvent
import android.util.Log
import kotlinx.coroutines.experimental.launch

class MainPresenter(
        private val view: MainView
) : LifecycleObserver {
    private val TAG = javaClass.simpleName
    private var currentServer: Server? = null

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun start() = launch {
        view.showReadyFragment()

        val server = Server()
        currentServer = server
        server.start()
        Log.d(TAG, "waiting for client sessions")
        for (clientSession in server.produceClientSessions()) {
            handleClientSession(clientSession)
        }
        Log.d(TAG, "no more client sessions")
    }

    private fun handleClientSession(session: ClientSession) {
        Log.d(TAG, "got client session $session")
        view.showShowtimeFragment()

        launch {
            Log.d(TAG, "waiting for directives")
            for (directive in session.produceDirectives()) {
                view.runDirective(directive)
            }
            Log.d(TAG, "session closed, no more directives")
            view.showReadyFragment()
        }

    }


    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun stop() {
        currentServer?.stop() ?: throw IllegalStateException("stop() with no current server")
        currentServer = null
    }
}