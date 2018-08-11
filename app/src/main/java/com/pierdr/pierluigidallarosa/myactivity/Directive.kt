package com.pierdr.pierluigidallarosa.myactivity

/**
 * A directive is a message sent by the Tramontana host to run a given action,
 * with some parameters if required.
 * These are only the abstract definitions of directives; other classes define how they are produced
 * and used.
 */
sealed class Directive {
    object MakeVibrate : Directive()

    data class SetColor(
            val red: Int,
            val green: Int,
            val blue: Int,
            val alpha: Int
    ) : Directive()

    data class SetBrightness(
            val brightness: Float
    ) : Directive()

    data class SetLed(
            val intensity: Float
    ) : Directive()

    data class ShowImage(
            val url: String
    ) : Directive()

    data class PlayVideo(
            val url: String
    ) : Directive()

    data class RegisterTouch(
            val multi: Boolean,
            val drag: Boolean
    ) : Directive()

    object ReleaseTouch : Directive()

    object RegisterDistance : Directive()

    object ReleaseDistance : Directive()

    data class RegisterAttitude(
            val updateRate: Float
    ) : Directive()

    object ReleaseAttitude : Directive()
}

