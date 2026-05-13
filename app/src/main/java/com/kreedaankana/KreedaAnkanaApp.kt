package com.kreedaankana

import android.app.Application
import com.google.firebase.FirebaseApp

class KreedaAnkanaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }
}
