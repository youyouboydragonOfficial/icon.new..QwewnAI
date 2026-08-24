package com.example.iconchanger.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransform
import com.example.iconchanger.R
import com.example.iconchanger.model.AppInfo
import java.util.Locale

/**
 * アプリ一覧用のアダプター
 */
class AppListAdapter(
    private val onItemClick: (AppInfo) -> Unit,
    private val onIconLongClick: (AppInfo) -> Unit = {}
) : ListAdapter<AppInfo, AppListAdapter.AppViewHolder>(AppDiffCallback()) {

    private var fullAppList: List<AppInfo> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app, parent, false)
        return AppViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    fun submitFullList(apps: List<AppInfo>) {
        fullAppList = apps
        submitList(apps)
    }

    inner class AppViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val iconImageView: ImageView = itemView.findViewById(R.id.appIcon)
        private val nameTextView: TextView = itemView.findViewById(R.id.appName)
        private val packageNameTextView: TextView = itemView.findViewById(R.id.packageName)

        fun bind(appInfo: AppInfo) {
            nameTextView.text = appInfo.appName
            packageNameTextView.text = appInfo.packageName

            // アイコンを読み込み
            if (appInfo.icon != null) {
                iconImageView.setImageBitmap(appInfo.icon)
            } else {
                // デフォルトアイコン
                iconImageView.setImageResource(R.drawable.ic_launcher_foreground)
            }

            itemView.setOnClickListener {
                onItemClick(appInfo)
            }

            itemView.setOnLongClickListener {
                onIconLongClick(appInfo)
                true
            }
        }
    }

    private class AppDiffCallback : DiffUtil.ItemCallback<AppInfo>() {
        override fun areItemsTheSame(oldItem: AppInfo, newItem: AppInfo): Boolean {
            return oldItem.packageName == newItem.packageName
        }

        override fun areContentsTheSame(oldItem: AppInfo, newItem: AppInfo): Boolean {
            return oldItem == newItem
        }
    }
}

/**
 * ショートカット一覧用のアダプター
 */
class ShortcutListAdapter(
    private val onItemClick: (com.example.iconchanger.model.ShortcutInfo) -> Unit,
    private val onDeleteClick: (com.example.iconchanger.model.ShortcutInfo) -> Unit
) : ListAdapter<com.example.iconchanger.model.ShortcutInfo, ShortcutListAdapter.ShortcutViewHolder>(ShortcutDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShortcutViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_shortcut, parent, false)
        return ShortcutViewHolder(view)
    }

    override fun onBindViewHolder(holder: ShortcutViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ShortcutViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val iconImageView: ImageView = itemView.findViewById(R.id.shortcutIcon)
        private val nameTextView: TextView = itemView.findViewById(R.id.shortcutName)
        private val deleteButton: ImageView = itemView.findViewById(R.id.deleteShortcut)

        fun bind(shortcut: com.example.iconchanger.model.ShortcutInfo) {
            nameTextView.text = shortcut.customName ?: shortcut.label

            // カスタムアイコンがあれば表示
            if (!shortcut.iconPath.isNullOrBlank()) {
                iconImageView.load(shortcut.iconPath) {
                    crossfade(true)
                    transformations(CircleCropTransform())
                }
            } else {
                iconImageView.setImageResource(R.drawable.ic_launcher_foreground)
            }

            itemView.setOnClickListener {
                onItemClick(shortcut)
            }

            deleteButton.setOnClickListener {
                onDeleteClick(shortcut)
            }
        }
    }

    private class ShortcutDiffCallback : DiffUtil.ItemCallback<com.example.iconchanger.model.ShortcutInfo>() {
        override fun areItemsTheSame(oldItem: com.example.iconchanger.model.ShortcutInfo, newItem: com.example.iconchanger.model.ShortcutInfo): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: com.example.iconchanger.model.ShortcutInfo, newItem: com.example.iconchanger.model.ShortcutInfo): Boolean {
            return oldItem == newItem
        }
    }
}
