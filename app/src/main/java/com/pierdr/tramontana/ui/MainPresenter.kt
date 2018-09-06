package com.pierdr.tramontana.ui

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.OnLifecycleEvent
import android.util.Log
import com.pierdr.tramontana.io.websocket.WebsocketServer
import com.pierdr.tramontana.model.ClientSession
import com.pierdr.tramontana.model.Event
import com.pierdr.tramontana.model.EventSink
import com.pierdr.tramontana.model.Server
import kotlinx.coroutines.experimental.launch


class MainPresenter(
        private val view: MainView
) : LifecycleObserver, EventSink {

    private val TAG = javaClass.simpleName
    private var currentServer: Server? = null
    private var currentSession: ClientSession? = null

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun start() = launch {
        view.showReadyFragment()

        val server: Server = WebsocketServer()
        currentServer = server
        server.start()
        Log.d(TAG, "waiting for client sessions")
        for (clientSession in server.produceClientSessions()) {
            handleClientSession(clientSession)
        }
        Log.d(TAG, "no more client sessions")
    }

    private fun handleClientSession(session: ClientSession) {
        Log.i(TAG, "got client session $session")
        currentSession = session
        view.showShowtimeFragment()

        launch {
            Log.d(TAG, "waiting for directives")
            for (directive in session.produceDirectives()) {
                view.runDirective(directive)
            }
            Log.d(TAG, "session closed, no more directives")
            currentSession = null
            view.showReadyFragment()
        }

    }

    override fun onEvent(event: Event) {
        Log.v(TAG, "got event $event")
        currentSession?.sendEvent(event)
                ?: throw IllegalStateException("got event with no session: $event")
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun stop() {
        currentServer?.stop()
                ?: throw IllegalStateException("stop() with no current server")
        currentServer = null
    }
}