package com.utilitybox.app.util

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager

/**
 * The id of a camera with a flash unit, preferring the back camera. Returns
 * null when the device has no flash, or when the camera service cannot be
 * queried.
 *
 * Toggling the torch through [CameraManager.setTorchMode] needs no camera
 * permission, which is why the flashlight tool and the home screen widget can
 * both use it without prompting.
 */
fun CameraManager.torchCameraId(): String? = runCatching {
    val withFlash = cameraIdList.filter { id ->
        getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
    }
    withFlash.firstOrNull { id ->
        getCameraCharacteristics(id).get(CameraCharacteristics.LENS_FACING) ==
            CameraCharacteristics.LENS_FACING_BACK
    } ?: withFlash.firstOrNull()
}.getOrNull()
