package com.example.flipit

import android.app.Activity
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

// Handles all advertisement logic for the app
class AdsManager(private val activity: Activity) {

    // Variables to store the ad objects once loaded from the network
    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null

    // Google AdMob Test IDs
    private val INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712"
    private val REWARDED_ID = "ca-app-pub-3940256099942544/5224354917"

    // Entry point to load all ad types simultaneously
    fun loadAds() {
        loadInterstitial()
        loadRewarded()
    }

    // Loads an Interstitial Ad from Google AdMob servers
    private fun loadInterstitial() {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(activity, INTERSTITIAL_ID, adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(ad: InterstitialAd) { interstitialAd = ad } // Ad loaded successfully
            override fun onAdFailedToLoad(error: LoadAdError) { interstitialAd = null } // Loaded failed
        })
    }

    // Loads a Rewarded Ad from Google AdMob servers
    private fun loadRewarded() {
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(activity, REWARDED_ID, adRequest, object : RewardedAdLoadCallback() {
            override fun onAdLoaded(ad: RewardedAd) { rewardedAd = ad } // Rewarded ad ready
            override fun onAdFailedToLoad(error: LoadAdError) { rewardedAd = null } // Failed
        })
    }

    // Displays the Interstitial Ad to the user
    fun showInterstitial() {
        interstitialAd?.let { ad ->
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    loadInterstitial() // Load next ad in background
                }
            }
            ad.show(activity)
        } ?: loadInterstitial() // If ad wasn't ready, try loading for next time
    }

    // Displays the Rewarded Ad and executes a callback upon successful completion
    fun showRewarded(onRewardEarned: () -> Unit) {
        rewardedAd?.let { ad ->
            ad.show(activity) {
                // This block runs only when the user is entitled to the reward
                onRewardEarned()
                rewardedAd = null
                loadRewarded() // Prepare next rewarded ad
            }
        } ?: run { loadRewarded() } // Ad not ready, try loading again
    }
}