package com.carplay.clone.utils

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import java.util.Calendar

class NightModeManager(private val context: Context) {
    
    fun applyNightMode() {
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        
        when {
            currentHour >= 19 || currentHour < 6 -> {
                // Modo noche: 7 PM - 6 AM
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }
            else -> {
                // Modo día
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }
    }
    
    fun isNightMode(): Boolean {
        val nightModeFlags = context.resources.configuration.uiMode and 
            Configuration.UI_MODE_NIGHT_MASK
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES
    }
}
