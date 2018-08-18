package com.pierdr.pierluigidallarosa.myactivity.websocket

import com.pierdr.pierluigidallarosa.myactivity.ConsoleManager
import com.pierdr.pierluigidallarosa.myactivity.Directive
import com.pierdr.pierluigidallarosa.myactivity.DirectiveSource
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.launch
import org.java_websocket.WebSocket

class WebsocketDirectiveSource
internal constructor(
        private val manager: WebsocketManager,
        private val cm: ConsoleManager
) : DirectiveSource {

    override fun produceDirectives(): ReceiveChannel<Directive> {
        val channel = Channel<Directive>()
        val parser = DirectiveParser()
        val listener = object : WebsocketManager.WebsocketManagerListener {
            override fun onNewMessage(message: String, socket: WebSocket) {
                launch {
                    channel.send(parser.parse(message))
                }
            }

            override fun onNewConnection(newDevice: String) {
                cm.addNewMessage("connection open with $newDevice")
            }

        }

        manager.addAListener(listener)

        return channel
    }
}