package com.pierdr.tramontana.io.websocket

import android.util.Log
import com.pierdr.tramontana.model.ClientSession
import com.pierdr.tramontana.model.Directive
import com.pierdr.tramontana.model.Event
import com.pierdr.tramontana.model.Server
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.launch
import org.java_websocket.WebSocket
import org.java_websocket.WebSocketImpl
import org.java_websocket.drafts.Draft_6455
import org.java_websocket.exceptions.WebsocketNotConnectedException
import org.java_websocket.handshake.ClientHandshake
import java.net.InetSocketAddress
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class WebsocketServer : Server, CoroutineScope {
    private lateinit var job: Job
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default + job

    private val TAG = javaClass.simpleName
    private var websocketServer: PluggableBehaviorWebSocketServer? = null
    private val protocol = Protocol()

    private var currentSession: WebSocketClientSession? = null
    override val currentClientSession: ClientSession?
        get() = currentSession

    /**
     * Starts the server.
     *
     * This method will suspend until the server is successfully started, or an error occurs.
     */
    override suspend fun start() {
        job = Job()
        suspendCoroutine<Unit> { continuation ->
            WebSocketImpl.DEBUG = false
            val server = PluggableBehaviorWebSocketServer(InetSocketAddress(9092), listOf(Draft_6455()))
            websocketServer = server
            server.connectionLostTimeout = 0
            server.isReuseAddr = true
            server.start()
            server.attachBehavior(object : WebSocketServerBehavior() {
                override fun onStart() {
                    Log.i(TAG, "server started")
                    continuation.resume(Unit)
                    server.detachBehavior()
                }

                override fun onError(conn: WebSocket?, ex: Exception) {
                    Log.w(TAG, "unable to start server: $ex")
                    if (conn != null) throw IllegalStateException("didn't expect a connection, but here it is: $conn")
                    continuation.resumeWithException(ex)
                    server.detachBehavior()
                }
            })

        }
    }

    /**
     * Stops the server.
     *
     * As per [org.java_websocket.server.WebSocketServer] documentation, this will
     * also stop all the connections, therefore, any receive channels returned by [produceClientSessions]
     * will stop too.
     */
    override fun stop() {
        job.cancel()
        websocketServer?.let {
            Log.d(TAG, "stopping server")
            it.stop()
        }
    }

    /**
     * Returns a [ReceiveChannel] that emits [ClientSession]s every time a new Tramontana client
     * connects to us. Only one connection at a time is supported. The channel stops when the server
     * is stopped. If the server is already stopped, this method returns a closed channel.
     */
    override fun produceClientSessions() = produce<ClientSession> {
        suspendCoroutine<Unit> { sessionsContinuation ->
            websocketServer?.attachBehavior(object : WebSocketServerBehavior() {
                override fun onOpen(conn: WebSocket, handshake: ClientHandshake) {
                    Log.i(TAG, "onOpen $conn")
                    if (currentSession != null) throw IllegalStateException("multiple connections are not supported")
                    launch {
                        val newSession = WebSocketClientSession(conn)
                        Log.v(TAG, "creating new session $newSession")
                        currentSession = newSession
                        channel.send(newSession)
                        Log.v(TAG, "session sent to recipient")
                    }
                }

                override fun onClose(conn: WebSocket, code: Int, reason: String?, remote: Boolean) {
                    Log.i(TAG, "onClose $conn")
                    val session = currentSession
                            ?: throw IllegalStateException("no connection to close")
                    if (conn != session.connection) throw IllegalAccessException("connection $conn is close but it's not ours, ${session.connection}")
                    session.directivesChannel.close()
                    currentSession = null
                }

                override fun onMessage(conn: WebSocket, message: String) {
                    Log.v(TAG, "onMessage $conn $message")
                    val session = currentSession
                            ?: throw IllegalStateException("no connection to close")
                    if (conn != session.connection) throw IllegalAccessException("connection $conn is close but it's not ours, ${session.connection}")
                    launch {
                        session.directivesChannel.send(protocol.parse(message))
                    }
                }

                override fun onStop() {
                    currentSession = null
                    sessionsContinuation.resume(Unit)
                }
            }) ?: channel.close()
        }
    }
}

class WebSocketClientSession(
        val connection: WebSocket
) : ClientSession {
    private val TAG = javaClass.simpleName
    private val protocol = Protocol()

    val directivesChannel = ConflatedBroadcastChannel<Directive>()

    override fun sendEvent(event: Event) {
        if (isClosed) {
            Log.i(TAG, "connection is closed, discarding event $event")
            return
        }
        val message = protocol.emit(event)
        try {
            connection.send(message)
        }
        catch (e: WebsocketNotConnectedException) {
            Log.w(TAG, "WebsocketNotConnectedException while trying to send event in a seemingly active connection $connection")
        }
    }

    override fun subscribeToDirectives(): ReceiveChannel<Directive> = directivesChannel.openSubscription()

    override fun close() {
        connection.close()
    }

    override val isClosed: Boolean
        get() = connection.isClosing || connection.isClosed || directivesChannel.isClosedForSend
}