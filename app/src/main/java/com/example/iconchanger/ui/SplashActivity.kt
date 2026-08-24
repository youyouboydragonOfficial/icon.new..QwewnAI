package com.example.iconchanger.ui

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.View
import android.view.animation.OvershootInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.iconchanger.R
import com.example.iconchanger.databinding.ActivitySplashBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * スプラッシュスクリーンアクティビティ
 * かっこいいアニメーションでアプリを起動
 */
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // アニメーションを開始
        startSplashAnimation()
    }

    private fun startSplashAnimation() {
        val logoAnimator = ObjectAnimator.ofFloat(binding.splashLogo, View.ALPHA, 0f, 1f)
        val textAnimator = ObjectAnimator.ofFloat(binding.splashText, View.ALPHA, 0f, 1f)
        
        // スケールアニメーション
        val scaleXAnimator = ObjectAnimator.ofFloat(binding.splashLogo, View.SCALE_X, 0.5f, 1f)
        val scaleYAnimator = ObjectAnimator.ofFloat(binding.splashLogo, View.SCALE_Y, 0.5f, 1f)
        
        // 回転アニメーション（オプション）
        val rotateAnimator = ObjectAnimator.ofFloat(binding.splashLogo, View.ROTATION, -10f, 0f)

        // アニメーションセット
        val animatorSet = AnimatorSet().apply {
            playTogether(
                logoAnimator,
                textAnimator,
                scaleXAnimator,
                scaleYAnimator,
                rotateAnimator
            )
            duration = 800
            interpolator = OvershootInterpolator(2f)
        }

        // プログレスバー表示
        binding.loadingProgress.visibility = View.VISIBLE
        
        animatorSet.start()

        lifecycleScope.launch {
            delay(2000) // 2 秒待機
            
            // メインアクティビティに遷移
            startActivity(android.content.Intent(this@SplashActivity, MainActivity::class.java))
            finish()
            
            // フェードアウトアニメーション
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }
}
