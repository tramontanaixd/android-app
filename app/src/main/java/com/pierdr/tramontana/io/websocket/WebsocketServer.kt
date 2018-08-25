package com.pierdr.tramontana.io.websocket

import android.util.Log
import com.pierdr.tramontana.model.ClientSession
import com.pierdr.tramontana.model.Directive
import com.pierdr.tramontana.model.Event
import com.pierdr.tramontana.model.Server
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.launch
import org.java_websocket.WebSocket
import org.java_websocket.WebSocketImpl
import org.java_websocket.drafts.Draft_6455
import org.java_websocket.handshake.ClientHandshake
import java.lang.Exception
import java.net.InetSocketAddress
import kotlin.coroutines.experimental.suspendCoroutine

class WebsocketServer : Server {
    private val TAG = javaClass.simpleName
    private val websocketServer = PluggableBehaviorWebSocketServer(InetSocketAddress(9092), listOf(Draft_6455()))
    private val protocol = Protocol()

    /**
     * Starts the server.
     *
     * This method will suspend until the server is successfully started, or an error occurs.
     */
    override suspend fun start() = suspendCoroutine<Unit> { continuation ->
        WebSocketImpl.DEBUG = false
        websocketServer.connectionLostTimeout = 0
        websocketServer.start()
        websocketServer.attachBehavior(object : WebSocketServerBehavior() {
            override fun onStart() {
                Log.d(TAG, "server started")
                continuation.resume(Unit)
                websocketServer.detachBehavior()
            }

            override fun onError(conn: WebSocket?, ex: Exception) {
                Log.d(TAG, "unable to start server: $ex")
                if (conn != null) throw IllegalStateException("didn't expect a connection, but here it is: $conn")
                continuation.resumeWithException(ex)
                websocketServer.detachBehavior()
            }
        })

    }

    /**
     * Stops the server.
     *
     * As per [org.java_websocket.server.WebSocketServer] documentation, this will
     * also stop all the connections, therefore, any receive channels returned by [produceClientSessions]
     * will stop too.
     */
    override fun stop() {
        Log.d(TAG, "stopping server")
        websocketServer.stop()
    }

    /**
     * Returns a [ReceiveChannel] that emits [ClientSession]s every time a new Tramontana client
     * connects to us. Only one connection at a time is supported. The channel stops when the server
     * is stopped.
     */
    override fun produceClientSessions() = produce<ClientSession> {
        suspendCoroutine<Unit> { sessionsContinuation ->
            websocketServer.attachBehavior(object : WebSocketServerBehavior() {
                private var currentSession: WebSocketClientSession? = null
                override fun onOpen(conn: WebSocket, handshake: ClientHandshake) {
                    Log.d(TAG, "onOpen $conn")
                    if (currentSession != null) throw IllegalStateException("multiple connections are not supported")
                    launch {
                        val newSession = WebSocketClientSession(conn)
                        Log.d(TAG, "creating new session $newSession")
                        currentSession = newSession
                        channel.send(newSession)
                        Log.d(TAG, "session sent to recipient")
                    }
                }

                override fun onClose(conn: WebSocket, code: Int, reason: String?, remote: Boolean) {
                    Log.d(TAG, "onClose $conn")
                    val session = currentSession
                            ?: throw IllegalStateException("no connection to close")
                    if (conn != session.connection) throw IllegalAccessException("connection $conn is close but it's not ours, ${session.connection}")
                    session.directivesChannel.close()
                    currentSession = null
                }

                override fun onMessage(conn: WebSocket, message: String) {
                    Log.d(TAG, "onMessage $conn $message")
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
            })
        }
    }
}

class WebSocketClientSession(
        val connection: WebSocket
) : ClientSession {
    private val TAG = javaClass.simpleName
    private val protocol = Protocol()

    val directivesChannel = Channel<Directive>()

    override fun sendEvent(event: Event) {
        val message = protocol.emit(event)
        Log.d(TAG, "sending message $message")
        connection.send(message)
    }

    override fun produceDirectives(): ReceiveChannel<Directive> = directivesChannel

    override fun close() {
        connection.close()
    }

}