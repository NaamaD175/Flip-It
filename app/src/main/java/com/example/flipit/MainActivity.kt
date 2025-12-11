package com.example.flipit

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import kotlin.random.Random
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics


class MainActivity : AppCompatActivity() {

    private lateinit var rootLayout: ConstraintLayout
    private lateinit var imgBackground: ImageView
    private lateinit var imgCoin: ImageView
    private lateinit var btnSettings: ImageButton
    private lateinit var txtResult: TextView

    private lateinit var gestureDetector: GestureDetector
    private var isFlipping = false

    private var currentCoin = "shekel"
    private var currentBackground = R.drawable.beach_background

    private val PREFS_NAME = "FlipItPrefs"
    private val KEY_PREMIUM_UNLOCKED = "isPremiumBackgroundUnlocked"
    private var isPremiumBackgroundUnlocked = false

    private var interstitialAd: InterstitialAd? = null

    private val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712" // <--- ID חדש!

    private lateinit var firebaseAnalytics: FirebaseAnalytics


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        MobileAds.initialize(this) {}

        firebaseAnalytics = FirebaseAnalytics.getInstance(this)

        // Load Advertisements immediate
        loadInterstitialAd()

        // Load purchase status before setting up UI
        loadPurchaseState()

        // Initialize UI elements
        rootLayout = findViewById(R.id.rootLayout)
        imgBackground = findViewById(R.id.imgBackground)
        imgCoin = findViewById(R.id.imgCoin)
        btnSettings = findViewById(R.id.btnSettings)
        txtResult = findViewById(R.id.txtResult)

        // Set default coin image and background upon launch
        setDefaultCoinImage()
        imgBackground.setImageResource(currentBackground)

        // Set camera distance for a deeper 3D effect during rotation
        val scale = resources.displayMetrics.density
        imgCoin.cameraDistance = 8000 * scale

        // Settings button click listener
        btnSettings.setOnClickListener {
            hideResult()
            showSettingsDialog()
        }

        // Initialize Gesture Detection for Tap and Swipe Up
        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            private val SWIPE_THRESHOLD = 100
            private val SWIPE_VELOCITY_THRESHOLD = 100

