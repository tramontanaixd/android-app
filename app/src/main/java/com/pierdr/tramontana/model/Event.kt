package com.pierdr.tramontana.model

sealed class Event {
    data class TouchDown(
            val x: Int,
            val y: Int
    ) : Event()

    data class MultiTouchDown(
            val touches: List<TouchPoint>
    ) : Event()

    data class Touched(
            val x: Int,
            val y: Int
    ) : Event()

    data class Attitude(
            val roll: Float,
            val pitch: Float,
            val yaw: Float
    ) : Event()

    data class Distance(
            val distance: Float
    ) : Event()

    object VideoEnded : Event()
}

data class TouchPoint(
        val x: Int,
        val y: Int
)