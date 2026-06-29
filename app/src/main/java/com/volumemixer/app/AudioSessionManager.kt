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
        val current = audioManager.activePlaybackConfigurations
        val activeApps = getActiveApps(current)
        onAppsChanged(activeApps)
    }

    fun stopMonitoring() {
        audioManager.unregisterAudioPlaybackCallback(playbackCallback)
        callback = null
    }

    private fun getActiveApps(configs: List<AudioPlaybackConfiguration>): List<AppAudioInfo> {
        val seen = mutableSetOf<String>()
        val result = mutableListOf<AppAudioInfo>()

        for (config in configs) {
            val uid = config.clientUid
            val packageName = getPackageFromUid(uid) ?: continue
            if (packageName in seen) continue
            if (packageName == context.packageName) continue
            seen.add(packageName)

            val appName = try {
                val info = packageManager.getApplicationInfo(packageName, 0)
                packageManager.getApplicationLabel(info).toString()
            } catch (e: PackageManager.NameNotFoundException) { packageName }

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
                    uid = uid
                )
            )
        }
        return result
    }

    private fun getPackageFromUid(uid: Int): String? {
        return try {
            packageManager.getPackagesForUid(uid)?.firstOrNull()
        } catch (e: Exception) {
            Log.w("AudioSessionManager", "uid: $uid")
            null
        }
    }

    fun saveVolumeLevel(packageName: String, level: Int) { volumeLevels[packageName] = level }
    fun saveMuteState(packageName: String, muted: Boolean) { muteStates[packageName] = muted }

    fun getCurrentApps(): List<AppAudioInfo> {
        return getActiveApps(audioManager.activePlaybackConfigurations)
    }
}
