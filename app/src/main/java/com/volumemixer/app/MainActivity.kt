package com.volumemixer.app

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.volumemixer.app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
    }

    private fun setupUI() {
        // زر تشغيل اللوحة العائمة
        binding.btnStartMixer.setOnClickListener {
            if (canDrawOverlays()) {
                startFloatingService()
            } else {
                requestOverlayPermission()
            }
        }

        // زر إيقاف اللوحة
        binding.btnStopMixer.setOnClickListener {
            stopFloatingService()
        }

        // تحقق من الإذن وحدّث الواجهة
        updatePermissionStatus()
    }

    private fun canDrawOverlays(): Boolean {
        return Settings.canDrawOverlays(this)
    }

    private fun requestOverlayPermission() {
        Toast.makeText(this, "من فضلك اسمح للتطبيق بالظهور فوق التطبيقات الأخرى", Toast.LENGTH_LONG).show()
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION)
    }

    private fun startFloatingService() {
        val intent = Intent(this, FloatingPanelService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        binding.statusText.text = "✅ Volume Mixer شغال"
        Toast.makeText(this, "Volume Mixer شغال! هتلاقي الأيقونة العائمة", Toast.LENGTH_SHORT).show()
    }

    private fun stopFloatingService() {
        val intent = Intent(this, FloatingPanelService::class.java)
        stopService(intent)
        binding.statusText.text = "⏹️ Volume Mixer متوقف"
    }

    private fun updatePermissionStatus() {
        if (canDrawOverlays()) {
            binding.permissionStatus.text = "✅ إذن النافذة العائمة: ممنوح"
        } else {
            binding.permissionStatus.text = "❌ إذن النافذة العائمة: غير ممنوح — اضغط Start"
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_OVERLAY_PERMISSION) {
            updatePermissionStatus()
            if (canDrawOverlays()) {
                startFloatingService()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updatePermissionStatus()
    }

    companion object {
        private const val REQUEST_OVERLAY_PERMISSION = 1001
    }
}
