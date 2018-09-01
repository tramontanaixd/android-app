package com.pierdr.tramontana.model

sealed class Event {
    data class TouchStart(
            val x: Int,
            val y: Int
    ) : Event()

    data class MultiTouchStart(
            val touches: List<TouchPoint>
    ) : Event()

    data class TouchDrag(
            val x: Int,
            val y: Int
    ) : Event()

    data class MultiTouchDrag(
            val touches: List<TouchPoint>
    ) : Event()

    data class TouchEnd(
            val x: Int,
            val y: Int
    ) : Event()

    data class MultiTouchEnd(
            val touches: List<TouchPoint>
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