package com.example.iconchanger.model

import android.graphics.Bitmap
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * アプリ情報のデータクラス
 */
@Parcelize
data class AppInfo(
    val packageName: String,
    val appName: String,
    val icon: Bitmap? = null,
    val isSystemApp: Boolean = false,
    val versionName: String = "",
    val versionCode: Long = 0L
) : Parcelable

/**
 * ショートカット情報のデータクラス
 */
@Parcelize
data class ShortcutInfo(
    val id: String,
    val label: String,
    val packageName: String,
    val activityName: String,
    val iconPath: String? = null,
    val customName: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) : Parcelable

/**
 * アイコン設定のデータクラス
 */
@Parcelize
data class IconConfig(
    val scale: Float = 1.0f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val rotation: Float = 0f,
    val brightness: Float = 1.0f,
    val contrast: Float = 1.0f,
    val saturation: Float = 1.0f
) : Parcelable

/**
 * GitHub リリース情報のデータクラス
 */
data class GitHubRelease(
    val tagName: String,
    val name: String,
    val body: String,
    val publishedAt: String,
    val htmlUrl: String,
    val assets: List<GitHubAsset>
)

data class GitHubAsset(
    val name: String,
    val size: Long,
    val downloadUrl: String,
    val contentType: String
)

/**
 * テーマ設定のデータクラス
 */
@Parcelize
data class ThemeConfig(
    val themeId: String = "default",
    val primaryColor: Int = -1,
    val accentColor: Int = -1,
    val backgroundColor: Int = -1,
    val isDarkMode: Boolean = false
) : Parcelable

/**
 * アニメーション設定のデータクラス
 */
@Parcelize
data class AnimationConfig(
    val splashDuration: Long = 2000L,
    val enableSplash: Boolean = true,
    val splashAnimationType: SplashAnimationType = SplashAnimationType.SCALE_FADE,
    val transitionDuration: Long = 300L
) : Parcelable

enum class SplashAnimationType {
    SCALE_FADE,
    ROTATE_FADE,
    SLIDE_FADE,
    BOUNCE_FADE,
    ZOOM_BLUR
}
