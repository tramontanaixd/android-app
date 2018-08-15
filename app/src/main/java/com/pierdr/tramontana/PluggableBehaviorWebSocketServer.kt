package com.pierdr.tramontana

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
    private var behavior: WebSocketServerBehavior? = null

    fun attachBehavior(newBehavior: WebSocketServerBehavior) {
        if (behavior != null) throw IllegalStateException("behavior already attach: $behavior")
        behavior = newBehavior
    }

    fun detachBehavior() {
        if (behavior == null) throw IllegalStateException("no behavior to detach")
        behavior = null
    }

    override fun onStart() {
        behavior?.onStart() ?: throw IllegalStateException("no behavior attached")
    }

    override fun onOpen(conn: WebSocket, handshake: ClientHandshake) {
        behavior?.onOpen(conn, handshake) ?: throw IllegalStateException("no behavior attached")
    }

    override fun onClose(conn: WebSocket, code: Int, reason: String?, remote: Boolean) {
        behavior?.onClose(conn, code, reason, remote)
                ?: throw IllegalStateException("no behavior attached")
    }

    override fun onMessage(conn: WebSocket, message: String) {
        behavior?.onMessage(conn, message) ?: throw IllegalStateException("no behavior attached")
    }

    override fun onError(conn: WebSocket?, ex: Exception) {
        behavior?.onError(conn, ex) ?: throw IllegalStateException("no behavior attached")
    }

    override fun stop() {
        behavior?.onStop() ?: throw IllegalStateException("no behavior attached")
        behavior = null
        super.stop()
    }
}