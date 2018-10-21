package com.pierdr.tramontana.io

import com.illposed.osc.OSCMessage
import com.illposed.osc.OSCPortOut
import com.pierdr.tramontana.model.Event
import com.pierdr.tramontana.model.EventSink
import kotlinx.coroutines.experimental.launch
import java.net.InetAddress

class OscSender : EventSink {
    private var attitudeSession: OscSession? = null
    private var touchSession: OscSession? = null

    fun startAttitudeSend(clientAddress: String, udpPort: Int) {
        val sender = OSCPortOut(InetAddress.getByName(clientAddress), udpPort)
        attitudeSession = OscSession(sender)
    }

    fun stopAttitudeSend() {
        attitudeSession = null
    }

    fun startTouchSend(clientAddress: String, udpPort: Int) {
        val sender = OSCPortOut(InetAddress.getByName(clientAddress), udpPort)
        touchSession = OscSession(sender)
    }

    fun stopTouchSend() {
        touchSession = null
    }

    override fun onEvent(event: Event) {
        attitudeSession?.send(event)
        touchSession?.send(event)
    }
}

class OscSession(
        private val sender: OSCPortOut
) {
    fun send(event: Event) {
        if (event is Event.Attitude) {
            launch {
                val msg = OSCMessage("/wek/inputs", listOf(event.yaw, event.roll, event.pitch))
                sender.send(msg)
            }
        }
        if (event is Event.MultiTouchDrag) {
            launch {
                val args = listOf(event.touches.size)
                        .plus(event.touches.flatMap { listOf(it.x, it.y) })
                val msg = OSCMessage("/wek/inputs", args)
                sender.send(msg)
            }
        }
    }
}