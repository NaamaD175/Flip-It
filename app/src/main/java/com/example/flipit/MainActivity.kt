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
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics


class MainActivity : AppCompatActivity() {

    // UI Components
    private lateinit var rootLayout: ConstraintLayout
    private lateinit var imgBackground: ImageView
    private lateinit var imgCoin: ImageView
    private lateinit var btnSettings: ImageButton
    private lateinit var txtResult: TextView

    // Game Logic
    private lateinit var gestureDetector: GestureDetector // Detects the touch of a finger
    private var isFlipping = false // true – the coin spins and further clicks are blocked, false – you can roll
    private var flipCount = 0 // How many tosses there have been so far - determines when to run ads
    private var currentCoin = "shekel" // Holds the name of the currently selected currency (default - Shekel)
    private var currentBackground = R.drawable.beach_background // Holds the name of the currently selected background

    // SharedPreferences
    private val PREFS_NAME = "FlipItPrefs" // The name of the internal "file" on the phone where we will save the data
    private val KEY_PREMIUM_UNLOCKED = "isPremiumBackgroundUnlocked" // The "key" where we store whether the user purchased the backgrounds
    private var isPremiumBackgroundUnlocked = false // Holds the answer whether the backgrounds are open
    private val KEY_ADS_REMOVED = "isAdsRemoved" // The key where we store whether the user purchased the remove Ads
    private var isAdsRemoved = false // Holds the answer to whether to display advertisements

    // Ads & Analytics
    private var interstitialAd: InterstitialAd? = null // Holds the ad itself that we downloaded from the Internet
    private val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712" // ID of the ad
    private lateinit var firebaseAnalytics: FirebaseAnalytics // Google's tool that collects statistics

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        MobileAds.initialize(this) {}

        firebaseAnalytics = FirebaseAnalytics.getInstance(this)

        // Load - if pay for remove ads
        loadPurchaseState()

        // Prepare the ad in the background (Don't show it yet)
        if (!isAdsRemoved) {
            loadInterstitialAd()
        }

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

