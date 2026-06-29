package com.volumemixer.app

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.volumemixer.app.databinding.ItemAppVolumeBinding

class AppVolumeAdapter(
    private val onVolumeChanged: (String, Int) -> Unit,
    private val onMuteToggled: (String, Boolean) -> Unit
) : RecyclerView.Adapter<AppVolumeAdapter.ViewHolder>() {

    private val items = mutableListOf<AppAudioInfo>()

    fun updateItems(newItems: List<AppAudioInfo>) {
        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = items.size
            override fun getNewListSize() = newItems.size
            override fun areItemsTheSame(oldPos: Int, newPos: Int) =
                items[oldPos].packageName == newItems[newPos].packageName
            override fun areContentsTheSame(oldPos: Int, newPos: Int) =
                items[oldPos] == newItems[newPos]
        })
        items.clear()
        items.addAll(newItems)
        diff.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAppVolumeBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class ViewHolder(private val binding: ItemAppVolumeBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(app: AppAudioInfo) {
            binding.appName.text = app.appName
            binding.appIcon.setImageDrawable(app.icon)
            binding.volumeSlider.progress = if (app.isMuted) 0 else app.volumeLevel
            binding.volumePercent.text = "${if (app.isMuted) 0 else app.volumeLevel}%"

            // أيقونة الكتم
            updateMuteButton(app.isMuted)

            // عند تغيير السلايدر
            binding.volumeSlider.setOnSeekBarChangeListener(object :
                android.widget.SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        binding.volumePercent.text = "$progress%"
                        onVolumeChanged(app.packageName, progress)
                        app.volumeLevel = progress
                        if (progress > 0 && app.isMuted) {
                            app.isMuted = false
                            updateMuteButton(false)
                        }
                    }
                }
                override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
            })

            // زر الكتم
            binding.btnMute.setOnClickListener {
                app.isMuted = !app.isMuted
                updateMuteButton(app.isMuted)
                binding.volumeSlider.progress = if (app.isMuted) 0 else app.volumeLevel
                binding.volumePercent.text = "${if (app.isMuted) 0 else app.volumeLevel}%"
                onMuteToggled(app.packageName, app.isMuted)
            }
        }

        private fun updateMuteButton(isMuted: Boolean) {
            binding.btnMute.text = if (isMuted) "🔇" else "🔊"
            binding.btnMute.alpha = if (isMuted) 0.5f else 1.0f
        }
    }
}
