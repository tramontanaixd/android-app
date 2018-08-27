package com.pierdr.tramontana.model

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

    // TODO directive transitionColors
    // TODO directive pulseLED
    /*IT WORKS AS FOLLOW:
        receives 3 parameters:
        1. duration
        2. times
        3. intensity

        The incoming message is:
        "{\"m\":\"pulseLED\",\"t\":\""+numberOfPulses+"\",\"d\":\""+duration+"\",\"i\":\""+intensity+"\"}"
        The method might be something like:

        onPulseFlashLight(json.getFloat("t"),json.getFloat("i"),json.getFloat("d"));

    */
    // TODO directive loopVideo
    // TODO directive getBattery
    //RETURNS A MESSAGE LIKE
    // "{\"m\":\"battery\",\"v\":\"0.05\"}"
    // TODO directive playAudio
    // TODO directive registerOrientation
    // TODO directive releaseOrientation
    // TODO directive registerMagnetometer
    // TODO directive releaseMagnetometer
    // TODO directive registerPowerSource
    // TODO directive releasePowerSource
    // TODO directive registerAudioJack
    // TODO directive releaseAudioJack

}
