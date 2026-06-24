package com.salesinventory.app

import android.app.Application
import android.os.Environment
import java.io.File
import java.io.FileWriter

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        val self = this
        Thread.setDefaultUncaughtExceptionHandler { _, e ->
            try {
                val dir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: filesDir
                val file = File(dir, "crash_log.txt")
                FileWriter(file).use { w ->
                    w.write("=== CRASH ===\n")
                    w.write("Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}\n")
                    w.write("Android: ${android.os.Build.VERSION.SDK_INT}\n")
                    w.write("${e.javaClass.name}: ${e.message}\n")
                    e.stackTrace.forEach { w.write("  at ${it.toString()}\n") }
                }
            } catch (_: Exception) {}
            val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
            defaultHandler?.uncaughtException(Thread.currentThread(), e)
        }
    }
}
