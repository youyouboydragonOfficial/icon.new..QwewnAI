package com.example.iconchanger.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

/**
 * 画像処理ユーティリティ
 */
object ImageUtils {

    /**
     * 円形に切り抜いたビットマップを作成
     */
    fun createCircularBitmap(bitmap: Bitmap): Bitmap {
        val size = minOf(bitmap.width, bitmap.height)
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        
        val canvas = Canvas(output)
        val paint = Paint().apply {
            isAntiAlias = true
            color = 0xFF000000.toInt()
        }
        
        val rect = RectF(0f, 0f, size.toFloat(), size.toFloat())
        val radius = size / 2f
        
        canvas.drawRoundRect(rect, radius, radius, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        
        val srcRect = RectF(
            (bitmap.width - size) / 2f,
            (bitmap.height - size) / 2f,
            (bitmap.width + size) / 2f,
            (bitmap.height + size) / 2f
        )
        
        canvas.drawBitmap(bitmap, srcRect, rect, paint)
        return output
    }

    /**
     * アイコンを加工（スケール、回転、色調整）
     */
    fun processIcon(
        context: Context,
        bitmap: Bitmap,
        scale: Float = 1.0f,
        offsetX: Float = 0f,
        offsetY: Float = 0f,
        rotation: Float = 0f,
        brightness: Float = 1.0f,
        contrast: Float = 1.0f,
        saturation: Float = 1.0f
    ): Bitmap {
        val size = context.resources.getDimensionPixelSize(android.R.dimen.app_icon_size)
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        
        // 色調整
        if (brightness != 1.0f || contrast != 1.0f || saturation != 1.0f) {
            val colorMatrix = ColorMatrix()
            
            // 彩度
            if (saturation != 1.0f) {
                val saturationMatrix = ColorMatrix()
                saturationMatrix.setSaturation(saturation)
                colorMatrix.postConcat(saturationMatrix)
            }
            
            // 明るさとコントラスト
            if (brightness != 1.0f || contrast != 1.0f) {
                val brightnessMatrix = ColorMatrix(floatArrayOf(
                    contrast, 0f, 0f, 0f, brightness * 255 * (1 - contrast),
                    0f, contrast, 0f, 0f, brightness * 255 * (1 - contrast),
                    0f, 0f, contrast, 0f, brightness * 255 * (1 - contrast),
                    0f, 0f, 0f, 1f, 0f
                ))
                colorMatrix.postConcat(brightnessMatrix)
            }
            
            paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        }
        
        val matrix = Matrix()
        
        // スケール
        val scaledWidth = bitmap.width * scale
        val scaledHeight = bitmap.height * scale
        val scaleRatio = minOf(size / scaledWidth, size / scaledHeight).coerceAtMost(1.0f)
        matrix.preScale(scale * scaleRatio, scale * scaleRatio)
        
        // オフセット適用
        matrix.postTranslate(
            (size - bitmap.width * scale * scaleRatio) / 2 + offsetX * size,
            (size - bitmap.height * scale * scaleRatio) / 2 + offsetY * size
        )
        
        // 回転
        if (rotation != 0f) {
            matrix.postRotate(rotation, size / 2f, size / 2f)
        }
        
        canvas.drawBitmap(bitmap, matrix, paint)
        return output
    }

    /**
     * ファイルからビットマップを読み込み（メモリ最適化）
     */
    suspend fun loadBitmapFromFile(file: File, reqWidth: Int, reqHeight: Int): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(file.absolutePath, options)
            
            val (width, height) = Pair(options.outWidth, options.outHeight)
            var inSampleSize = 1
            
            if (height > reqHeight || width > reqWidth) {
                val halfHeight = height / 2
                val halfWidth = width / 2
                
                while (halfHeight / inSampleSize >= reqHeight && 
                       halfWidth / inSampleSize >= reqWidth) {
                    inSampleSize *= 2
                }
            }
            
            options.inJustDecodeBounds = false
            options.inSampleSize = inSampleSize
            options.inPreferredConfig = Bitmap.Config.ARGB_8888
            
            BitmapFactory.decodeFile(file.absolutePath, options)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * ビットマップをファイルに保存
     */
    suspend fun saveBitmapToFile(bitmap: Bitmap, file: File): Boolean = withContext(Dispatchers.IO) {
        try {
            FileOutputStream(file).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}

/**
 * ネットワークユーティリティ
 */
object NetworkUtils {

    /**
     * ネットワーク接続状態をチェック
     */
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        } else {
            @Suppress("DEPRECATION")
            connectivityManager.activeNetworkInfo?.isConnected == true
        }
    }
}

/**
 * GitHub API ユーティリティ
 */
object GitHubUtils {

    private const val OWNER = "your-username"
    private const val REPO = "icon-changer-app"
    private const val BASE_URL = "https://api.github.com/repos/$OWNER/$REPO"
    
    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "IconChangerApp")
                .build()
            chain.proceed(request)
        }
        .build()

    /**
     * 最新のリリース情報を取得
     */
    suspend fun getLatestRelease(): com.example.iconchanger.model.GitHubRelease? = withContext(Dispatchers.IO) {
        if (!NetworkUtils.isNetworkAvailable(androidx.core.content.ContextCompat.startForegroundService(
                androidx.appcompat.app.AppCompatActivity(), 
                android.content.Intent()
            ).let { return@withContext null })) {
            return@withContext null
        }
        
        try {
            val request = Request.Builder()
                .url("$BASE_URL/releases/latest")
                .build()
            
            val response = client.newCall(request).execute()
            
            if (!response.isSuccessful) {
                return@withContext null
            }
            
            val body = response.body?.string() ?: return@withContext null
            parseReleaseJson(body)
        } catch (e: Exception) {
            null
        }
    }.let { null }

    private fun parseReleaseJson(json: String): com.example.iconchanger.model.GitHubRelease? {
        // 簡易 JSON パース（実際には Gson や Moshi を使用）
        return try {
            val regex = "\"tag_name\"\\s*:\\s*\"([^\"]+)\"".toRegex()
            val tagName = regex.find(json)?.groupValues?.get(1) ?: ""
            
            val nameRegex = "\"name\"\\s*:\\s*\"([^\"]+)\"".toRegex()
            val name = nameRegex.find(json)?.groupValues?.get(1) ?: ""
            
            val bodyRegex = "\"body\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"".toRegex()
            val body = bodyRegex.find(json)?.groupValues?.get(1)?.replace("\\n", "\n") ?: ""
            
            val publishedAtRegex = "\"published_at\"\\s*:\\s*\"([^\"]+)\"".toRegex()
            val publishedAt = publishedAtRegex.find(json)?.groupValues?.get(1) ?: ""
            
            val htmlUrlRegex = "\"html_url\"\\s*:\\s*\"([^\"]+)\"".toRegex()
            val htmlUrl = htmlUrlRegex.find(json)?.groupValues?.get(1) ?: ""
            
            com.example.iconchanger.model.GitHubRelease(
                tagName = tagName,
                name = name,
                body = body.replace("\\\"", "\""),
                publishedAt = publishedAt,
                htmlUrl = htmlUrl,
                assets = emptyList()
            )
        } catch (e: Exception) {
            null
        }
    }
}
