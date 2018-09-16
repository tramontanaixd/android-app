package com.pierdr.tramontana.io.websocket

import android.util.Log
import org.java_websocket.WebSocket
import org.java_websocket.drafts.Draft
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import java.lang.Exception
import java.net.InetSocketAddress

/**
 * Tells what should a [PluggableBehaviorWebSocketServer] do when certain WebSocket events occur.
 */
open class WebSocketServerBehavior {
    /** see [WebSocketServer.onStart] */
    open fun onStart() {}

    /** see [WebSocketServer.onOpen] */
    open fun onOpen(conn: WebSocket, handshake: ClientHandshake) {}

    /** see [WebSocketServer.onClose] */
    open fun onClose(conn: WebSocket, code: Int, reason: String?, remote: Boolean) {}

    /** see [WebSocketServer.onMessage] */
    open fun onMessage(conn: WebSocket, message: String) {}

    /** see [WebSocketServer.onError] */
    open fun onError(conn: WebSocket?, ex: Exception) {}

    /** called when the server has been stopped by us. */
    open fun onStop() {}
}

/**
 * A [WebSocketServer] whose implementation does nothing by default, but [WebSocketServerBehavior]s
 * can be attached and detached at runtime. At most one behavior can be attached at a time.
 */
class PluggableBehaviorWebSocketServer(
        address: InetSocketAddress,
        drafts: List<Draft>
) : WebSocketServer(address, drafts) {
    private val tag = "PBWebSocketServer"
    private var behavior: WebSocketServerBehavior = NullBehavior()

    fun attachBehavior(newBehavior: WebSocketServerBehavior) {
        behavior = newBehavior
    }

    fun detachBehavior() {
        Log.d(tag, "detachBehavior() called here: behavior=$behavior", Throwable())
        behavior = NullBehavior()
    }

    override fun onStart() {
        behavior.onStart()
    }

    override fun onOpen(conn: WebSocket, handshake: ClientHandshake) {
        behavior.onOpen(conn, handshake)
    }

    override fun onClose(conn: WebSocket, code: Int, reason: String?, remote: Boolean) {
        behavior.onClose(conn, code, reason, remote)
    }

    override fun onMessage(conn: WebSocket, message: String) {
        behavior.onMessage(conn, message)
    }

    override fun onError(conn: WebSocket?, ex: Exception) {
        behavior.onError(conn, ex)
    }

    override fun stop() {
        behavior.onStop()
        behavior = NullBehavior()
        super.stop()
    }
}

class NullBehavior : WebSocketServerBehavior() {
    override fun onStart() {
        throw IllegalStateException("onStart() with null behavior")
    }

    override fun onOpen(conn: WebSocket, handshake: ClientHandshake) {
        throw IllegalStateException("onOpen($conn) with null behavior")
    }

    override fun onClose(conn: WebSocket, code: Int, reason: String?, remote: Boolean) {
        throw IllegalStateException("onClose($conn, $code, $reason, $remote) with null behavior")
    }

    override fun onMessage(conn: WebSocket, message: String) {
        throw IllegalStateException("onMessage($conn, $message) with null behavior")
    }

    override fun onError(conn: WebSocket?, ex: Exception) {
        // gracefully handle close events by doing nothing
    }

    override fun onStop() {
        // gracefully handle stop events by doing nothing
    }
}