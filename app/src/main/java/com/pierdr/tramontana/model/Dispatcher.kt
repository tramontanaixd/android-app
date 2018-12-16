package com.pierdr.tramontana.model

import android.os.Vibrator
import android.util.Log
import com.pierdr.tramontana.io.OscSender
import com.pierdr.tramontana.io.PowerMonitor
import com.pierdr.tramontana.io.sensor.*
import com.pierdr.tramontana.ui.Flashlight
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.launch
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import kotlin.coroutines.CoroutineContext
import kotlin.math.roundToInt

/**
 * This class:
 * * executes directives
 * * dispatches events to clients or OSC endpoints
 * * forwards commands to UI
 * * receives events from UI and from sensors
 */
class Dispatcher : KoinComponent, CoroutineScope, EventSink {
    private val TAG = javaClass.simpleName
    private val server: Server by inject()
    private val sensors: Sensors by inject()
    private val vibrator: Vibrator by inject()
    private val flashlight: Flashlight by inject()
    private val powerMonitor: PowerMonitor by inject()
    private val oscSender: OscSender by inject()

    private val clientEventDestination: EventSink = object : EventSink {
        override fun onEvent(event: Event) {
            server.currentClientSession?.sendEvent(event)
        }
    }

    private val oscEventDestination: EventSink = object : EventSink {
        override fun onEvent(event: Event) {
            oscSender.onEvent(event)
        }
    }

    private val eventDestinations = mutableSetOf<EventSink>()

    private lateinit var job: Job
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

    private var uiDirectivesChannel = BroadcastChannel<Directive.NeedsUi>(10)

    fun start() {
        job = Job()
        launch {
            try {
                server.start()
                Log.d(TAG, "waiting for client sessions")
                for (clientSession in server.produceClientSessions()) {
                    handleClientSession(clientSession)
                }
                Log.d(TAG, "no more client sessions")
            }
            finally {
                server.stop()
            }
        }
    }

    fun stop() {
        job.cancel()
        sensors.stopAll()
        flashlight.stop()
    }

    fun produceUiDirectives(): ReceiveChannel<Directive.NeedsUi> = uiDirectivesChannel.openSubscription()

    private fun handleClientSession(session: ClientSession) {
        Log.i(TAG, "got client session $session")

        launch {
            Log.d(TAG, "waiting for directives")
            val directiveSubscription = session.subscribeToDirectives()
            runDirectiveOnUiThread(StartUi)
            for (directive in directiveSubscription) {
                withEventDestinationsSetUp(directive) {
                    when (directive) {
                        is Directive.MakeVibrate -> vibrator.vibrate(100)
                        is Directive.SetLed -> flashlight.set(directive.intensity)
                        is Directive.PulseLed -> flashlight.pulse(directive.numberOfPulses, directive.durationMillis)
                        is Directive.RegisterDistance -> sensors.startSensor(Proximity::class)
                        is Directive.ReleaseDistance -> sensors.stopSensor(Proximity::class)
                        is Directive.RegisterAttitude -> sensors.startSensor(Attitude::class, directive.updateRate.toMicros())
                        is Directive.ReleaseAttitude -> sensors.stopSensor(Attitude::class)
                        is Directive.RegisterOrientation -> sensors.startSensor(Orientation::class)
                        is Directive.ReleaseOrientation -> sensors.stopSensor(Orientation::class)
                        is Directive.RegisterMagnetometer -> sensors.startSensor(Magnetometer::class)
                        is Directive.ReleaseMagnetometer -> sensors.stopSensor(Magnetometer::class)
                        is Directive.GetBattery -> powerMonitor.sendBatteryLevel()
                        is Directive.RegisterPowerSource -> sensors.startSensor(PowerSource::class)
                        is Directive.ReleasePowerSource -> sensors.stopSensor(PowerSource::class)
                        is Directive.RegisterAudioJack -> sensors.startSensor(AudioJack::class)
                        is Directive.ReleaseAudioJack -> sensors.stopSensor(AudioJack::class)
                        is Directive.SendAttitudeToOSC -> startAttitudeToOSC(directive)
                        is Directive.StopAttitudeToOSC -> stopAttitudeToOSC()
                        is Directive.SendTouchToOSC -> startTouchToOSC(directive)
                        is Directive.StopTouchToOSC -> stopTouchToOSC()
                        is Directive.NeedsUi -> runDirectiveOnUiThread(directive)
                    }
                }
            }
            runDirectiveOnUiThread(StopUi)
            Log.d(TAG, "session or view closed")
        }

    }

    private fun withEventDestinationsSetUp(directive: Directive, action: () -> Unit) {
        when (directive) {
            is Directive.StopsEventsToClient -> eventDestinations -= clientEventDestination
            is Directive.StopsEventsToOSC -> eventDestinations -= oscEventDestination
        }
        action()
        when (directive) {
            is Directive.StartsEventsToClient -> eventDestinations += clientEventDestination
            is Directive.StartsEventsToOSC -> eventDestinations += oscEventDestination
        }
    }

    private fun runDirectiveOnUiThread(directive: Directive.NeedsUi) {
        launch(Dispatchers.Main) {
            uiDirectivesChannel.send(directive)
        }
    }

    private fun Float.toMicros() = (1_000_000 / this).toInt()

    private fun startAttitudeToOSC(directive: Directive.SendAttitudeToOSC) {
        sensors.stopSensor(Attitude::class)
        oscSender.startAttitudeSend(directive.address, directive.port)
        sensors.startSensor(Attitude::class, (1_000_000f / directive.updateRate).roundToInt())
    }

    private fun stopAttitudeToOSC() {
        sensors.stopSensor(Attitude::class)
        oscSender.stopAttitudeSend()
    }

    private fun startTouchToOSC(directive: Directive.SendTouchToOSC) {
        runDirectiveOnUiThread(Directive.RegisterTouch(true, true))
        oscSender.startTouchSend(directive.address, directive.port)
    }

    private fun stopTouchToOSC() {
        oscSender.stopTouchSend()
        runDirectiveOnUiThread(Directive.ReleaseTouch)
    }

    override fun onEvent(event: Event) {
        eventDestinations.forEach { it.onEvent(event) }
    }
}