package com.nilam.iptv

import android.app.Application

class NilamApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Firebase.initialize(this) // ready for future Firebase integration
    }
}
