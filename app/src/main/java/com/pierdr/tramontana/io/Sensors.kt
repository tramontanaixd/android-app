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

class Sensors(
        private val applicationContext: Context,
        private val eventSink: EventSink,
        private val userReporter: UserReporter
) : LifecycleObserver, SensorEventListener {
    private val tag = "Sensors"

    private val sensorManager by lazy { applicationContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager }

    private val availableSensors = HashMap<Type, Sensor>()

    private val registeredSensors = HashSet<Sensor>()

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun collectAvailableSensors() {
        for (type in Type.values()) {
            availableSensors[type] = sensorManager.getDefaultSensor(type.toAndroidSensorType())
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun unregisterAllSensors() {
        for (sensor in registeredSensors) {
            sensorManager.unregisterListener(this, sensor)
        }
        registeredSensors.clear()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onSensorChanged(event: SensorEvent) {
        Log.d(tag, "onSensorChanged $event")
        for ((type, sensor) in availableSensors.entries) {
            if (event.sensor != sensor) continue
            when (type) {
                Sensors.Type.PROXIMITY -> eventSink.onEvent(Event.Distance(event.values[0]))
                Sensors.Type.ROTATION -> eventSink.onEvent(Event.Attitude(event.values[0], event.values[1], event.values[2]))
                Sensors.Type.ORIENTATION -> TODO("determine orientation event format")
                Sensors.Type.MAGNETOMETER -> TODO("determina magnetometer event format")
            }.javaClass // for "exhaustive when", see https://youtrack.jetbrains.com/issue/KT-12380#focus=streamItem-27-2727497-0-0
        }
    }

    fun startSensor(type: Type, samplingPeriodUs: Int = SensorManager.SENSOR_DELAY_UI) {
        val sensor = availableSensors[type]
        if (sensor == null) {
            userReporter.showWarning("Sensor of type $type is not available on this device.")
            return
        }
        if (registeredSensors.contains(sensor)) return
        sensorManager.registerListener(this, sensor, samplingPeriodUs)
        registeredSensors.add(sensor)
    }

    fun stopSensor(type: Type) {
        val sensor = availableSensors[type] ?: return
        if (!registeredSensors.contains(sensor)) return
        sensorManager.unregisterListener(this, sensor)
        registeredSensors.remove(sensor)
    }

    enum class Type {
        PROXIMITY,
        ROTATION,
        ORIENTATION,
        MAGNETOMETER
    }

    private fun Type.toAndroidSensorType(): Int = when (this) {
        Sensors.Type.PROXIMITY -> Sensor.TYPE_PROXIMITY
        Sensors.Type.ROTATION -> Sensor.TYPE_ROTATION_VECTOR
        Sensors.Type.ORIENTATION -> Sensor.TYPE_ORIENTATION // TODO use non-deprecated orientation sensor
        Sensors.Type.MAGNETOMETER -> Sensor.TYPE_MAGNETIC_FIELD
    }
}