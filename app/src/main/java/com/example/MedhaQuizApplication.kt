package com.example

import android.app.Application
import com.example.core.appcheck.AppCheckManager
import com.google.firebase.FirebaseApp

class MedhaQuizApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        try {
            FirebaseApp.initializeApp(this)
        } catch (e: Exception) {
            // Already initialized or will initialize via Google Services plugin
        }
        AppCheckManager.initialize(this)
    }
}
