package com.volumemixer.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.volumemixer.app.databinding.FloatingPanelBinding
import rikka.shizuku.Shizuku

class FloatingPanelService : Service() {

    private lateinit var windowManager: WindowManager
    private var floatingView: View? = null
    private lateinit var binding: FloatingPanelBinding
    private lateinit var audioSessionManager: AudioSessionManager
    private lateinit var adapter: AppVolumeAdapter

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        audioSessionManager = AudioSessionManager(this)

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        setupFloatingPanel()
        startAudioMonitoring()
    }

    private fun setupFloatingPanel() {
        binding = FloatingPanelBinding.inflate(LayoutInflater.from(this))
        floatingView = binding.root

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 200
        }

        adapter = AppVolumeAdapter(
            onVolumeChanged = { packageName, level ->
                audioSessionManager.saveVolumeLevel(packageName, level)
                // التحكم الحقيقي في الصوت عبر Shizuku
                setVolumeWithShizuku(level)
            },
            onMuteToggled = { packageName, muted ->
                audioSessionManager.saveMuteState(packageName, muted)
                val volume = if (muted) 0 else audioSessionManager.getCurrentApps()
                    .find { it.packageName == packageName }?.volumeLevel ?: 80
                setVolumeWithShizuku(if (muted) 0 else volume)
            }
        )

        binding.appsRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.appsRecyclerView.adapter = adapter

        binding.btnClose.setOnClickListener { stopSelf() }

        binding.panelHeader.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(floatingView, params)
                    true
                }
                else -> false
            }
        }

        windowManager.addView(floatingView, params)
    }

    private fun setVolumeWithShizuku(level: Int) {
        try {
            if (Shizuku.pingBinder()) {
                // تحويل 0-100 إلى 0-15 (نطاق صوت أندرويد)
                val androidVolume = (level * 15 / 100)
                val process = Shizuku.newProcess(
                    arrayOf("cmd", "media_session", "volume",
                        "--stream", "3",
                        "--set", androidVolume.toString()),
                    null, null
                )
                process.waitFor()
            }
        } catch (e: Exception) {
            android.util.Log.e("FloatingPanel", "Shizuku volume error: ${e.message}")
        }
    }

    private fun startAudioMonitoring() {
        audioSessionManager.startMonitoring { apps ->
            if (apps.isEmpty()) {
                binding.emptyState.visibility = View.VISIBLE
                binding.appsRecyclerView.visibility = View.GONE
            } else {
                binding.emptyState.visibility = View.GONE
                binding.appsRecyclerView.visibility = View.VISIBLE
                adapter.updateItems(apps)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Volume Mixer", NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Volume Mixer يعمل في الخلفية" }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🎚️ Volume Mixer شغال")
            .setContentText("اضغط لفتح الإعدادات")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        audioSessionManager.stopMonitoring()
        floatingView?.let { windowManager.removeView(it) }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CHANNEL_ID = "volume_mixer_channel"
        private const val NOTIFICATION_ID = 1001
    }
}
