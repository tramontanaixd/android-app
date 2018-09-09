package com.pierdr.tramontana.io

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
import com.pierdr.tramontana.model.UserReporter
import java.util.*
import kotlin.reflect.KClass

class Sensors(
        applicationContext: Context,
        eventSink: EventSink,
        private val userReporter: UserReporter
) : LifecycleObserver {
    private val availableSensors: Map<KClass<out TramontanaSensor>, TramontanaSensor>

    init {
        val allSensors: List<TramontanaSensor> = listOf(
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

class Attitude(eventSink: EventSink, applicationContext: Context) : TramontanaSensor(eventSink), SensorEventListener {
    private val tag = "Orientation"
    private val rotationMatrix = FloatArray(16)
    private val inclinationMatrix = FloatArray(9)
    private val orientationVector = FloatArray(3)

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
        if (!isRunning) return
        sensors.forEach {
            sensorManager.unregisterListener(this, it)
        }
        isRunning = false
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) { }

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

const val HALF_GRAVITY = SensorManager.GRAVITY_EARTH / 2

class Orientation(eventSink: EventSink, applicationContext: Context) : SimpleAndroidSensor(eventSink, applicationContext, Sensor.TYPE_ACCELEROMETER) {
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

class Magnetometer(eventSink: EventSink, applicationContext: Context) : SimpleAndroidSensor(eventSink, applicationContext, Sensor.TYPE_MAGNETIC_FIELD) {
    override fun onSensorChanged(event: SensorEvent?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}