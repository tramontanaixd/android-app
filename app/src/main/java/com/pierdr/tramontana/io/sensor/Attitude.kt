package com.pierdr.tramontana.io.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import com.pierdr.tramontana.model.Event
import org.koin.standalone.inject
import java.util.*

class Attitude : TramontanaSensor(), SensorEventListener {
    private val tag = "Orientation"
    private val rotationMatrix = FloatArray(16)
    private val inclinationMatrix = FloatArray(9)
    private val orientationVector = FloatArray(3)

    private val applicationContext: Context by inject()
    private val sensorManager = applicationContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magneticField = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    private val sensors = listOf(accelerometer, magneticField)
    private val lastValues: MutableMap<Sensor, FloatArray> = HashMap()

    override val isAvailable: Boolean
        get() = sensors.all { it != null }

    override fun start(samplingPeriodUs: Int) {
        if (isRunning) return
        sensors.forEach {
            sensorManager.registerListener(this, it, samplingPeriodUs)
        }
        isRunning = true
    }

    override var isRunning = false

    override fun stop() {
        Log.d(tag, "stop() called")
        if (!isRunning) return
        sensors.forEach {
            sensorManager.unregisterListener(this, it)
        }
        Log.d(tag, "stop() sensors unregistered")
        isRunning = false
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}

    override fun onSensorChanged(event: SensorEvent) {
        lastValues[event.sensor] = event.values.clone()

        if (lastValues.size != sensors.size) return

        val computeSuccess = SensorManager.getRotationMatrix(rotationMatrix, inclinationMatrix, lastValues[accelerometer], lastValues[magneticField])
        if (!computeSuccess) {
            Log.w(tag, "failed to compute rotation matrix")
            return
        }

        SensorManager.getOrientation(rotationMatrix, orientationVector)

        val yaw = orientationVector[0]
        val pitch = orientationVector[1]
        val roll = orientationVector[2]
        eventSink.onEvent(Event.Attitude(roll, pitch, yaw))
    }
}