package com.pierdr.pierluigidallarosa.myactivity

import com.pierdr.tramontana.model.Directive
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch

interface DirectiveExecutor {
    fun executeDirective(directive: Directive)
}

class AndroidDirectiveExecutor(
        private val activity: MainActivity
) : DirectiveExecutor {
    override fun executeDirective(directive: Directive) {
        launch(UI) {
            executeDirectiveOnUiThread(directive)
        }
    }

    private fun executeDirectiveOnUiThread(directive: Directive) {
        when (directive) {
            is Directive.PlayVideo -> activity.playVideo(directive.url)
            else -> TODO("this code will all go away in favor of ShowtimeFragment")
        }.javaClass // .javaClass is added to make an "exhaustive when", see https://youtrack.jetbrains.com/issue/KT-12380#focus=streamItem-27-2727497-0-0
    }
}