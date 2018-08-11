package com.pierdr.pierluigidallarosa.myactivity

import android.content.Context
import android.os.Vibrator
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch

interface DirectiveExecutor {
    fun executeDirective(directive: Directive)
}

class AndroidDirectiveExecutor(
        applicationContext: Context,
        private val activity: MainActivity
) : DirectiveExecutor {
    private val vibrer = applicationContext.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

    override fun executeDirective(directive: Directive) {
        launch(UI) {
            executeDirectiveOnUiThread(directive)
        }
    }

    private fun executeDirectiveOnUiThread(directive: Directive) {
        when (directive) {
            is Directive.MakeVibrate -> vibrer.vibrate(100)
            is Directive.SetColor -> activity.onSetColor(
                    directive.red,
                    directive.green,
                    directive.blue,
                    directive.alpha)
            is Directive.SetBrightness -> activity.setBrightness(directive.brightness)
            is Directive.SetLed -> activity.setFlashLight(directive.intensity)
            is Directive.ShowImage -> activity.showImage(directive.url)
            is Directive.PlayVideo -> activity.playVideo(directive.url)
            is Directive.RegisterTouch -> activity.startTouchListening(directive.multi, directive.drag)
            is Directive.ReleaseTouch -> activity.stopTouchListening()
            is Directive.RegisterDistance -> activity.startDistanceSensing()
            is Directive.ReleaseDistance -> activity.stopDistanceSensing()
            is Directive.RegisterAttitude -> activity.startAttitudeSensing(directive.updateRate)
            is Directive.ReleaseAttitude -> activity.stopAttitudeSensing()
        }.javaClass // .javaClass is added to make an "exhaustive when", see https://youtrack.jetbrains.com/issue/KT-12380#focus=streamItem-27-2727497-0-0
    }
}