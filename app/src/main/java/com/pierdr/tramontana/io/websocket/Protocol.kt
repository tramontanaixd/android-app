package com.pierdr.tramontana.io.websocket

import com.pierdr.tramontana.model.Directive
import com.pierdr.tramontana.model.Event
import com.pierdr.tramontana.model.TouchPoint
import processing.core.PApplet
import processing.data.JSONArray
import processing.data.JSONObject

/**
 * Implements the Tramontana JSON protocol. It has methods to transform JSON messages in [Directive] and
 * [Event] objects into JSON messages.
 */
class Protocol {
    fun parse(message: String): Directive {
        println("received a message")
        println(message)

        val json = JSONObject.parse(message)

        val directive = json.getString("m")

        return when (directive) {
            "makeVibrate" -> Directive.MakeVibrate
            "setColor" -> parseSetColor(json)
            "setBrightness" -> Directive.SetBrightness(json.getFloat("b"))
            "setLED" -> Directive.SetLed(json.getFloat("in"))
            "showImage" -> Directive.ShowImage(json.getString("url"))
            "playVideo" -> Directive.PlayVideo(json.getString("url"))
            "registerTouch", "registerTouchDrag" -> parseRegisterTouch(json)
            "releaseTouch", "releaseTouchDrag" -> Directive.ReleaseTouch
            "registerDistance" -> Directive.RegisterDistance
            "releaseDistance" -> Directive.ReleaseDistance
            "registerAttitude" -> Directive.RegisterAttitude(json.getFloat("f"))
            "releaseAttitude" -> Directive.ReleaseAttitude
            "registerOrientation" -> Directive.RegisterOrientation(json.getFloat("f"))
            "releaseOrientation" -> Directive.ReleaseOrientation
            "registerMagnetometer" -> Directive.RegisterMagnetometer(json.getFloat("f"))
            "releaseMagnetometer" -> Directive.ReleaseMagnetometer

            else -> throw IllegalArgumentException("invalid directive $directive")
        }

    }

    private fun parseSetColor(json: JSONObject): Directive.SetColor {
        val colorsAsFloat = json.getString("r").contains(".")
        System.out.println(colorsAsFloat)
        return if (colorsAsFloat) {
            fun floatColorToInt(key: String): Int =
                    PApplet.map(json.getFloat(key), 0.0.toFloat(), 1.0.toFloat(), 0f, 255f).toInt()
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

    fun emit(event: Event): String = when (event) {
        is Event.TouchStart -> {
            JSONObject()
                    .setString("m", "touchedDown")
                    .setXyMembers(event.x, event.y)
        }
        is Event.MultiTouchStart -> {
            JSONObject()
                    .setString("m", "touchedDown")
                    .setTsMember(event.touches)
        }
        is Event.TouchDrag -> {
            JSONObject()
                    .setString("m", "drag")
                    .setXyMembers(event.x, event.y)
        }
        is Event.MultiTouchDrag -> {
            JSONObject()
                    .setString("m", "drag")
                    .setTsMember(event.touches)
        }
        is Event.TouchEnd -> {
            JSONObject()
                    .setString("m", "touched")
                    .setXyMembers(event.x, event.y)
        }
        is Event.MultiTouchEnd -> {
            JSONObject()
                    .setString("m", "touched")
                    .setTsMember(event.touches)
        }
        is Event.Attitude -> {
            JSONObject()
                    .setString("m", "a")
                    .setString("r", "${event.roll}")
                    .setString("p", "${event.pitch}")
                    .setString("y", "${event.yaw}")
        }
        is Event.Distance -> {
            JSONObject()
                    .setString("m", "distanceChanged")
                    .setString("proximity", "${event.distance}")
        }
        is Event.VideoEnded -> {
            JSONObject()
                    .setString("m", "videoEnded")
        }
    }.toString()

    private fun JSONObject.setXyMembers(x: Int, y: Int) = this
            .setString("x", "$x")
            .setString("y", "$y")

    private fun JSONObject.setTsMember(touches: List<TouchPoint>) = this
            .setJSONArray("ts", JSONArray().apply {
                touches.forEach {
                    append(JSONObject().setXyMembers(it.x, it.y))
                }
            })
}