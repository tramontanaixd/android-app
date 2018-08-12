package com.pierdr.pierluigidallarosa.myactivity

import kotlinx.coroutines.experimental.channels.ReceiveChannel

interface DirectiveSource {
    fun produceDirectives(): ReceiveChannel<Directive>
}

