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

    data class Orientation(
            val orientation: Direction
    ) : Event() {
        enum class Direction {
            PORTRAIT,
            PORTRAIT_UPSIDE_DOWN,
            LANDSCAPE_LEFT,
            LANDSCAPE_RIGHT,
            FACING_UP,
            FACING_DOWN
        }
    }

    data class Distance(
            val distance: Float
    ) : Event()

    data class Magnetometer(
            val threshold: Int,
            val intensity: Float
    ) : Event()

    object VideoEnded : Event()

    data class BatteryLevel(
        val fraction: Float
    ) : Event()

    data class PowerSourceChanged(
            val pluggedIn: Boolean
    ) : Event()
}

data class TouchPoint(
        val x: Int,
        val y: Int
)