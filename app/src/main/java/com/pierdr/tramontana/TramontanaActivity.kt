package com.pierdr.tramontana

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.OnLifecycleEvent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.pierdr.pierluigidallarosa.myactivity.R
import kotlinx.coroutines.experimental.launch

class TramontanaActivity : AppCompatActivity(), SessionConsumer {
    private val TAG = javaClass.simpleName

    private val serverObserver = ServerLifecycleObserver(this)

    private var readyFragment: ReadyFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onResume() {
        super.onResume()
        showReadyFragment()
    }

    private fun showReadyFragment() {
        readyFragment = ReadyFragment()
        supportFragmentManager.beginTransaction()
                .replace(R.id.container, readyFragment)
                .commit()
    }

    override fun onStartClientSession(session: ClientSession) {
        Log.d(TAG, "onStartClientSession $session")
        showShowtimeFragment()

        launch {
            Log.d(TAG, "waiting for directives")
            for (directive in session.produceDirectives()) {

            }
            Log.d(TAG, "session closed, no more directives")
            showReadyFragment()
        }
    }

    private fun showShowtimeFragment() {
        supportFragmentManager.beginTransaction()
                .remove(readyFragment)
                .commit()
    }

    init {
        lifecycle.addObserver(serverObserver)
    }
}

interface SessionConsumer {
    /**
     * Signals that a new client session has started.
     *
     * Listen to [ClientSession.produceDirectives]: when this channel closes, the session is ended.
     */
    fun onStartClientSession(session: ClientSession)
}

class ServerLifecycleObserver(
        private val sessionConsumer: SessionConsumer
) : LifecycleObserver {
    private val TAG = javaClass.simpleName
    private var currentServer: Server? = null

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun start() = launch {
        val server = Server()
        currentServer = server
        server.start()
        Log.d(TAG, "waiting for client sessions")
        for (clientSession in server.produceClientSessions()) {
            Log.d(TAG, "got client session $clientSession")
            sessionConsumer.onStartClientSession(clientSession)
        }
        Log.d(TAG, "no more client sessions")
    }


    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun stop() {
        currentServer?.stop() ?: throw IllegalStateException("stop() with no current server")
        currentServer = null
    }
}

