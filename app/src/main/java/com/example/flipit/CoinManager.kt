package com.example.flipit

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.media.MediaPlayer
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import kotlin.random.Random

// Handles the visual 3D animation and audio effects for the coin flip
class CoinManager(
    private val context: Context,
    private val imgCoin: ImageView,
    private val txtResult: TextView
) {
    var currentCoin = "shekel"
    private var mediaPlayer: MediaPlayer? = null

// Updates the starting coin image based on the selected currency
    fun updateDefaultImage() {
        val frontRes = when (currentCoin) {
            "shekel" -> R.drawable.coin_shekel_front
            "euro" -> R.drawable.coin_euro_front
            "dollar" -> R.drawable.coin_dollar_front
            else -> R.drawable.coin_shekel_front
        }
        imgCoin.setImageResource(frontRes)
    }
    // Initializes and starts the flip sound effect from the raw resources
    private fun startFlipSound() {
        stopFlipSound()
        mediaPlayer = MediaPlayer.create(context, R.raw.coin_flip)
        mediaPlayer?.start()
    }

    // Stops the sound and releases the MediaPlayer to free up system memory
    private fun stopFlipSound() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    // Main function to execute the coin toss animation
    fun flip(onAnimationEnd: () -> Unit) {
        val isHeads = Random.nextBoolean()
        val (headsRes, tailsRes) = when (currentCoin) {
            "shekel" -> R.drawable.coin_shekel_front to R.drawable.coin_shekel_back
            "euro" -> R.drawable.coin_euro_front to R.drawable.coin_euro_back
            else -> R.drawable.coin_dollar_front to R.drawable.coin_dollar_back
        }

        val resultDrawable = if (isHeads) headsRes else tailsRes
        val resultText = if (isHeads) "Heads" else "Tails"

        // Play the spinning sound
        startFlipSound()

        // Going up - simulates the coin being tossed upwards
        val upSet = AnimatorSet().apply {
            val goUp = ObjectAnimator.ofFloat(imgCoin, View.TRANSLATION_Y, 0f, -800f)
            val rotateUp = ObjectAnimator.ofFloat(imgCoin, View.ROTATION_X, 0f, 1800f)
            // Scaling down makes the coin look further away
            val scaleUp = ObjectAnimator.ofFloat(imgCoin, View.SCALE_X, 1f, 0.7f)
            val scaleUpY = ObjectAnimator.ofFloat(imgCoin, View.SCALE_Y, 1f, 0.7f)

            playTogether(goUp, rotateUp, scaleUp, scaleUpY)
            duration = 750
            interpolator = DecelerateInterpolator()
        }

        // Falling down - simulates the coin falling back to the hand
        val downSet = AnimatorSet().apply {
            val goDown = ObjectAnimator.ofFloat(imgCoin, View.TRANSLATION_Y, -800f, 0f)
            // Calculate final rotation based on heads/tails result
            val rotateDown = ObjectAnimator.ofFloat(imgCoin, View.ROTATION_X, 1800f, 3600f + (if (isHeads) 0f else 180f))
            // Scaling back to normal size as it gets closer
            val scaleDown = ObjectAnimator.ofFloat(imgCoin, View.SCALE_X, 0.7f, 1f)
            val scaleDownY = ObjectAnimator.ofFloat(imgCoin, View.SCALE_Y, 0.7f, 1f)

            playTogether(goDown, rotateDown, scaleDown, scaleDownY)
            duration = 750
            interpolator = AccelerateInterpolator()
        }

        // Combine animations to play sequentially
        val fullAnimation = AnimatorSet().apply {
            playSequentially(upSet, downSet)
        }

        // Set initial face
        imgCoin.setImageResource(headsRes)

        fullAnimation.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                // Stop the sound on impact
                stopFlipSound()
                // Finalize orientation and image
                imgCoin.rotationX = if (isHeads) 0f else 180f
                imgCoin.setImageResource(resultDrawable)

                // Show the result text with a fade-in effect
                txtResult.text = resultText
                txtResult.alpha = 0f
                txtResult.visibility = View.VISIBLE
                txtResult.animate().alpha(1f).setDuration(250).start()

                // Notify MainActivity to handle ads or state
                onAnimationEnd()
            }
        })

        fullAnimation.start()
    }
}