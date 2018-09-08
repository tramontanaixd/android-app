package com.pierdr.tramontana.io

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.OnLifecycleEvent
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.pierdr.tramontana.model.Event
import com.pierdr.tramontana.model.EventSink
import com.pierdr.tramontana.model.UserReporter
import kotlin.reflect.KClass

class Sensors(
        applicationContext: Context,
        eventSink: EventSink,
        private val userReporter: UserReporter
) : LifecycleObserver {
    private val tag = "Sensors"

    private val availableSensors: Map<KClass<out TramontanaSensor>, TramontanaSensor>

    init {
        val allSensors = listOf(
                Proximity(eventSink, applicationContext),
                Attitude(eventSink, applicationContext),
                Orientation(eventSink, applicationContext),
                Magnetometer(eventSink, applicationContext)
        )
        availableSensors = allSensors
                .filter { it.isAvailable }
                .associateBy { it::class }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun unregisterAllSensors() {
        availableSensors.values.forEach { it.stop() }
    }

    fun startSensor(sensorClass: KClass<out TramontanaSensor>, samplingPeriodUs: Int = SensorManager.SENSOR_DELAY_UI) {
        val sensor = availableSensors[sensorClass]!!
        if (!sensor.isAvailable) {
            userReporter.showWarning("Sensor of type $sensorClass is not available on this device.")
            return
        }
        sensor.start(samplingPeriodUs)
    }

    fun stopSensor(sensorClass: KClass<out TramontanaSensor>) {
        availableSensors[sensorClass]!!.stop()
    }
}

sealed class TramontanaSensor(
        val eventSink: EventSink
) {
    abstract val isAvailable: Boolean
    abstract fun start(samplingPeriodUs: Int)
    abstract val isRunning: Boolean
    abstract fun stop()
}

/** A [TramontanaSensor] that directly maps to an Android [android.hardware.Sensor]. */
abstract class SimpleAndroidSensor(
        eventSink: EventSink,
        applicationContext: Context,
        type: Int
) : TramontanaSensor(eventSink), SensorEventListener {
    private val sensorManager = applicationContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val sensor = sensorManager.getDefaultSensor(type)

    override val isAvailable: Boolean
        get() = sensor != null

    override fun start(samplingPeriodUs: Int) {
        if (isRunning) return
        sensorManager.registerListener(this, sensor, samplingPeriodUs)
        isRunning = true
    }

    override var isRunning = false

    override fun stop() {
        if (!isRunning) return
        sensorManager.unregisterListener(this, sensor)
        isRunning = false
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) { }
}

class Proximity(eventSink: EventSink, applicationContext: Context) : SimpleAndroidSensor(eventSink, applicationContext, Sensor.TYPE_PROXIMITY) {
    override fun onSensorChanged(event: SensorEvent) {
        eventSink.onEvent(Event.Distance(event.values[0]))
    }
}

class Attitude(eventSink: EventSink, applicationContext: Context) : SimpleAndroidSensor(eventSink, applicationContext, Sensor.TYPE_ROTATION_VECTOR) {
    override fun onSensorChanged(event: SensorEvent) {
        eventSink.onEvent(Event.Attitude(event.values[0], event.values[1], event.values[2]))
    }
}

class Orientation(eventSink: EventSink, applicationContext: Context) : SimpleAndroidSensor(eventSink, applicationContext, Sensor.TYPE_ORIENTATION) {
    override fun onSensorChanged(event: SensorEvent?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

class Magnetometer(eventSink: EventSink, applicationContext: Context) : SimpleAndroidSensor(eventSink, applicationContext, Sensor.TYPE_MAGNETIC_FIELD) {
    override fun onSensorChanged(event: SensorEvent?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}