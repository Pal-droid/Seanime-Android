package com.seanime.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import java.io.File
import java.io.FileOutputStream

class SeanimeService : Service() {

    private var process: Process? = null
    private val CHANNEL_ID = "seanime_channel"
    private val NOTIF_ID = 1
    private lateinit var notificationManager: NotificationManager

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        notificationManager = getSystemService(NotificationManager::class.java)
        createNotificationChannel()
        updateNotification("Starting native server...")
        
        // 1. Create the fake resolv.conf before starting
        createFakeResolvConf()
        
        startBinary()
        return START_STICKY
    }

    private fun createFakeResolvConf() {
        try {
            val resolvFile = File(filesDir, "resolv.conf")
            // Use Google and Cloudflare DNS
            val content = "nameserver 8.8.8.8\nnameserver 1.1.1.1\n"
            FileOutputStream(resolvFile).use { it.write(content.toByteArray()) }
            Log.d("SeanimeService", "Created resolv.conf at ${resolvFile.absolutePath}")
        } catch (e: Exception) {
            Log.e("SeanimeService", "Failed to create resolv.conf", e)
        }
    }

    private fun updateNotification(status: String) {
        val notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Seanime Server")
            .setContentText(status)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setOnlyAlertOnce(true)
            .build()

        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIF_ID, notification)
        }
    }

    private fun startBinary() {
        try {
            val binaryPath = File(applicationInfo.nativeLibraryDir, "libseanime.so")
            
            if (!binaryPath.exists()) {
                updateNotification("Error: libseanime.so missing")
                return
            }

            val pb = ProcessBuilder(binaryPath.absolutePath, "--datadir", filesDir.absolutePath)
                .directory(filesDir)
                .redirectErrorStream(true)

            // --- THE NEW DNS FIX STRATEGY ---
            // 1. Still try the CGO fix
            pb.environment()["GODEBUG"] = "netdns=cgo"
            // 2. Point to our fake resolv.conf (works for some Go versions/wrappers)
            pb.environment()["RESOLV_CONF"] = File(filesDir, "resolv.conf").absolutePath
            // 3. Set standard Linux home
            pb.environment()["HOME"] = filesDir.absolutePath
            // 4. Set TMPDIR to prevent crashes during temporary file creation
            pb.environment()["TMPDIR"] = cacheDir.absolutePath

            process = pb.start()

            Thread {
                try {
                    process?.inputStream?.bufferedReader()?.use { reader ->
                        reader.forEachLine { line ->
                            Log.d("SeanimeLog", line)
                            if (line.isNotBlank()) updateNotification(line.take(100))
                        }
                    }
                    val exitCode = process?.waitFor()
                    updateNotification("Process ended (Code: $exitCode)")
                } catch (e: Exception) {
                    Log.e("SeanimeService", "Stream Error", e)
                }
            }.start()

        } catch (e: Exception) {
            Log.e("SeanimeService", "Startup failure", e)
            updateNotification("Startup Exception: ${e.message}")
        }
    }

    override fun onDestroy() {
        process?.destroy()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "Seanime Server", NotificationManager.IMPORTANCE_LOW)
        notificationManager.createNotificationChannel(channel)
    }
}