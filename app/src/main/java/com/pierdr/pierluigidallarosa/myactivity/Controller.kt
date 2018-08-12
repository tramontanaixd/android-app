package com.pierdr.pierluigidallarosa.myactivity

import android.widget.Toast
import com.pierdr.pierluigidallarosa.myactivity.websocket.WebsocketDirectiveSource
import com.pierdr.pierluigidallarosa.myactivity.websocket.WebsocketManager
import kotlinx.coroutines.experimental.launch
import org.java_websocket.WebSocketImpl
import org.java_websocket.drafts.Draft_6455

/**
 * Initializes the concrete implementations of a [DirectiveSource] and of a
 * [DirectiveExecutor], and connects them together.
 */
class Controller(
        private val mainActivity: MainActivity
) {
    private val cm = mainActivity.consoleManager

    fun start() {
        //START WEBSOCKETS
        try {
            val websocketManager = initWebsocketManager()

            val directiveSource = WebsocketDirectiveSource(websocketManager, cm)
            val executor: DirectiveExecutor = AndroidDirectiveExecutor(mainActivity.applicationContext, mainActivity)

            launch {
                for (directive in directiveSource.produceDirectives()) {
                    executor.executeDirective(directive)
                }
            }

            startWebsocketManager(websocketManager)

        } catch (e: Exception) {
            val toastTmp = Toast.makeText(mainActivity, "Coudln't start the Websocket Server.", Toast.LENGTH_SHORT)
            toastTmp.show()
            cm.addNewMessage("Error in starting the websocket server.")
            println("Error in starting the websocket manager.")
        }
    }

    private fun initWebsocketManager(): WebsocketManager {
        WebSocketImpl.DEBUG = false
        return WebsocketManager(9092, Draft_6455())
    }

    private fun startWebsocketManager(manager: WebsocketManager) {
        manager.connectionLostTimeout = 0
        manager.start()
        cm.addNewMessage("server started")
    }
}