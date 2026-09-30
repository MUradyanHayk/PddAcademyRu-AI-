package ru.pdd.academy

import android.app.Application
import ru.pdd.academy.util.Log

class PddApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.init(this)
    }
}