            // Detects a short tap
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                flipCoin()
                return true
            }

            // Detects a swipe motion
            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (e1 == null) return false

                val diffY = e2.y - e1.y
                val diffX = e2.x - e1.x

                // Detects a strong upward swipe
                if (Math.abs(diffX) < Math.abs(diffY) && Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD && diffY < 0) {
                    flipCoin()
                    return true
                }
                return false
            }

            override fun onDown(e: MotionEvent): Boolean {
                return true
            }
        })

        // Attaches the gesture detector to the coin image
        imgCoin.setOnTouchListener { v, event ->
            gestureDetector.onTouchEvent(event)
            true
        }
    }

    private fun loadPurchaseState() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        isPremiumBackgroundUnlocked = prefs.getBoolean(KEY_PREMIUM_UNLOCKED, false)
    }

    private fun savePurchaseState() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_PREMIUM_UNLOCKED, true).apply()
    }

    private fun hideResult() {
        txtResult.visibility = View.INVISIBLE
    }

    private fun setDefaultCoinImage() {
        val frontRes = when (currentCoin) {
            "shekel" -> R.drawable.coin_shekel_front
            "euro" -> R.drawable.coin_euro_front
            "dollar" -> R.drawable.coin_dollar_front
            else -> R.drawable.coin_shekel_front
        }
        imgCoin.setImageResource(frontRes)
    }

    private fun flipCoin() {
        if (isFlipping) return
        isFlipping = true

        hideResult()

        val textColor = if (currentBackground == R.drawable.beach_background) {
            resources.getColor(android.R.color.black, theme)
        } else {
            resources.getColor(android.R.color.white, theme)
        }
        txtResult.setTextColor(textColor)

        val isHeads = Random.nextBoolean()

        val (headsRes, tailsRes) = when (currentCoin) {
            "shekel" -> R.drawable.coin_shekel_front to R.drawable.coin_shekel_back
            "euro" -> R.drawable.coin_euro_front to R.drawable.coin_euro_back
            "dollar" -> R.drawable.coin_dollar_front to R.drawable.coin_dollar_back
            else -> R.drawable.coin_shekel_front to R.drawable.coin_shekel_back
        }

        val resultDrawable = if (isHeads) headsRes else tailsRes
        val resultText = if (isHeads) "Heads" else "Tails"

        val totalDuration = 1500L
        val rotationRounds = 5

        val targetRotation = (360 * rotationRounds).toFloat() + if (!isHeads) 180f else 0f

        val rotate = ObjectAnimator.ofFloat(imgCoin, "rotationY", 0f, targetRotation).apply {
            duration = totalDuration
            interpolator = AccelerateDecelerateInterpolator()
        }

        val upAndDown = ObjectAnimator.ofFloat(imgCoin, "translationY", 0f, -600f, 0f).apply {
            duration = totalDuration
            interpolator = OvershootInterpolator(1.0f)
        }

        val flipSet = AnimatorSet().apply {
            playTogether(rotate, upAndDown)
        }

        imgCoin.setImageResource(headsRes)

        flipSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator) {
                imgCoin.rotationY = 0f
            }

            override fun onAnimationEnd(animation: Animator) {
                imgCoin.rotationY = 0f
                imgCoin.translationY = 0f

                imgCoin.setImageResource(resultDrawable)
                isFlipping = false

                txtResult.text = resultText
                txtResult.alpha = 0f
                txtResult.visibility = View.VISIBLE
                txtResult.animate()
                    .alpha(1f)
                    .setDuration(250)
                    .start()
            }
        })

        flipSet.start()
    }


    private fun showSettingsDialog() {
        val options = arrayOf("Change background", "Change coins", "Help")

        AlertDialog.Builder(this)
            .setTitle("Settings")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showBackgroundDialogGate()
                    1 -> showCoinDialog()
                    2 -> showHelpDialog()
                }
            }
            .show()
    }

    private fun showHelpDialog() {
        AlertDialog.Builder(this)
            .setTitle("How to Flip")
            .setMessage("To flip the coin, you have two options:\n\n1. **Tap:** Simply tap on the coin image in the center of the screen.\n2. **Swipe Up:** Swipe your finger quickly upwards from the coin image.")
            .setPositiveButton("Got It!") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showBackgroundDialogGate() {
        if (isPremiumBackgroundUnlocked) {
            showSelectBackgroundOptions()
        } else {
            showPurchaseBackgroundDialog()
        }
    }

    private fun showPurchaseBackgroundDialog() {
        AlertDialog.Builder(this)
            .setTitle("Unlock Premium Backgrounds ($2.00)")
            .setMessage("The advanced background options cost $2.00. Do you want to unlock them now?")
            .setPositiveButton("Yes (Pay $2)") { dialog, _ ->
                isPremiumBackgroundUnlocked = true
                savePurchaseState()
                dialog.dismiss()
                showSelectBackgroundOptions()
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showSelectBackgroundOptions() {
        val names = arrayOf("Beach", "Road", "Garden")

        AlertDialog.Builder(this)
            .setTitle("Change background")
            .setItems(names) { _, which ->
                val backgroundRes = when (which) {
                    0 -> R.drawable.beach_background
                    1 -> R.drawable.road_background
                    2 -> R.drawable.garden_background
                    else -> R.drawable.beach_background
                }

                imgBackground.setImageResource(backgroundRes)
                currentBackground = backgroundRes
            }
            .show()
    }

    private fun showCoinDialog() {
        val coinNames = arrayOf("Shekel", "Euro", "Dollar")
        AlertDialog.Builder(this)
            .setTitle("Change coin")
            .setItems(coinNames) { _, which ->
                currentCoin = when (which) {
                    0 -> "shekel"
                    1 -> "euro"
                    2 -> "dollar"
                    else -> "shekel"
                }

                setDefaultCoinImage()
            }
            .show()
    }


    private fun loadInterstitialAd() {
        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(this,
            INTERSTITIAL_AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    showInterstitialAd() // Show the ad when she is ready
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    interstitialAd = null
                    // If the ad fails to load initially - do nothing and allow the user to use the app
                }
            })
    }

    private fun showInterstitialAd() {
        if (interstitialAd != null) {
            interstitialAd?.show(this)

        } else {
            // If the ad doesn't load in time, it is skipped
        }
    }
}