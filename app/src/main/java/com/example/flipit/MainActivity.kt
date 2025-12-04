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

class MainActivity : AppCompatActivity() {

    private lateinit var rootLayout: ConstraintLayout
    private lateinit var imgCoin: ImageView
    private lateinit var btnSettings: ImageButton
    private lateinit var txtResult: TextView

    private lateinit var gestureDetector: GestureDetector

    private var isFlipping = false

    // הגדרות
    private var currentCoin = "shekel"
    private var currentBackground = R.drawable.beach_background

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        rootLayout = findViewById(R.id.rootLayout)
        imgCoin = findViewById(R.id.imgCoin)
        btnSettings = findViewById(R.id.btnSettings)
        txtResult = findViewById(R.id.txtResult)

        // הצגת מטבע ברירת מחדל
        setDefaultCoinImage()

        // עומק לסיבוב
        val scale = resources.displayMetrics.density
        imgCoin.cameraDistance = 8000 * scale

        // כפתור הגדרות
        btnSettings.setOnClickListener {
            hideResult()
            showSettingsDialog()
        }

        // הגדרת זיהוי תנועות (Swipe & Tap)
        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            private val SWIPE_THRESHOLD = 100
            private val SWIPE_VELOCITY_THRESHOLD = 100

            // זיהוי לחיצה קצרה (Tap)
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                flipCoin()
                return true
            }

            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (e1 == null) return false

                val diffY = e2.y - e1.y
                val diffX = e2.x - e1.x

                // זיהוי Swipe למעלה (diffY שלילי)
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

        // מעביר אירועי מגע לתמונת המטבע
        imgCoin.setOnTouchListener { v, event ->
            gestureDetector.onTouchEvent(event)
            true
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    // ---------- ניהול תוצאה ----------

    private fun hideResult() {
        txtResult.visibility = View.INVISIBLE
    }

    // ---------- תמונת מטבע לפי מטבע נוכחי ----------

    private fun setDefaultCoinImage() {
        val frontRes = when (currentCoin) {
            "shekel" -> R.drawable.coin_shekel_front
            "euro" -> R.drawable.coin_euro_front
            "dollar" -> R.drawable.coin_dollar_front
            else -> R.drawable.coin_shekel_front
        }
        imgCoin.setImageResource(frontRes)
    }

    // ---------- הטלת מטבע (משופרת) ----------

    private fun flipCoin() {
        if (isFlipping) return
        isFlipping = true

        hideResult()

        // עדכון צבע הטקסט לפי הרקע הנוכחי
        val textColor = if (currentBackground == R.drawable.beach_background) {
            resources.getColor(android.R.color.black, theme) // שחור לחוף
        } else {
            resources.getColor(android.R.color.white, theme) // לבן לשאר הרקעים
        }
        txtResult.setTextColor(textColor)


        // בחירת צד המטבע (עץ / פלי)
        val isHeads = Random.nextBoolean()

        // בחירת תמונות לפי מטבע נוכחי
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

        // סיבוב מלא
        val targetRotation = (360 * rotationRounds).toFloat() + if (!isHeads) 180f else 0f

        // סיבוב סביב ציר Y
        val rotate = ObjectAnimator.ofFloat(imgCoin, "rotationY", 0f, targetRotation).apply {
            duration = totalDuration
            interpolator = AccelerateDecelerateInterpolator()
        }

        // תנועה למעלה ולמטה
        val upAndDown = ObjectAnimator.ofFloat(imgCoin, "translationY", 0f, -600f, 0f).apply {
            duration = totalDuration
            interpolator = OvershootInterpolator(1.0f)
        }

        val flipSet = AnimatorSet().apply {
            playTogether(rotate, upAndDown)
        }

        // מציגים את צד ה-Heads לפני תחילת האנימציה
        imgCoin.setImageResource(headsRes)

        flipSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator) {
                imgCoin.rotationY = 0f
            }

            override fun onAnimationEnd(animation: Animator) {
                // איפוס סופי של מאפיינים
                imgCoin.rotationY = 0f
                imgCoin.translationY = 0f

                // הצגת התמונה הסופית
                imgCoin.setImageResource(resultDrawable)
                isFlipping = false

                // מציגים את התוצאה
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

    // ---------- SETTINGS ----------

    private fun showSettingsDialog() {
        val options = arrayOf("Change background", "Change coins", "Help")

        AlertDialog.Builder(this)
            .setTitle("Settings")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showBackgroundDialog()
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


    private fun showBackgroundDialog() {
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
                rootLayout.setBackgroundResource(backgroundRes)
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
}