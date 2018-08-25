package com.pierdr.tramontana.ui

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.OnLifecycleEvent
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import com.pierdr.tramontana.model.Event
import com.pierdr.tramontana.model.EventSink

class Sensors(
        private val applicationContext: Context,
        private val eventSink: EventSink
) : LifecycleObserver, SensorEventListener {
    private val tag = "Sensors"

    private val sensorManager by lazy { applicationContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    private val proximitySensor by lazy { sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY) }
    private val rotationSensor by lazy { sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) }

    private val registeredSensors = HashSet<Sensor>()

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun stop() {
        for (sensor in registeredSensors) {
            sensorManager.unregisterListener(this, sensor)
        }
        registeredSensors.clear()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onSensorChanged(event: SensorEvent) {
        Log.d(tag, "onSensorChanged $event")
        when (event.sensor) {
            proximitySensor -> eventSink.onEvent(Event.Distance(event.values[0]))
            rotationSensor -> eventSink.onEvent(Event.Attitude(event.values[0], event.values[1], event.values[2]))
        }
    }

    fun startDistance() = startSensor(proximitySensor, SensorManager.SENSOR_DELAY_UI)
    fun stopDistance() = stopSensor(proximitySensor)

    fun startAttitude(updateRate: Float) =
            startSensor(rotationSensor, (1_000_000 / updateRate).toInt())

    fun stopAttitude() = stopSensor(rotationSensor)

    private fun startSensor(sensor: Sensor, samplingPeriodUs: Int) {
        // TODO report to user about unsupported sensors, i.e. null sensor
        if (registeredSensors.contains(sensor)) return
        sensorManager.registerListener(this, sensor, samplingPeriodUs)
        registeredSensors.add(sensor)
    }

    private fun stopSensor(sensor: Sensor) {
        if (!registeredSensors.contains(sensor)) return
        sensorManager.unregisterListener(this, sensor)
        registeredSensors.remove(sensor)
    }
}