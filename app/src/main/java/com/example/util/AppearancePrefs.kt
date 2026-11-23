package com.example.util

import android.content.Context
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppThemeMode(val label: String, val subtitle: String) {
    SYSTEM("System", "Match device"),
    LIGHT("Light", "Bright ledger"),
    DARK("Dark", "Charcoal dark")
}

enum class AppTextSize(val label: String, val scale: Float, val percentLabel: String) {
    SMALL("Small", 0.90f, "90%"),
    MEDIUM("Medium", 1.00f, "100%"),
    LARGE("Large", 1.13f, "113%")
}

enum class AppTextFormat(val label: String, val subtitle: String) {
    MODERN("Modern Sans", "Plus Jakarta Sans & Inter"),
    ROUNDED("Soft Rounded", "Nunito clean rounded"),
    SERIF("Classic Serif", "Lora editorial ledger"),
    MONO("Ledger Mono", "Roboto Mono accounting"),
    SYSTEM("System Default", "Device default font")
}

@Immutable
data class AppearancePreferences(
    val themeMode: AppThemeMode = AppThemeMode.DARK,
    val textSize: AppTextSize = AppTextSize.MEDIUM,
    val textFormat: AppTextFormat = AppTextFormat.MODERN
) {
    fun summaryLabel(): String = "${themeMode.label} • ${textSize.label} • ${textFormat.label}"
}

object AppearanceManager {
    private const val PREFS_NAME = "money_ledger_appearance_prefs"
    private const val KEY_THEME_MODE = "theme_mode"
    private const val KEY_TEXT_SIZE = "text_size"
    private const val KEY_TEXT_FORMAT = "text_format"

    private val _state = MutableStateFlow(AppearancePreferences())
    val state: StateFlow<AppearancePreferences> = _state.asStateFlow()

    @Volatile
    private var initialized = false

    fun init(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val theme = runCatching {
                AppThemeMode.valueOf(prefs.getString(KEY_THEME_MODE, AppThemeMode.DARK.name) ?: AppThemeMode.DARK.name)
            }.getOrDefault(AppThemeMode.DARK)
            val size = runCatching {
                AppTextSize.valueOf(prefs.getString(KEY_TEXT_SIZE, AppTextSize.MEDIUM.name) ?: AppTextSize.MEDIUM.name)
            }.getOrDefault(AppTextSize.MEDIUM)
            val format = runCatching {
                AppTextFormat.valueOf(prefs.getString(KEY_TEXT_FORMAT, AppTextFormat.MODERN.name) ?: AppTextFormat.MODERN.name)
            }.getOrDefault(AppTextFormat.MODERN)
            _state.value = AppearancePreferences(themeMode = theme, textSize = size, textFormat = format)
            initialized = true
        }
    }

    fun setThemeMode(context: Context, mode: AppThemeMode) {
        init(context)
        if (_state.value.themeMode == mode) return
        _state.value = _state.value.copy(themeMode = mode)
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_THEME_MODE, mode.name)
            .apply()
    }

    fun setTextSize(context: Context, size: AppTextSize) {
        init(context)
        if (_state.value.textSize == size) return
        _state.value = _state.value.copy(textSize = size)
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_TEXT_SIZE, size.name)
            .apply()
    }

    fun setTextFormat(context: Context, format: AppTextFormat) {
        init(context)
        if (_state.value.textFormat == format) return
        _state.value = _state.value.copy(textFormat = format)
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_TEXT_FORMAT, format.name)
            .apply()
    }

    fun resetDefaults(context: Context) {
        init(context)
        val defaults = AppearancePreferences()
        _state.value = defaults
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_THEME_MODE, defaults.themeMode.name)
            .putString(KEY_TEXT_SIZE, defaults.textSize.name)
            .putString(KEY_TEXT_FORMAT, defaults.textFormat.name)
            .apply()
    }
}
