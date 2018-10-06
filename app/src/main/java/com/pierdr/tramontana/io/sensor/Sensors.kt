package com.pierdr.tramontana.io.sensor

import android.arch.lifecycle.LifecycleObserver
import android.hardware.SensorManager
import android.util.Log
import com.pierdr.tramontana.model.UserReporter
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import kotlin.reflect.KClass

/**
 * Handles sensor-like event sources that a client can subscribe and unsubscribe to.
 */
class Sensors : LifecycleObserver, KoinComponent {
    private val tag = "Sensors"
    private val userReporter: UserReporter by inject()
    private val availableSensors: Map<KClass<out TramontanaSensor>, TramontanaSensor>

    init {
        val allSensors: List<TramontanaSensor> = listOf(
                Proximity(),
                Attitude(),
                Orientation(),
                Magnetometer(),
                PowerSource(),
                AudioJack()
        )
        availableSensors = allSensors
                .filter { it.isAvailable }
                .associateBy { it::class }
    }

    fun stopAll() {
        Log.d(tag, "unregisterAllSensors()")
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