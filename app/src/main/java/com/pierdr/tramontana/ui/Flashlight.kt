package com.pierdr.tramontana.ui

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import com.pierdr.tramontana.model.UserReporter
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject

class Flashlight : KoinComponent {
    private val cameraManager: CameraManager by inject()
    private val userReporter: UserReporter by inject()

    private val cameraId by lazy {
        cameraManager.cameraIdList.firstOrNull { cameraId ->
            cameraManager.getCameraCharacteristics(cameraId).get(CameraCharacteristics.FLASH_INFO_AVAILABLE)
        }
    }

    fun set(value: Float) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            userReporter.showWarning("Camera not available, unable to set flashlight")
        } else {
            val cameraIdLocal = cameraId
            if (cameraIdLocal == null) {
                userReporter.showWarning("No flashlight available")
                return
            }
            cameraManager.setTorchMode(cameraId, value > 0)
        }
    }

    fun pulse(numberOfPulses: Int, durationMillis: Int) {
        // TODO cancel previous pulse job
        launch {
            for (i in 0 until numberOfPulses) {
                set(1f)
                delay(durationMillis)
                set(0f)
                delay(durationMillis)
            }
        }
    }

}