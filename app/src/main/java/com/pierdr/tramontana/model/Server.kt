package com.pierdr.tramontana.model

import kotlinx.coroutines.experimental.channels.ReceiveChannel

interface Server {
    /**
     * Starts the server.
     *
     * This method will suspend until the server is successfully started, or an error occurs.
     */
    suspend fun start()

    /**
     * Stops the server.
     *
     * This will also stop all the connections, and any receive channels returned by
     * [produceClientSessions] will stop too.
     */
    fun stop()

    /**
     * Returns a [ReceiveChannel] that emits [ClientSession]s every time a new Tramontana client
     * connects to us. Only one connection at a time is supported. The channel stops when the server
     * is stopped.
     */
    fun produceClientSessions(): ReceiveChannel<ClientSession>
}