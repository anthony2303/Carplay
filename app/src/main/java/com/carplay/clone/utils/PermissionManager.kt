package com.carplay.clone.utils

import android.app.Activity
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class PermissionManager(private val activity: Activity) {
    
    fun isPermissionGranted(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            activity,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    fun requestPermission(permission: String, requestCode: Int) {
        if (!isPermissionGranted(permission)) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(permission),
                requestCode
            )
        }
    }
    
    fun requestMultiplePermissions(permissions: Array<String>, requestCode: Int) {
        val notGranted = permissions.filter { !isPermissionGranted(it) }
        if (notGranted.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                activity,
                notGranted.toTypedArray(),
                requestCode
            )
        }
    }
    
    fun shouldShowRationale(permission: String): Boolean {
        return ActivityCompat.shouldShowRequestPermissionRationale(
            activity,
            permission
        )
    }
}
