package com.example.flipit

import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.gms.ads.MobileAds
import com.google.firebase.analytics.FirebaseAnalytics

// Serves as the primary controller for the Flip It app
class MainActivity : AppCompatActivity() {

    // UI Components
    private lateinit var rootLayout: ConstraintLayout
    private lateinit var imgBackground: ImageView
    private lateinit var imgCoin: ImageView
    private lateinit var btnSettings: ImageButton
    private lateinit var txtResult: TextView

    // Logic Managers
    private lateinit var dataManager: DataManager
    private lateinit var adsManager: AdsManager
    private lateinit var coinManager: CoinManager
    private lateinit var firebaseAnalytics: FirebaseAnalytics

    // Game State
    private lateinit var gestureDetector: GestureDetector
    private var isFlipping = false
    private var flipCount = 0
    private var currentBackground = R.drawable.beach_background

    // Counter for remaining background changes
    private var backgroundChangesLeft = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize External SDKs (Ads and Analytics)
        MobileAds.initialize(this) {}
        firebaseAnalytics = FirebaseAnalytics.getInstance(this)

        // Initialize Data and Ad Managers
        dataManager = DataManager(this)
        adsManager = AdsManager(this)

        // Initialize UI References
        initUI()

        /// Initialize CoinManager
        coinManager = CoinManager(this, imgCoin, txtResult)

        // Load Ads in the background if the user hasn't purchased Remove Ads
        if (!dataManager.isAdsRemoved) {
            adsManager.loadAds()
        }

        // Set Initial App State
        coinManager.updateDefaultImage()
        imgBackground.setImageResource(currentBackground)

        // Setup User Input Listeners (Taps and Swipes)
        setupGestureDetector()

        // Settings Button Listener
        btnSettings.setOnClickListener {
            txtResult.visibility = View.INVISIBLE
            showSettingsDialog()
        }
    }

    // Links XML views to Kotlin variables and sets visual properties
    private fun initUI() {
        rootLayout = findViewById(R.id.rootLayout)
        imgBackground = findViewById(R.id.imgBackground)
        imgCoin = findViewById(R.id.imgCoin)
        btnSettings = findViewById(R.id.btnSettings)
        txtResult = findViewById(R.id.txtResult)

        // Set camera distance for a deeper 3D perspective during rotation
        val scale = resources.displayMetrics.density
        imgCoin.cameraDistance = 8000 * scale
    }

    // Handler for flipping the coin
    private fun handleFlipAction() {
        if (isFlipping) return
        isFlipping = true
        txtResult.visibility = View.INVISIBLE

        // Adjust result text color based on the current background for contrast
        val textColor = if (currentBackground == R.drawable.beach_background)
            android.R.color.black else android.R.color.white
        txtResult.setTextColor(resources.getColor(textColor, theme))

        // Trigger the 3D animation
        coinManager.flip {
            isFlipping = false
            checkAndShowAd()
        }
    }

    // Determine when an Interstitial Ad should be displayed
    private fun checkAndShowAd() {
        if (!dataManager.isAdsRemoved) {
            flipCount++
            // Show ad on the 1st flip and then every 3 flips thereafter
            if (flipCount == 1 || (flipCount > 1 && (flipCount - 1) % 3 == 0)) {
                adsManager.showInterstitial()
            }
        }
    }

    // Handle single taps and upward swipes on the coin
    private fun setupGestureDetector() {
        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                handleFlipAction()
                return true
            }
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, vx: Float, vy: Float): Boolean {
                // Detect fast upward swipe
                if (e1 != null && e2.y - e1.y < -100 && Math.abs(vy) > 100) {
                    handleFlipAction()
                    return true
                }
                return false
            }
            override fun onDown(e: MotionEvent): Boolean = true
        })
        imgCoin.setOnTouchListener { _, event -> gestureDetector.onTouchEvent(event) }
    }

    // Builds and displays the main Settings dialog dynamically based on purchase status
    private fun showSettingsDialog() {
        val options = mutableListOf("Change coins")
        // Change text based on whether backgrounds are unlocked
        if (dataManager.isPremiumUnlocked || backgroundChangesLeft > 0) options.add("Change background")
        else options.add("Change background ($2 / Free Ad)")
        // Only show "Remove Ads" if not already purchased
        if (!dataManager.isAdsRemoved) options.add("Remove Ads ($5)")
        options.add("Help")

        AlertDialog.Builder(this)
            .setTitle("Settings")
            .setItems(options.toTypedArray()) { _, which ->
                when (options[which]) {
                    "Change coins" -> showCoinDialog()
                    "Help" -> showHelpDialog()
                    "Remove Ads ($5)" -> showRemoveAdsDialog()
                    else -> showBackgroundDialogGate()
                }
            }.show()
    }

    // Dialog for selecting currency types
    private fun showCoinDialog() {
        val coinNames = arrayOf("Shekel", "Euro", "Dollar")
        AlertDialog.Builder(this).setTitle("Change coin").setItems(coinNames) { _, which ->
            coinManager.currentCoin = coinNames[which].lowercase()
            coinManager.updateDefaultImage()
        }.show()
    }

    // Offers payment or Rewarded Ad if locked
    private fun showBackgroundDialogGate() {
        if (dataManager.isPremiumUnlocked || backgroundChangesLeft > 0) {
            showSelectBackgroundOptions()
        } else {
            AlertDialog.Builder(this)
                .setTitle("Unlock Backgrounds")
                .setMessage("Pay \$2 for lifetime access or watch a video to change background 3 times")
                .setPositiveButton("Pay $2") { _, _ ->
                    dataManager.isPremiumUnlocked = true
                    showSelectBackgroundOptions()
                }
                .setNeutralButton("Watch Video") { _, _ ->
                    adsManager.showRewarded {
                        backgroundChangesLeft = 3
                        showSelectBackgroundOptions()
                    }
                }
                .setNegativeButton("Back", null).show()
        }
    }

    // Dialog to apply the selected background image
    private fun showSelectBackgroundOptions() {
        val names = arrayOf("Beach", "Road", "Garden")
        AlertDialog.Builder(this).setTitle("Select Background").setItems(names) { _, which ->
            currentBackground = when (which) {
                0 -> R.drawable.beach_background
                1 -> R.drawable.road_background
                else -> R.drawable.garden_background
            }
            imgBackground.setImageResource(currentBackground)
            if (!dataManager.isPremiumUnlocked && backgroundChangesLeft > 0) {
                backgroundChangesLeft--
            }
        }.show()
    }

    // Purchase simulation for removing advertisements
    private fun showRemoveAdsDialog() {
        AlertDialog.Builder(this).setTitle("Remove Ads")
            .setMessage("Remove all ads for 5$?")
            .setPositiveButton("Pay") { _, _ -> dataManager.isAdsRemoved = true }
            .setNegativeButton("Cancel", null).show()
    }

    // Basic instructional dialog
    private fun showHelpDialog() {
        AlertDialog.Builder(this).setTitle("How to Flip")
            .setMessage("Tap the coin or swipe up to flip it!").show()
    }
}