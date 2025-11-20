package com.test.hypernotification.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SettingsViewModel : ViewModel() {
    private val _apiToken = MutableStateFlow("")
    val apiToken: StateFlow<String> = _apiToken

    private val _enableFocusNotification = MutableStateFlow(true)
    val enableFocusNotification: StateFlow<Boolean> = _enableFocusNotification

    private val _vibrationFeedback = MutableStateFlow(true)
    val vibrationFeedback: StateFlow<Boolean> = _vibrationFeedback

    private val _tileDelay = MutableStateFlow(1000)
    val tileDelay: StateFlow<Int> = _tileDelay

    fun updateApiToken(token: String) {
        _apiToken.value = token
    }

    fun updateFocusNotification(enabled: Boolean) {
        _enableFocusNotification.value = enabled
    }

    fun updateVibrationFeedback(enabled: Boolean) {
        _vibrationFeedback.value = enabled
    }

    fun updateTileDelay(delay: Int) {
        _tileDelay.value = delay
    }

    fun loadSettings(context: Context) {
        val prefs = context.getSharedPreferences("config", Context.MODE_PRIVATE)
        _apiToken.value = prefs.getString("token", "") ?: ""
        _enableFocusNotification.value = prefs.getBoolean("enable_focus_notification", true)
        _vibrationFeedback.value = prefs.getBoolean("vibration_feedback", true)
        _tileDelay.value = prefs.getInt("tile_click_delay", 1000)
    }

    fun saveSettings(context: Context) {
        val prefs = context.getSharedPreferences("config", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString("token", _apiToken.value)
            putBoolean("enable_focus_notification", _enableFocusNotification.value)
            putBoolean("vibration_feedback", _vibrationFeedback.value)
            putInt("tile_click_delay", _tileDelay.value)
            apply()
        }
    }
}