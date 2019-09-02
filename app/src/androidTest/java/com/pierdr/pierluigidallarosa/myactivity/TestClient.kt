package com.pierdr.pierluigidallarosa.myactivity

import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI

open class TestClient(
        private val doOnOpen: () -> Unit = {},
        private val doOnError: (Exception) -> Unit = { throw it }
) : WebSocketClient(URI.create("http://localhost:9092")) {
    override fun onOpen(handshakedata: ServerHandshake?) {
        doOnOpen()
    }

    override fun onClose(code: Int, reason: String?, remote: Boolean) {
    }

    override fun onMessage(message: String?) {
    }

    override fun onError(ex: Exception?) {
        doOnError(ex!!)
    }
}