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
            "transitionColors" -> parseTransitionColors(json)
            "setBrightness" -> Directive.SetBrightness(json.getFloat("b"))
            "setLED" -> Directive.SetLed(json.getFloat("in"))
            "pulseLED" -> Directive.PulseLed(json.getInt("t"), (json.getFloat("d") * 1000).toLong(), 1f)
            "showImage" -> Directive.ShowImage(json.getString("url"))
            "playVideo" -> Directive.PlayVideo(json.getString("url"))
            "playAudio" -> Directive.PlayAudio(json.getString("url"))
            "registerTouch", "registerTouchDrag" -> parseRegisterTouch(json)
            "releaseTouch", "releaseTouchDrag" -> Directive.ReleaseTouch
            "registerDistance" -> Directive.RegisterDistance
            "releaseDistance" -> Directive.ReleaseDistance
            "registerAttitude" -> Directive.RegisterAttitude(json.getFloat("f"))
            "releaseAttitude" -> Directive.ReleaseAttitude
            "registerOrientation" -> Directive.RegisterOrientation
            "releaseOrientation" -> Directive.ReleaseOrientation
            "registerMagnetometer" -> Directive.RegisterMagnetometer
            "releaseMagnetometer" -> Directive.ReleaseMagnetometer
            "getBattery" -> Directive.GetBattery
            "registerPowerSource" -> Directive.RegisterPowerSource
            "releasePowerSource" -> Directive.ReleasePowerSource
            "registerAudioJack" -> Directive.RegisterAudioJack
            "releaseAudioJack" -> Directive.ReleaseAudioJack
            "a2w" -> parseSendAttitudeToOSC(json)
            "sa2w" -> Directive.StopAttitudeToOSC
            "t2w" -> parseSendTouchToOSC(json)
            "st2w" -> Directive.StopTouchToOSC
            else -> throw IllegalArgumentException("invalid directive $directive")
        }

    }

    private fun parseSendTouchToOSC(json: JSONObject) = Directive.SendTouchToOSC(
            address = json.getString("i"),
            port = json.getString("p").toInt(),
            maxNumFingers = json.getString("n").toInt()
    )

    private fun parseSendAttitudeToOSC(json: JSONObject) = Directive.SendAttitudeToOSC(
            address = json.getString("i"),
            port = json.getString("p").toInt(),
            updateRate = json.getString("f").toFloat().coerceAtMost(29f))

    private fun parseTransitionColors(json: JSONObject) = Directive.TransitionColors(
            json.getFloatColor("r1"),
            json.getFloatColor("g1"),
            json.getFloatColor("b1"),
            json.getFloatColor("a1"),
            json.getFloatColor("r2"),
            json.getFloatColor("g2"),
            json.getFloatColor("b2"),
            json.getFloatColor("a2"),
            json.getFloat("duration").toInt()
    )

    private fun parseSetColor(json: JSONObject): Directive.SetColor {
        val colorsAsFloat = json.getString("r").contains(".")
        return if (colorsAsFloat) {
            Directive.SetColor(
                    json.getFloatColor("r"),
                    json.getFloatColor("g"),
                    json.getFloatColor("b"),
                    json.getFloatColor("a")
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

    private fun JSONObject.getFloatColor(key: String): Int =
            PApplet.map(getFloat(key), 0.0.toFloat(), 1.0.toFloat(), 0f, 255f).toInt()

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
        is Event.Orientation -> {
            JSONObject()
                    .setString("m", "orientationChanged")
                    .setString("value", "${event.orientation.toIndex()}")
        }
        is Event.Distance -> {
            JSONObject()
                    .setString("m", "distanceChanged")
                    .setString("proximity", "${event.distance}")
        }
        is Event.Magnetometer -> {
            JSONObject()
                    .setString("m", "magnetometerUpdate")
                    .setString("i", "${event.intensity}")
                    .setString("t", "${event.threshold}")
        }
        is Event.VideoEnded -> {
            JSONObject()
                    .setString("m", "videoEnded")
        }
        is Event.BatteryLevel -> {
            JSONObject()
                    .setString("m", "battery")
                    .setString("v", "${event.fraction}")
        }
        is Event.PowerSourceChanged -> {
            JSONObject()
                    .setString("m", "powerSourceChanged")
                    .setString("source", if (event.pluggedIn) "1" else "0")
        }
        is Event.AudioJackChanged -> {
            JSONObject()
                    .setString("m", "audioJackChanged")
                    .setString("in", if (event.pluggedIn) "1" else "0")
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

    private fun Event.Orientation.Direction.toIndex() = when (this) {
        Event.Orientation.Direction.PORTRAIT -> 0
        Event.Orientation.Direction.PORTRAIT_UPSIDE_DOWN -> 1
        Event.Orientation.Direction.LANDSCAPE_LEFT -> 2
        Event.Orientation.Direction.LANDSCAPE_RIGHT -> 3
        Event.Orientation.Direction.FACING_UP -> 4
        Event.Orientation.Direction.FACING_DOWN -> 5
    }
}