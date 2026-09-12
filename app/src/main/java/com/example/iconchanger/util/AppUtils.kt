package com.example.iconchanger.util

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import com.example.iconchanger.model.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * インストール済みアプリを取得するユーティリティクラス
 */
object AppUtils {

    /**
     * すべてのインストール済みアプリを取得
     */
    suspend fun getAllInstalledApps(context: Context): List<AppInfo> = withContext(Dispatchers.IO) {
        val packageManager = context.packageManager
        val apps = mutableListOf<AppInfo>()
        
        try {
            val packages = packageManager.getInstalledPackages(PackageManager.GET_META_DATA)
            
            for (pkg in packages) {
                try {
                    val appInfo = pkg.applicationInfo
                    val appName = packageManager.getApplicationLabel(appInfo).toString()
                    
                    // システムアプリか判定
                    val isSystemApp = (appInfo.flags and PackageManager.FLAG_SYSTEM) != 0 ||
                                     (appInfo.flags and PackageManager.FLAG_UPDATED_SYSTEM_APP) != 0
                    
                    // アイコンを取得
                    var iconBitmap: Bitmap? = null
                    try {
                        val icon = packageManager.getApplicationIcon(appInfo)
                        iconBitmap = drawableToBitmap(icon)
                    } catch (e: Exception) {
                        // アイコン取得失敗時は null
                    }
                    
                    apps.add(
                        AppInfo(
                            packageName = pkg.packageName,
                            appName = appName,
                            icon = iconBitmap,
                            isSystemApp = isSystemApp,
                            versionName = pkg.versionName ?: "",
                            versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                                pkg.longVersionCode
                            } else {
                                @Suppress("DEPRECATION")
                                pkg.versionCode.toLong()
                            }
                        )
                    )
                } catch (e: Exception) {
                    // 個別のパッケージ処理エラーは無視して続行
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        // アプリ名でソート
        apps.sortedBy { it.appName.lowercase() }
    }

    /**
     * アプリを検索
     */
    suspend fun searchApps(context: Context, query: String): List<AppInfo> = withContext(Dispatchers.IO) {
        val allApps = getAllInstalledApps(context)
        if (query.isBlank()) {
            return@withContext allApps
        }
        
        val lowerQuery = query.lowercase()
        allApps.filter { 
            it.appName.lowercase().contains(lowerQuery) || 
            it.packageName.lowercase().contains(lowerQuery)
        }
    }

    /**
     * Drawable を Bitmap に変換
     */
    private fun drawableToBitmap(drawable: Drawable): Bitmap? {
        return if (drawable is BitmapDrawable) {
            drawable.bitmap
        } else {
            try {
                val bitmap = Bitmap.createBitmap(
                    drawable.intrinsicWidth.coerceAtLeast(1),
                    drawable.intrinsicHeight.coerceAtLeast(1),
                    Bitmap.Config.ARGB_8888
                )
                val canvas = android.graphics.Canvas(bitmap)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)
                bitmap
            } catch (e: Exception) {
                null
            }
        }
    }

    /**
     * パッケージ名からアプリ情報を取得
     */
    suspend fun getAppInfo(context: Context, packageName: String): AppInfo? = withContext(Dispatchers.IO) {
        try {
            val packageManager = context.packageManager
            val pkg = packageManager.getPackageInfo(packageName, 0)
            val appInfo = pkg.applicationInfo
            
            val appName = packageManager.getApplicationLabel(appInfo).toString()
            val isSystemApp = (appInfo.flags and PackageManager.FLAG_SYSTEM) != 0
            
            var iconBitmap: Bitmap? = null
            try {
                val icon = packageManager.getApplicationIcon(appInfo)
                iconBitmap = drawableToBitmap(icon)
            } catch (e: Exception) {
                // アイコン取得失敗
            }
            
            AppInfo(
                packageName = packageName,
                appName = appName,
                icon = iconBitmap,
                isSystemApp = isSystemApp,
                versionName = pkg.versionName ?: "",
                versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    pkg.longVersionCode
                } else {
                    @Suppress("DEPRECATION")
                    pkg.versionCode.toLong()
                }
            )
        } catch (e: Exception) {
            null
        }
    }
}
