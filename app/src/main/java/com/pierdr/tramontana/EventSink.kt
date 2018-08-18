package com.pierdr.tramontana

sealed class Event {
    data class TouchDown(
            val x: Int,
            val y: Int
    ) : Event()

    data class Touched(
            val x: Int,
            val y: Int
    ) : Event()
}

interface EventSink {
    fun onEvent(event: Event)
}