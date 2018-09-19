package com.pierdr.tramontana.io

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.pierdr.tramontana.model.Event
import com.pierdr.tramontana.model.Server
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject

class PowerMonitor : KoinComponent {
    private val applicationContext: Context by inject()
    private val server: Server by inject()

    fun sendBatteryLevel() {
        server.currentClientSession?.sendEvent(Event.BatteryLevel(batteryLevel))
    }

    private val batteryLevel: Float
        get() {
            val batteryStatusIntent: Intent = applicationContext.registerReceiver(
                    null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                    ?: throw IllegalStateException("no battery status available")
            val level: Int = batteryStatusIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale: Int = batteryStatusIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            return level / scale.toFloat()
        }
}
