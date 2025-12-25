package com.example.flipit

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.widget.Button
import android.widget.RatingBar
import android.widget.TextView

class DialogManager(private val context: Context) {

    private var dialog: AlertDialog? = null

    fun showRateUsDialog(onRateSubmit: (Float) -> Unit) {
        val builder = AlertDialog.Builder(context)

        val inflater = LayoutInflater.from(context)
        val dialogView = inflater.inflate(R.layout.dialog_rate_us, null)
        builder.setView(dialogView)

        val btnContinue = dialogView.findViewById<Button>(R.id.btnContinue)
        val btnCancel = dialogView.findViewById<TextView>(R.id.btnCancel)
        val ratingBar = dialogView.findViewById<RatingBar>(R.id.ratingBar)

        dialog = builder.create()

        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        // Continue Button
        btnContinue.setOnClickListener {
            val rating = ratingBar.rating
            onRateSubmit(rating)
            dialog?.dismiss()
        }

        // Cancel Button
        btnCancel.setOnClickListener {
            dialog?.dismiss()
        }

        dialog?.show()
    }

    fun showShareDialog(onShareConfirmed: () -> Unit) {
        val builder = AlertDialog.Builder(context)
        val inflater = LayoutInflater.from(context)
        val dialogView = inflater.inflate(R.layout.dialog_share_app, null)
        builder.setView(dialogView)

        val btnSure = dialogView.findViewById<TextView>(R.id.btnSure)
        val btnNoThanks = dialogView.findViewById<TextView>(R.id.btnNoThanks)

        val dialog = builder.create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        btnSure.setOnClickListener {
            onShareConfirmed()
            dialog.dismiss()
        }

        btnNoThanks.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}