package com.volumemixer.app

import android.graphics.drawable.Drawable

data class AppAudioInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable?,
    var volumeLevel: Int = 80,  // 0-100
    var isMuted: Boolean = false,
    val uid: Int = 0
)
