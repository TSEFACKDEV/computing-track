package com.klein.ictcomputing

import android.app.Application
import com.klein.ictcomputing.alarm.AlarmSystem
import com.klein.ictcomputing.data.AppRepository

open class IctApp : Application() {
    lateinit var repo: AppRepository
        private set

    override fun onCreate() {
        super.onCreate()
        repo = AppRepository(this)
        if (AlarmSystem.enabled(this)) {
            AlarmSystem.schedule(this)
        }
    }

    fun currentMorningSessionOpen(): Boolean = false
}
