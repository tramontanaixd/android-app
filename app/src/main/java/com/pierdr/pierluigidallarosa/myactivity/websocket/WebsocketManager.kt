package com.pierdr.pierluigidallarosa.myactivity.websocket

import org.java_websocket.WebSocket
import org.java_websocket.drafts.Draft
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer

import java.net.InetSocketAddress

class WebsocketManager
internal constructor(port: Int, d: Draft) : WebSocketServer(InetSocketAddress(port), listOf(d)) {
    private var aListener: WebsocketManagerListener? = null

    interface WebsocketManagerListener {

        fun onNewMessage(message: String, socket: WebSocket)

        fun onNewConnection(newDevice: String)
    }

    init {
        this.aListener = null
    }

    fun addAListener(aListener: WebsocketManagerListener) {
        this.aListener = aListener
    }

    override fun onOpen(conn: WebSocket, handshake: ClientHandshake) {
        counter++
        println("///////////Opened connection number$counter")
        aListener!!.onNewConnection(conn.remoteSocketAddress.address.toString())
    }

    override fun onClose(conn: WebSocket, code: Int, reason: String, remote: Boolean) {
        println("closed")
    }

    override fun onError(conn: WebSocket, ex: Exception) {
        println("Error:")
        ex.printStackTrace()
    }

    override fun onStart() {
        println("Server started!")
    }

    override fun onMessage(conn: WebSocket, message: String) {
        aListener!!.onNewMessage(message, conn)
    }

    companion object {

        private var counter = 0
    }
}
