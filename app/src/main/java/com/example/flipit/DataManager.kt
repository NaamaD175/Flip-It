package com.example.flipit

import android.content.Context
import android.content.SharedPreferences

// Handles persistent data storage for the app - it used SharedPreferences
class DataManager(context: Context) {
    // Initialize SharedPreferences - a local key-value store on the device
    private val prefs: SharedPreferences = context.getSharedPreferences("FlipItPrefs", Context.MODE_PRIVATE)

    companion object {
        // Unique keys used to identify specific data items in the storage
        private const val KEY_PREMIUM_UNLOCKED = "isPremiumBackgroundUnlocked"
        private const val KEY_ADS_REMOVED = "isAdsRemoved"
        private const val KEY_APP_RATING = "app_rating"
    }

    // Manage the premium backgrounds unlock state
    var isPremiumUnlocked: Boolean
        // Returns the saved value, or false if it doesn't exist yet
        get() = prefs.getBoolean(KEY_PREMIUM_UNLOCKED, false)
        // Persists the new value to the SharedPreferences file asynchronously
        set(value) = prefs.edit().putBoolean(KEY_PREMIUM_UNLOCKED, value).apply()

    // Manage whether advertisements should be removed
    var isAdsRemoved: Boolean
        // Returns true if ads are removed, false otherwise
        get() = prefs.getBoolean(KEY_ADS_REMOVED, false)
        // Saves the updated ad-status preference
        set(value) = prefs.edit().putBoolean(KEY_ADS_REMOVED, value).apply()

    var appRating: Float
        // Returns the saved star rating, or 0.0f if the user hasn't rated yet
        get() = prefs.getFloat(KEY_APP_RATING, 0f)
        // Saves the new star rating to the local storage
        set(value) = prefs.edit().putFloat(KEY_APP_RATING, value).apply()

}