package com.pierdr.tramontana.io.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import com.pierdr.tramontana.model.Event
import org.koin.standalone.inject

/** A [TramontanaSensor] that directly maps to an Android [android.hardware.Sensor]. */
abstract class SimpleAndroidSensor(
        type: Int
) : TramontanaSensor(), SensorEventListener {
    private val applicationContext: Context by inject()
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

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
}

class Proximity : SimpleAndroidSensor(Sensor.TYPE_PROXIMITY) {
    override fun onSensorChanged(event: SensorEvent) {
        eventSink.onEvent(Event.Distance(event.values[0]))
    }
}

const val HALF_GRAVITY = SensorManager.GRAVITY_EARTH / 2

class Orientation : SimpleAndroidSensor(Sensor.TYPE_ACCELEROMETER) {
    private val tag = "Orientation"
    private var lastDirection: Event.Orientation.Direction? = null

    override fun onSensorChanged(event: SensorEvent) {
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        val direction = when {
            z > HALF_GRAVITY -> Event.Orientation.Direction.FACING_UP
            z < -HALF_GRAVITY -> Event.Orientation.Direction.FACING_DOWN
            y > HALF_GRAVITY -> Event.Orientation.Direction.PORTRAIT
            y < -HALF_GRAVITY -> Event.Orientation.Direction.PORTRAIT_UPSIDE_DOWN
            x > HALF_GRAVITY -> Event.Orientation.Direction.LANDSCAPE_LEFT
            x < -HALF_GRAVITY -> Event.Orientation.Direction.LANDSCAPE_RIGHT
            else -> {
                Log.w(tag, String.format("unable to compute orientation: x %-5.3f y %-5.3f z %-5.3f", x, y, z))
                null
            }
        } ?: return
        if (direction != lastDirection) {
            eventSink.onEvent(Event.Orientation(direction))
            lastDirection = direction
        }
    }
}

class Magnetometer : SimpleAndroidSensor(Sensor.TYPE_MAGNETIC_FIELD) {
    override fun onSensorChanged(event: SensorEvent) {
        val magnitude = Math.sqrt(event.values.map { it * it }.fold(0.0) { sum, x -> sum + x })
        eventSink.onEvent(Event.Magnetometer(if (magnitude > 70.0) 1 else 0, magnitude.toFloat()))
    }
}