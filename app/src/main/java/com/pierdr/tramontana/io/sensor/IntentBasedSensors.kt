package com.pierdr.tramontana.io.sensor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.pierdr.tramontana.model.Event
import org.koin.standalone.inject

abstract class IntentBasedSensor : TramontanaSensor() {
    private val applicationContext: Context by inject()
    private var receiver: BroadcastReceiver? = null

    override val isAvailable: Boolean
        get() = true

    override fun start(samplingPeriodUs: Int) {
        val receiverLocal = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent) {
                onIntent(intent)
            }
        }
        applicationContext.registerReceiver(receiverLocal, intentFilter)
        receiver = receiverLocal
    }

    abstract val intentFilter: IntentFilter
    abstract fun onIntent(intent: Intent)

    override val isRunning: Boolean
        get() = receiver != null

    override fun stop() {
        receiver?.let {
            applicationContext.unregisterReceiver(it)
            receiver = null
        }
    }
}

class PowerSource : IntentBasedSensor() {
    override val intentFilter: IntentFilter
        get() = IntentFilter(Intent.ACTION_BATTERY_CHANGED)

    override fun onIntent(intent: Intent) {
        val plugged: Int = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
        eventSink.onEvent(Event.PowerSourceChanged(plugged > 0))
    }
}

class AudioJack : IntentBasedSensor() {
    override val intentFilter: IntentFilter
        get() = IntentFilter(Intent.ACTION_HEADSET_PLUG)

    override fun onIntent(intent: Intent) {
        val plugged: Int = intent.getIntExtra("state", 0)
        eventSink.onEvent(Event.AudioJackChanged(plugged > 0))
    }
}