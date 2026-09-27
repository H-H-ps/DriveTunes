package com.drivetunes

import android.app.Application

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        PlayStats.init(this)
        Favorites.init(this)
    }
}
