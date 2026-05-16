package com.kreedaankana.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.kreedaankana.R
import com.kreedaankana.databinding.ActivitySplashBinding

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private var isNavigationTriggered = false
    private val handler = Handler(Looper.getMainLooper())
    
    // Fallback runnable to ensure the user gets to the next screen even if the video fails to load/play
    private val fallbackRunnable = Runnable {
        navigateToNextScreen()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. Build URI for sports_logo_reveal.mp4 located in raw folder
        val videoUri = Uri.parse("android.resource://$packageName/${R.raw.sports_logo_reveal}")

        // 2. Set URI and start video playback
        binding.videoViewSplash.setVideoURI(videoUri)
        
        binding.videoViewSplash.setOnPreparedListener { mediaPlayer ->
            // Prevent audio focus issues and start playback
            mediaPlayer.setVolume(0f, 0f) // Mute the intro video sound
            mediaPlayer.start()
            
            // Adjust VideoView dimensions dynamically to match original video aspect ratio
            val videoWidth = mediaPlayer.videoWidth.toFloat()
            val videoHeight = mediaPlayer.videoHeight.toFloat()
            
            if (videoWidth > 0 && videoHeight > 0) {
                val videoAspectRatio = videoWidth / videoHeight
                val screenWidth = resources.displayMetrics.widthPixels.toFloat()
                val screenHeight = resources.displayMetrics.heightPixels.toFloat()
                val screenAspectRatio = screenWidth / screenHeight

                val layoutParams = binding.videoViewSplash.layoutParams
                if (videoAspectRatio > screenAspectRatio) {
                    // Video is wider than screen aspect ratio - cover screen height, cropping left/right sides
                    layoutParams.width = (screenHeight * videoAspectRatio).toInt()
                    layoutParams.height = screenHeight.toInt()
                } else {
                    // Video is taller than screen aspect ratio - cover screen width, cropping top/bottom sides
                    layoutParams.width = screenWidth.toInt()
                    layoutParams.height = (screenWidth / videoAspectRatio).toInt()
                }
                binding.videoViewSplash.layoutParams = layoutParams
            }
            
            // Set up a safe fallback timer based on video length plus a 1-second grace period
            val videoDuration = mediaPlayer.duration.toLong()
            val safetyTimeout = if (videoDuration > 0) videoDuration + 1000 else 6000L
            handler.postDelayed(fallbackRunnable, safetyTimeout)
        }

        // 3. Navigate instantly once the logo reveal animation reaches completion
        binding.videoViewSplash.setOnCompletionListener {
            navigateToNextScreen()
        }

        // 4. Handle video error gracefully by directly continuing to the next screen
        binding.videoViewSplash.setOnErrorListener { _, _, _ ->
            navigateToNextScreen()
            true
        }

        // 5. Allow users to skip the intro instantly
        binding.btnSkipSplash.setOnClickListener {
            navigateToNextScreen()
        }
    }

    private fun navigateToNextScreen() {
        if (isNavigationTriggered) return
        isNavigationTriggered = true
        
        // Remove any pending fallback timeouts
        handler.removeCallbacks(fallbackRunnable)

        // Check user session
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        val nextIntent = if (currentUser != null) {
            Intent(this, MainActivity::class.java)
        } else {
            Intent(this, com.kreedaankana.ui.auth.LoginActivity::class.java)
        }
        
        startActivity(nextIntent)
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        finish()
    }

    override fun onPause() {
        super.onPause()
        if (binding.videoViewSplash.isPlaying) {
            binding.videoViewSplash.pause()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(fallbackRunnable)
    }
}
