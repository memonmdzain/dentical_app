package com.dentical.staff

import android.app.Application
import android.content.Context
import com.dentical.staff.data.remote.SyncManager
import dagger.hilt.android.HiltAndroidApp
import java.io.File
import javax.inject.Inject

@HiltAndroidApp
class DenticalApplication : Application() {

    // Eager-init so the ProcessLifecycleOwner observer is registered before any Activity starts.
    @Inject lateinit var syncManager: SyncManager

    override fun onCreate() {
        super.onCreate()
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val msg = "Thread: ${thread.name}\n\n${throwable.stackTraceToString()}".take(8000)
                // Write to both file and SharedPreferences for reliability
                File(filesDir, "crash.txt").writeText(msg)
                getSharedPreferences("crash", Context.MODE_PRIVATE)
                    .edit().putString("trace", msg).commit()
            } catch (_: Throwable) {}
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
