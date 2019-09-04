package com.pierdr.pierluigidallarosa.myactivity.util

import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI

interface WebSocketEndpoint {
    fun onOpen(handshakedata: ServerHandshake)
    fun onClose(code: Int, reason: String?, remote: Boolean)
    fun onMessage(message: String)
    fun onError(ex: Exception)
}

class MockableClient(private val endpoint: WebSocketEndpoint)
    : WebSocketClient(URI.create("http://localhost:9092")) {
    override fun onOpen(handshakedata: ServerHandshake) {
        endpoint.onOpen(handshakedata)
    }

    override fun onClose(code: Int, reason: String?, remote: Boolean) {
        endpoint.onClose(code, reason, remote)
    }

    override fun onMessage(message: String) {
        endpoint.onMessage(message)
    }

    override fun onError(ex: Exception) {
        endpoint.onError(ex)
    }
}