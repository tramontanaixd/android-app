package com.pierdr.pierluigidallarosa.myactivity

import android.widget.Toast
import kotlinx.coroutines.experimental.launch

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
            val directiveSource = WebsocketDirectiveSource(cm)
            val executor: DirectiveExecutor = AndroidDirectiveExecutor(mainActivity.applicationContext, mainActivity)

            launch {
                for (directive in directiveSource.produceDirectives()) {
                    executor.executeDirective(directive)
                }
            }

        } catch (e: Exception) {
            val toastTmp = Toast.makeText(mainActivity, "Coudln't start the Websocket Server.", Toast.LENGTH_SHORT)
            toastTmp.show()
            cm.addNewMessage("Error in starting the websocket server.")
            println("Error in starting the websocket manager.")
        }
    }
}