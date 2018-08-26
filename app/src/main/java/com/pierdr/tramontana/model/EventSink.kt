package com.pierdr.tramontana.model

interface EventSink {
    fun onEvent(event: Event)
}