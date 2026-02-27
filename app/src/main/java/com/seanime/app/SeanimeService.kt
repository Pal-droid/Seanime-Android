package com.seanime.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
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
    private val ACTION_STOP_SERVICE = "STOP_SEANIME_SERVICE"
    private lateinit var notificationManager: NotificationManager

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_SERVICE) {
            stopSelf()
            return START_NOT_STICKY
        }

        notificationManager = getSystemService(NotificationManager::class.java)
        createNotificationChannel()

        createFakeResolvConf()
        startBinary()

        updateNotification("Server is running")

        return START_STICKY
    }

    private fun createFakeResolvConf() {
        try {
            val resolvFile = File(filesDir, "resolv.conf")
            val content = "nameserver 8.8.8.8\nnameserver 1.1.1.1\n"
            FileOutputStream(resolvFile).use { it.write(content.toByteArray()) }
        } catch (e: Exception) {
            Log.e("SeanimeService", "Failed to create resolv.conf", e)
        }
    }

    private fun updateNotification(status: String) {
        val stopIntent = Intent(this, SeanimeService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }

        val pendingStopIntent = PendingIntent.getService(
            this, 0, stopIntent, 
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Seanime")
            .setContentText(status)
            .setSmallIcon(R.mipmap.ic_launcher) // Uses your app icon
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .addAction(
                Notification.Action.Builder(
                    android.R.drawable.ic_menu_close_clear_cancel,
                    "Exit", 
                    pendingStopIntent
                ).build()
            )
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
        } else {
            startForeground(NOTIF_ID, notification)
        }
    }

    private fun startBinary() {
        try {
            val binaryPath = File(applicationInfo.nativeLibraryDir, "libseanime.so")
            if (!binaryPath.exists()) {
                updateNotification("Error: Binary missing")
                return
            }

            val pb = ProcessBuilder(binaryPath.absolutePath, "--datadir", filesDir.absolutePath)
                .directory(filesDir)
                .redirectErrorStream(true)

            pb.environment().apply {
                put("GODEBUG", "netdns=cgo")
                put("RESOLV_CONF", File(filesDir, "resolv.conf").absolutePath)
                put("HOME", filesDir.absolutePath)
                put("TMPDIR", cacheDir.absolutePath)
            }

            process = pb.start()

            Thread {
                try {
                    process?.inputStream?.bufferedReader()?.use { it.forEachLine { line -> Log.d("SeanimeLog", line) } }
                } catch (e: Exception) {
                    Log.e("SeanimeService", "Stream Error", e)
                }
            }.start()

        } catch (e: Exception) {
            updateNotification("Server failed to start")
        }
    }

    override fun onDestroy() {
        process?.destroy()
        process = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "Seanime Server", NotificationManager.IMPORTANCE_LOW)
        notificationManager.createNotificationChannel(channel)
    }
}