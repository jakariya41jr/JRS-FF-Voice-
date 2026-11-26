package com.example

import android.app.Application
import com.example.audio.SoundEngine
import com.example.data.db.AppDatabase
import com.example.data.prefs.AppPreferences
import com.example.data.repository.SoundboardRepository
import com.example.system.RootShizukuController

class AppContainer(app: Application) {
    val prefs = AppPreferences(app)
    val database = AppDatabase.getInstance(app)
    val soundEngine = SoundEngine(app, prefs)
    val repository = SoundboardRepository(app, database, prefs, soundEngine)
    val rootShizukuController = RootShizukuController(prefs)
}

class VoxDeckApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
