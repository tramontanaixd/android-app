package com.pierdr.pierluigidallarosa.myactivity

import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.launch
import org.java_websocket.WebSocket
import org.java_websocket.WebSocketImpl
import org.java_websocket.drafts.Draft_6455
import processing.core.PApplet.map
import processing.data.JSONObject

interface DirectiveSource {
    fun produceDirectives(): ReceiveChannel<Directive>
}

class WebsocketDirectiveSource
internal constructor(
        private val cm: ConsoleManager
) : DirectiveSource {

    override fun produceDirectives(): ReceiveChannel<Directive> {
        val channel = Channel<Directive>()
        val listener = object : WebsocketManager.WebsocketManagerListener {
            override fun onNewMessage(message: String, socket: WebSocket) {
                launch {
                    channel.send(parseDirective(message))
                }
            }

            override fun onNewConnection(newDevice: String) {
                cm.addNewMessage("connection open with $newDevice")
            }

        }

        WebSocketImpl.DEBUG = false
        val manager = WebsocketManager(9092, Draft_6455())
        manager.addAListener(listener)

        manager.connectionLostTimeout = 0
        manager.start()
        cm.addNewMessage("server started")

        return channel
    }

    private fun parseDirective(message: String): Directive {
        println("received a message")
        println(message)

        val json = JSONObject.parse(message)

        val directive = json.getString("m")
        cm.addNewMessage("received msg: $directive")

        return when (directive) {
            "makeVibrate" -> Directive.MakeVibrate
            "setColor" -> parseSetColor(json)
            "setBrightness" -> Directive.SetBrightness(json.getFloat("b"))
            "setLED" -> Directive.SetLed(json.getFloat("in"))
            "showImage" -> Directive.ShowImage(json.getString("url"))
            "playVideo" -> Directive.PlayVideo(json.getString("url"))
            "registerTouch", "registerTouchDrag" -> parseRegisterTouch(json)
            "releaseTouch" -> Directive.ReleaseTouch
            "registerDistance" -> Directive.RegisterDistance
            "releaseDistance" -> Directive.ReleaseDistance
            "registerAttitude" -> Directive.RegisterAttitude(json.getFloat("f"))
            "releaseAttitude" -> Directive.ReleaseAttitude
            else -> throw IllegalArgumentException("invalid directive $directive")
        }

    }

    private fun parseSetColor(json: JSONObject): Directive.SetColor {
        val colorsAsFloat = json.getString("r").contains(".")
        System.out.println(colorsAsFloat)
        return if (colorsAsFloat) {
            fun floatColorToInt(key: String): Int =
                    map(json.getFloat(key), 0.0.toFloat(), 1.0.toFloat(), 0f, 255f).toInt()
            Directive.SetColor(
                    floatColorToInt("r"),
                    floatColorToInt("g"),
                    floatColorToInt("b"),
                    floatColorToInt("a")
            )
        } else {
            Directive.SetColor(
                    json.getInt("r"),
                    json.getInt("g"),
                    json.getInt("b"),
                    json.getInt("a")
            )
        }
    }

    private fun parseRegisterTouch(json: JSONObject): Directive.RegisterTouch {
        val multi = json.hasKey("multi")
        val drag = json.get("m") == "registerTouchDrag"
        return Directive.RegisterTouch(multi, drag)
    }
}