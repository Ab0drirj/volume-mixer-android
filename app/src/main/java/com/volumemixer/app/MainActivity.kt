package com.volumemixer.app

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.volumemixer.app.databinding.ActivityMainBinding
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuProvider

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val shizukuPermissionListener = Shizuku.OnRequestPermissionResultListener { _, grantResult ->
        if (grantResult == PackageManager.PERMISSION_GRANTED) {
            binding.shizukuStatus.text = "✅ Shizuku: متصل — تحكم كامل في الصوت!"
            Toast.makeText(this, "✅ Shizuku شغال! التحكم في الصوت متاح", Toast.LENGTH_SHORT).show()
        } else {
            binding.shizukuStatus.text = "❌ Shizuku: تم الرفض"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Shizuku.addRequestPermissionResultListener(shizukuPermissionListener)
        setupUI()
    }

    private fun setupUI() {
        binding.btnStartMixer.setOnClickListener {
            if (canDrawOverlays()) {
                startFloatingService()
            } else {
                requestOverlayPermission()
            }
        }

        binding.btnStopMixer.setOnClickListener {
            stopFloatingService()
        }

        binding.btnConnectShizuku.setOnClickListener {
            connectShizuku()
        }

        updatePermissionStatus()
        updateShizukuStatus()
    }

    private fun connectShizuku() {
        try {
            if (!Shizuku.pingBinder()) {
                Toast.makeText(this, "❌ Shizuku مش شغال — افتح تطبيق Shizuku وفعّله", Toast.LENGTH_LONG).show()
                return
            }
            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                binding.shizukuStatus.text = "✅ Shizuku: متصل — تحكم كامل في الصوت!"
                Toast.makeText(this, "✅ Shizuku متصل بالفعل!", Toast.LENGTH_SHORT).show()
            } else {
                Shizuku.requestPermission(1001)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "❌ تأكد إن Shizuku شغال أولاً", Toast.LENGTH_LONG).show()
        }
    }

    private fun updateShizukuStatus() {
        try {
            when {
                !Shizuku.pingBinder() ->
                    binding.shizukuStatus.text = "⚠️ Shizuku: مش شغال — افتح تطبيق Shizuku"
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED ->
                    binding.shizukuStatus.text = "✅ Shizuku: متصل — تحكم كامل في الصوت!"
                else ->
                    binding.shizukuStatus.text = "⚠️ Shizuku: محتاج إذن — اضغط Connect"
            }
        } catch (e: Exception) {
            binding.shizukuStatus.text = "⚠️ Shizuku: غير متاح"
        }
    }

    private fun canDrawOverlays() = Settings.canDrawOverlays(this)

    private fun requestOverlayPermission() {
        Toast.makeText(this, "من فضلك اسمح للتطبيق بالظهور فوق التطبيقات الأخرى", Toast.LENGTH_LONG).show()
        startActivityForResult(
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")),
            REQUEST_OVERLAY_PERMISSION
        )
    }

    private fun startFloatingService() {
        val intent = Intent(this, FloatingPanelService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent)
        else startService(intent)
        binding.statusText.text = "✅ Volume Mixer شغال"
        Toast.makeText(this, "Volume Mixer شغال! هتلاقي الأيقونة العائمة", Toast.LENGTH_SHORT).show()
    }

    private fun stopFloatingService() {
        stopService(Intent(this, FloatingPanelService::class.java))
        binding.statusText.text = "⏹️ Volume Mixer متوقف"
    }

    private fun updatePermissionStatus() {
        binding.permissionStatus.text = if (canDrawOverlays())
            "✅ إذن النافذة العائمة: ممنوح"
        else
            "❌ إذن النافذة العائمة: غير ممنوح — اضغط Start"
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_OVERLAY_PERMISSION) {
            updatePermissionStatus()
            if (canDrawOverlays()) startFloatingService()
        }
    }

    override fun onResume() {
        super.onResume()
        updatePermissionStatus()
        updateShizukuStatus()
    }

    override fun onDestroy() {
        super.onDestroy()
        Shizuku.removeRequestPermissionResultListener(shizukuPermissionListener)
    }

    companion object {
        private const val REQUEST_OVERLAY_PERMISSION = 1001
    }
}
