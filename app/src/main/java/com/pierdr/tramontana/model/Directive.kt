package com.pierdr.tramontana.model

/**
 * A directive is a message sent by the Tramontana host to run a given action,
 * with some parameters if required.
 * These are only the abstract definitions of directives; other classes define how they are produced
 * and used.
 */
sealed class Directive {
    interface NeedsUi
    interface StartsEventsToClient
    interface StopsEventsToClient
    interface StartsEventsToOSC
    interface StopsEventsToOSC

    object MakeVibrate : Directive()

    data class SetColor(
            val red: Int,
            val green: Int,
            val blue: Int,
            val alpha: Int
    ) : Directive(), NeedsUi

    data class TransitionColors(
            val fromRed: Int,
            val fromGreen: Int,
            val fromBlue: Int,
            val fromAlpha: Int,
            val toRed: Int,
            val toGreen: Int,
            val toBlue: Int,
            val toAlpha: Int,
            val duration: Int
    ) : Directive(), NeedsUi

    data class SetBrightness(
            val brightness: Float
    ) : Directive(), NeedsUi

    data class SetLed(
            val intensity: Float
    ) : Directive(), NeedsUi

    data class PulseLed(
            val numberOfPulses: Int,
            val durationMillis: Long,
            val intensity: Float
    ) : Directive()

    data class ShowImage(
            val url: String
    ) : Directive(), NeedsUi

    data class PlayVideo(
            val url: String
    ) : Directive(), NeedsUi

    data class PlayAudio(
            val url: String
    ) : Directive(), NeedsUi

    data class RegisterTouch(
            val multi: Boolean,
            val drag: Boolean
    ) : Directive(), NeedsUi, StartsEventsToClient

    object ReleaseTouch : Directive(), NeedsUi, StopsEventsToClient

    object RegisterDistance : Directive(), StartsEventsToClient

    object ReleaseDistance : Directive(), StopsEventsToClient

    object RegisterOrientation : Directive(), StartsEventsToClient

    object ReleaseOrientation : Directive(), StopsEventsToClient

    data class RegisterAttitude(
            val updateRate: Float
    ) : Directive(), StartsEventsToClient

    object ReleaseAttitude : Directive(), StopsEventsToClient

    object RegisterMagnetometer : Directive(), StartsEventsToClient

    object ReleaseMagnetometer : Directive(), StopsEventsToClient

    object GetBattery : Directive()

    object RegisterPowerSource : Directive(), StartsEventsToClient

    object ReleasePowerSource : Directive(), StopsEventsToClient

    object RegisterAudioJack : Directive(), StartsEventsToClient

    object ReleaseAudioJack : Directive(), StopsEventsToClient

    data class SendAttitudeToOSC(
            val address: String,
            val port: Int,
            val updateRate: Float
    ) : Directive(), StartsEventsToOSC

    object StopAttitudeToOSC : Directive(), StopsEventsToOSC

    data class SendTouchToOSC(
            val address: String,
            val port: Int,
            val maxNumFingers: Int
    ) : Directive(), StartsEventsToOSC, StopsEventsToOSC

    object StopTouchToOSC : Directive()
}

object StartUi : Directive.NeedsUi

object StopUi : Directive.NeedsUi