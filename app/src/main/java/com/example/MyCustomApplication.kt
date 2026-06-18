package com.example

import android.app.Application
import android.content.Context
import android.os.Build
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

class MyCustomApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                AppLifecycleTracker.isAppInForeground = true
            }

            override fun onStop(owner: LifecycleOwner) {
                AppLifecycleTracker.isAppInForeground = false
            }
        })
    }

    override fun attachBaseContext(base: Context) {
        val attributionContext = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            base.createAttributionContext("nog_default_attribution")
        } else {
            base
        }
        super.attachBaseContext(attributionContext)
    }
}
