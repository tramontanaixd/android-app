package com.pierdr.tramontana.io.sensor

import com.pierdr.tramontana.model.EventSink
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject

abstract class TramontanaSensor : KoinComponent {
    val eventSink: EventSink by inject()
    abstract val isAvailable: Boolean
    abstract fun start(samplingPeriodUs: Int)
    abstract val isRunning: Boolean
    abstract fun stop()
}