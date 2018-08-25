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


    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun start() {
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun stop() {
        // TODO unregister all sensors
    }


    fun startDistance() {
        // TODO handle double registration
        sensorManager.registerListener(this, proximitySensor, SensorManager.SENSOR_DELAY_UI)
    }

    fun stopDistance() {
        sensorManager.unregisterListener(this, proximitySensor)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onSensorChanged(event: SensorEvent) {
        Log.d(tag, "onSensorChanged $event")
        when (event.sensor) {
            proximitySensor -> eventSink.onEvent(Event.Distance(event.values[0]))
        }
    }

}