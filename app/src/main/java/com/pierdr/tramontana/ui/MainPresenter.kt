package com.pierdr.tramontana.ui

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.OnLifecycleEvent
import android.util.Log
import com.pierdr.tramontana.io.websocket.WebsocketServer
import com.pierdr.tramontana.model.ClientSession
import com.pierdr.tramontana.model.Event
import com.pierdr.tramontana.model.EventSink
import com.pierdr.tramontana.model.Server
import kotlinx.coroutines.experimental.launch


class MainPresenter : LifecycleObserver, EventSink {

    private val TAG = javaClass.simpleName
    private var currentServer: Server? = null
    private var currentSession: ClientSession? = null
    private var view: MainView? = null

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun start(owner: LifecycleOwner) = launch {
        view = owner as MainView
        view?.showReadyFragment()

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
        view?.showShowtimeFragment() ?: return

        launch {
            Log.d(TAG, "waiting for directives")
            for (directive in session.produceDirectives()) {
                view?.runDirective(directive) ?: break
            }
            Log.d(TAG, "session or view closed")
            currentSession = null
            view?.showReadyFragment()
        }

    }

    override fun onEvent(event: Event) {
        val session = currentSession
        if (session == null) {
            Log.i(TAG, "discarding event when there's no session: $event")
            return
        }
        session.sendEvent(event)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun stop() {
        currentServer?.stop()
                ?: throw IllegalStateException("stop() with no current server")
        currentServer = null
    }
}