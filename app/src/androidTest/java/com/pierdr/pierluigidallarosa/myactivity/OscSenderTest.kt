package com.pierdr.pierluigidallarosa.myactivity

import com.pierdr.tramontana.io.OscSender
import com.pierdr.tramontana.model.Event
import org.junit.Test

class OscSenderTest {
    @Test
    fun sendAttitudeEvent() {
        // this test doesn't check anything, just sends an OSC message. Use an OSC receiver app to test.
        with(OscSender()) {
            startAttitudeSend("192.168.0.107", 8000)
            onEvent(Event.Attitude(yaw = 10f, roll = 20f, pitch = 30f))
        }
    }
}