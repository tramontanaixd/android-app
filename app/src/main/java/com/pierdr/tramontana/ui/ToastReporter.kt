package com.pierdr.tramontana.ui

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.pierdr.tramontana.model.UserReporter

class ToastReporter(
        private val context: Context
) : UserReporter {
    private val tag = "ToastReporter"

    override fun showWarning(message: String) {
        Log.i(tag, "toast showed: \"$message\"")
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
}