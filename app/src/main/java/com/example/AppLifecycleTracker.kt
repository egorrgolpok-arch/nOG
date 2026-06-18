package com.example

object AppLifecycleTracker {
    @Volatile
    var isAppInForeground: Boolean = false
}
