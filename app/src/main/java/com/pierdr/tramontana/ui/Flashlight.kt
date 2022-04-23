package com.pierdr.tramontana.ui

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import com.pierdr.tramontana.model.UserReporter
import kotlinx.coroutines.*
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import kotlin.coroutines.CoroutineContext

class Flashlight : KoinComponent, CoroutineScope {
    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    private val cameraManager: CameraManager by inject()
    private val userReporter: UserReporter by inject()
    private var pulseJob: Job? = null

    private val cameraId by lazy {
        cameraManager.cameraIdList.firstOrNull { cameraId ->
            cameraManager.getCameraCharacteristics(cameraId).get(CameraCharacteristics.FLASH_INFO_AVAILABLE)
                ?: false
        }
    }

    fun set(value: Float) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            userReporter.showWarning("Camera not available, unable to set flashlight")
        } else {
            val cameraId = this.cameraId
            if (cameraId == null) {
                userReporter.showWarning("No flashlight available")
                return
            }
            cameraManager.setTorchMode(cameraId, value > 0)
        }
    }

    fun pulse(numberOfPulses: Int, durationMillis: Long) {
        val previousPulseJob = pulseJob
        pulseJob = launch {
            previousPulseJob?.cancelAndJoin()
            for (i in 0 until numberOfPulses) {
                set(1f)
                delay(durationMillis)
                set(0f)
                delay(durationMillis)
            }
        }
    }

    fun stop() {
        pulseJob?.cancel()
    }
}