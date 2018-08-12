package com.pierdr.pierluigidallarosa.myactivity

import android.widget.TextView


internal class ConsoleManager(private val c1: TextView) {

    private val messages = MutableList(3) {""}

    fun addNewMessage(newMessage: String) {
        messages[2] = messages[1]
        messages[1] = messages[0]
        messages[0] = newMessage
        c1.text = String.format("-> %s\n->%s\n->%s", messages[0], messages[1], messages[2])
    }


}
