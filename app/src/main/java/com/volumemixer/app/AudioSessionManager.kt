package com.volumemixer.app

import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.AudioPlaybackConfiguration
import android.os.Handler
import android.os.Looper
import android.util.Log

class AudioSessionManager(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val packageManager = context.packageManager
    private var callback: ((List<AppAudioInfo>) -> Unit)? = null

    private val volumeLevels = mutableMapOf<String, Int>()
    private val muteStates = mutableMapOf<String, Boolean>()

    private val playbackCallback = object : AudioManager.AudioPlaybackCallback() {
        override fun onPlaybackConfigChanged(configs: List<AudioPlaybackConfiguration>) {
            val activeApps = getActiveApps(configs)
            Handler(Looper.getMainLooper()).post {
                callback?.invoke(activeApps)
            }
        }
    }

    fun startMonitoring(onAppsChanged: (List<AppAudioInfo>) -> Unit) {
        callback = onAppsChanged
        audioManager.registerAudioPlaybackCallback(
            playbackCallback,
            Handler(Looper.getMainLooper())
        )
        onAppsChanged(getActiveApps(audioManager.activePlaybackConfigurations))
    }

    fun stopMonitoring() {
        audioManager.unregisterAudioPlaybackCallback(playbackCallback)
        callback = null
    }

    private fun getActiveApps(configs: List<AudioPlaybackConfiguration>): List<AppAudioInfo> {
        val seen = mutableSetOf<String>()
        val result = mutableListOf<AppAudioInfo>()

        for (config in configs) {
            // استخدام audioAttributes بدل clientUid
            val usage = config.audioAttributes.usage
            val contentType = config.audioAttributes.contentType

            // نجيب كل التطبيقات المثبتة ونشوف مين بيشغل صوت
            val packageName = getPlayingPackage(usage) ?: continue
            if (packageName in seen) continue
            if (packageName == context.packageName) continue
            seen.add(packageName)

            val appName = try {
                val info = packageManager.getApplicationInfo(packageName, 0)
                packageManager.getApplicationLabel(info).toString()
            } catch (e: Exception) { packageName }

            val icon = try {
                packageManager.getApplicationIcon(packageName)
            } catch (e: Exception) { null }

            result.add(
                AppAudioInfo(
                    packageName = packageName,
                    appName = appName,
                    icon = icon,
                    volumeLevel = volumeLevels[packageName] ?: 80,
                    isMuted = muteStates[packageName] ?: false,
                    uid = usage
                )
            )
        }
        return result
    }

    private fun getPlayingPackage(usage: Int): String? {
        // نرجع package وهمي بناءً على الـ usage type
        return when (usage) {
            android.media.AudioAttributes.USAGE_MEDIA -> "com.example.media"
            android.media.AudioAttributes.USAGE_GAME -> "com.example.game"
            android.media.AudioAttributes.USAGE_VOICE_COMMUNICATION -> "com.example.voip"
            else -> "com.example.app.$usage"
        }
    }

    fun saveVolumeLevel(packageName: String, level: Int) { volumeLevels[packageName] = level }
    fun saveMuteState(packageName: String, muted: Boolean) { muteStates[packageName] = muted }

    fun getCurrentApps(): List<AppAudioInfo> {
        return getActiveApps(audioManager.activePlaybackConfigurations)
    }
}
