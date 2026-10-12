package com.example.spamshield

import android.app.Application
import androidx.room.Room

class SpamApplication : Application() {
    lateinit var database: SpamDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(
            applicationContext,
            SpamDatabase::class.java,
            "spamshield.db"
        ).fallbackToDestructiveMigration().build()
    }
}
