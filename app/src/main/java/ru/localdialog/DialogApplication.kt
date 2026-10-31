package ru.localdialog

import android.app.Application
import ru.localdialog.data.ModelRepository
import ru.localdialog.data.SettingsStore
import ru.localdialog.data.TranscriptStore

class DialogApplication : Application() {
    lateinit var store: TranscriptStore
        private set
    lateinit var models: ModelRepository
        private set
    lateinit var settingsStore: SettingsStore
        private set

    override fun onCreate() {
        super.onCreate()
        store = TranscriptStore(this)
        models = ModelRepository(this)
        settingsStore = SettingsStore(this)
    }
}
