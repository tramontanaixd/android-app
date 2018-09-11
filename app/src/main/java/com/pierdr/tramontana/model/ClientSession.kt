package com.pierdr.tramontana.model

import kotlinx.coroutines.experimental.channels.SubscriptionReceiveChannel

/**
 * Represents an established connection with a client, from which one can exchange data. The
 * connection may be stopped either by our side or by the remote side.
 *
 * Before sending events or start receiving directive, check that the connection is still alive by
 * calling [isClosed].
 */
interface ClientSession {
    fun sendEvent(event: Event)
    fun subscribeToDirectives(): SubscriptionReceiveChannel<Directive>
    val isClosed: Boolean
    fun close()
}