    // Load the user's purchase status from the local device storage
    private fun loadPurchaseState() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        isPremiumBackgroundUnlocked = prefs.getBoolean(KEY_PREMIUM_UNLOCKED, false)
        isAdsRemoved = prefs.getBoolean(KEY_ADS_REMOVED, false)
    }

    // Save the premium backgrounds purchase status to local storage
    private fun saveBackgroundPurchaseState() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_PREMIUM_UNLOCKED, true).apply()
    }

    //Hide the result text (heads or tails) from the screen
    private fun hideResult() {
        txtResult.visibility = View.INVISIBLE
    }

    // Sets the correct image for the coin based on the selected currency
    private fun setDefaultCoinImage() {
        val frontRes = when (currentCoin) {
            "shekel" -> R.drawable.coin_shekel_front
            "euro" -> R.drawable.coin_euro_front
            "dollar" -> R.drawable.coin_dollar_front
            else -> R.drawable.coin_shekel_front
        }
        imgCoin.setImageResource(frontRes)
    }

    // Flip coin - handles the animation, randomized and updates the UI
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

                // Check if we need to show an ad
                if (!isAdsRemoved) {
                    flipCount++

                    // Logic: Show on 1st flip, OR every 3 flips thereafter
                    if (flipCount == 1 || (flipCount > 1 && (flipCount - 1) % 3 == 0)) {
                        showInterstitialAd()
                    }
                }
            }
        })

        flipSet.start()
    }

    // Shows the main settings menu with all available options - DYNAMICALLY
    private fun showSettingsDialog() {
        val optionsList = mutableListOf<String>()
        val actionsList = mutableListOf<Int>() // 0=Coin, 1=Bg, 2=Ads, 3=Help

        // Change Coins (Always visible)
        optionsList.add("Change coins")
        actionsList.add(0)

        // Change Background (Text changes if purchased)
        if (isPremiumBackgroundUnlocked) {
            optionsList.add("Change background") // No price shown
        } else {
            optionsList.add("Change background ($2)")
        }
        actionsList.add(1)

        // Remove Ads (Visible ONLY if NOT purchased)
        if (!isAdsRemoved) {
            optionsList.add("Remove Ads ($5)")
            actionsList.add(2)
        }

        // Help (Always visible)
        optionsList.add("Help")
        actionsList.add(3)

        // Convert list to array for the Dialog
        val finalOptions = optionsList.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Settings")
            .setItems(finalOptions) { _, which ->
                // 'which' is the index in the displayed list
                // We map it back to the original action ID using actionsList
                val actionId = actionsList[which]

                when (actionId) {
                    0 -> showCoinDialog()
                    1 -> showBackgroundDialogGate()
                    2 -> showRemoveAdsDialog()
                    3 -> showHelpDialog()
                }
            }
            .show()
    }

    // Handles the remove Ads purchase - save the state and shows a success message
    private fun showRemoveAdsDialog() {
        // Double check, although the button shouldn't be there if removed
        if (isAdsRemoved) return

        AlertDialog.Builder(this)
            .setTitle("Remove Ads ($5.00)")
            .setMessage("Hate ads? You can remove them forever for just $5.00. Want to proceed?")
            .setPositiveButton("Yes (Pay $5)") { dialog, _ ->
                isAdsRemoved = true
                val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                prefs.edit().putBoolean(KEY_ADS_REMOVED, true).apply()

                dialog.dismiss()

                AlertDialog.Builder(this)
                    .setTitle("Success!")
                    .setMessage("Payment successful. No more ads will be shown")
                    .setPositiveButton("Great") { d, _ -> d.dismiss() }
                    .show()
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    // Shows a help dialog explaining how to use the app
    private fun showHelpDialog() {
        AlertDialog.Builder(this)
            .setTitle("How to Flip")
            .setMessage("To flip the coin, you have two options:\n\n1. Tap: Simply tap on the coin image in the center of the screen.\n2. Swipe Up: Swipe your finger quickly upwards from the coin image")
            .setPositiveButton("Got It!") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    // Checks if premium backgrounds are unlocked, yes - shows selection list, no - shows purchase dialog
    private fun showBackgroundDialogGate() {
        if (isPremiumBackgroundUnlocked) {
            showSelectBackgroundOptions()
        } else {
            showPurchaseBackgroundDialog()
        }
    }

    // Handles the premium backgrounds purchase simulation - shows success message
    private fun showPurchaseBackgroundDialog() {
        AlertDialog.Builder(this)
            .setTitle("Unlock Premium Backgrounds ($2.00)")
            .setMessage("The advanced background options cost $2.00. Do you want to unlock them now?")
            .setPositiveButton("Yes (Pay $2)") { dialog, _ ->
                // Perform purchase logic
                isPremiumBackgroundUnlocked = true
                saveBackgroundPurchaseState()

                dialog.dismiss()

                // Show Success Dialog (Matching the Ads logic)
                AlertDialog.Builder(this)
                    .setTitle("Success!")
                    .setMessage("Payment successful. Premium backgrounds unlocked.")
                    .setPositiveButton("Great") { d, _ ->
                        d.dismiss()
                        // 3. Only now show the options
                        showSelectBackgroundOptions()
                    }
                    .show()
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    //Displays the list of available backgrounds for the user to choose
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

    // Displays the list of available coin types for the user to choose
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

    // Loads a full screen interstitial ad from AdMob
    private fun loadInterstitialAd() {
        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(this,
            INTERSTITIAL_AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    // NOTICE: We removed "showInterstitialAd()" from here.
                    // We only want to load it and keep it ready for later.
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    interstitialAd = null
                }
            })
    }

    // Displays the loaded interstitial Ad if available
    private fun showInterstitialAd() {
        if (interstitialAd != null) {
            // Setup callback to know when user closes the ad
            interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    // Ad closed -> Reset var and load the NEXT ad so it's ready for later
                    interstitialAd = null
                    loadInterstitialAd()
                }

                override fun onAdFailedToShowFullScreenContent(p0: com.google.android.gms.ads.AdError) {
                    interstitialAd = null
                }
            }

            interstitialAd?.show(this)
        } else {
            // If ad wasn't ready for some reason, try loading again for next time
            loadInterstitialAd()
        }
    }
}