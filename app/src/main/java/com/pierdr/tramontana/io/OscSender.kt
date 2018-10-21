package com.pierdr.tramontana.io

import com.illposed.osc.OSCMessage
import com.illposed.osc.OSCPortOut
import com.pierdr.tramontana.model.Event
import com.pierdr.tramontana.model.EventSink
import kotlinx.coroutines.experimental.launch
import java.net.InetAddress

class OscSender : EventSink {
    private var attitudeSession: OscSession? = null

    fun startAttitudeSend(clientAddress: String, udpPort: Int) {
        val sender = OSCPortOut(InetAddress.getByName(clientAddress), udpPort)
        attitudeSession = OscSession(sender)
    }

    fun stopAttitudeSend() {
        attitudeSession = null
    }

    override fun onEvent(event: Event) {
        attitudeSession?.send(event)
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
    }
}