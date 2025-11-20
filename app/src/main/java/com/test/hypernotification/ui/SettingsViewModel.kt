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

    private val _autoRecognition = MutableStateFlow(false)
    val autoRecognition: StateFlow<Boolean> = _autoRecognition

    private val _vibrationFeedback = MutableStateFlow(true)
    val vibrationFeedback: StateFlow<Boolean> = _vibrationFeedback

    fun updateApiToken(token: String) {
        viewModelScope.launch {
            _apiToken.value = token
            // TODO: 保存到SharedPreferences
        }
    }

    fun updateFocusNotification(enabled: Boolean) {
        viewModelScope.launch {
            _enableFocusNotification.value = enabled
            // TODO: 保存到SharedPreferences
        }
    }

    fun updateAutoRecognition(enabled: Boolean) {
        viewModelScope.launch {
            _autoRecognition.value = enabled
            // TODO: 保存到SharedPreferences
        }
    }

    fun updateVibrationFeedback(enabled: Boolean) {
        viewModelScope.launch {
            _vibrationFeedback.value = enabled
            // TODO: 保存到SharedPreferences
        }
    }

    fun loadSettings(context: Context) {
        val prefs = context.getSharedPreferences("config", Context.MODE_PRIVATE)
        _apiToken.value = prefs.getString("token", "") ?: ""
        _enableFocusNotification.value = prefs.getBoolean("enable_focus_notification", true)
        _autoRecognition.value = prefs.getBoolean("auto_recognition", false)
        _vibrationFeedback.value = prefs.getBoolean("vibration_feedback", true)
    }

    fun saveSettings(context: Context) {
        val prefs = context.getSharedPreferences("config", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString("token", _apiToken.value)
            putBoolean("enable_focus_notification", _enableFocusNotification.value)
            putBoolean("auto_recognition", _autoRecognition.value)
            putBoolean("vibration_feedback", _vibrationFeedback.value)
            apply()
        }
    }
}