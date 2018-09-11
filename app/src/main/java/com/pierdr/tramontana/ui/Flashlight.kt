package com.pierdr.tramontana.ui

import android.hardware.camera2.CameraManager
import android.os.Build
import com.pierdr.tramontana.model.UserReporter
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject

class Flashlight : KoinComponent {
    private val cameraManager: CameraManager by inject()
    private val userReporter: UserReporter by inject()

    fun set(value: Float) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            userReporter.showWarning("Camera not available, unable to set flashlight")
            return
        } else {
            val cameraId: String = cameraManager.cameraIdList[0]
            cameraManager.setTorchMode(cameraId, value > 0)
        }
    }

}