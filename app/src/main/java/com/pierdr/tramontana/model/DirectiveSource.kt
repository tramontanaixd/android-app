package com.pierdr.tramontana.model

import kotlinx.coroutines.experimental.channels.ReceiveChannel

interface DirectiveSource {
    fun produceDirectives(): ReceiveChannel<Directive>
}

