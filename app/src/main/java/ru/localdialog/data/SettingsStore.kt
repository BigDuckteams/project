package ru.localdialog.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AppSettings(
    val selectedModelId: String = ModelCatalog.default.id,
    val automaticSpeakers: Boolean = true,
    val speakerCount: Int = 2,
    val clusterThreshold: Float = 0.5f,
    val darkTheme: Boolean = false,
    val whisperLanguage: String = "ru",
)

class SettingsStore(context: Context) {
    private val preferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val _settings = MutableStateFlow(read())
    val settings = _settings.asStateFlow()

    private fun read() = AppSettings(
        selectedModelId = preferences.getString("model", ModelCatalog.default.id)!!,
        automaticSpeakers = preferences.getBoolean("automatic", true),
        speakerCount = preferences.getInt("speakers", 2).coerceIn(1, 12),
        clusterThreshold = preferences.getFloat("threshold", 0.5f).coerceIn(0.1f, 0.95f),
        darkTheme = preferences.getBoolean("dark", false),
        whisperLanguage = preferences.getString("language", "ru")!!,
    )

    fun update(transform: (AppSettings) -> AppSettings) {
        val updated = transform(_settings.value)
        val value = updated.copy(speakerCount = updated.speakerCount.coerceIn(1, 12))
        preferences.edit()
            .putString("model", value.selectedModelId)
            .putBoolean("automatic", value.automaticSpeakers)
            .putInt("speakers", value.speakerCount)
            .putFloat("threshold", value.clusterThreshold)
            .putBoolean("dark", value.darkTheme)
            .putString("language", value.whisperLanguage)
            .apply()
        _settings.value = value
    }
}
