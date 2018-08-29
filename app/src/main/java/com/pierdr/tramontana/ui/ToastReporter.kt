package com.pierdr.tramontana.ui

import android.content.Context
import android.widget.Toast
import com.pierdr.tramontana.model.UserReporter

class ToastReporter(
        private val context: Context
) : UserReporter {
    override fun showWarning(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
}