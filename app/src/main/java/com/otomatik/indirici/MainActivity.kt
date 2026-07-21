package com.otomatik.indirici

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.ViewFlipper
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var flipper: ViewFlipper
    private lateinit var dotsContainer: LinearLayout
    private lateinit var nextButton: TextView
    private lateinit var skipButton: TextView

    private val pageCount = 4
    private var currentPage = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        flipper = findViewById(R.id.viewFlipper)
        dotsContainer = findViewById(R.id.dotsContainer)
        nextButton = findViewById(R.id.nextButton)
        skipButton = findViewById(R.id.skipButton)

        buildDots()
        updateDots()
        updateButtons()

        nextButton.setOnClickListener { goToNextPage() }
        skipButton.setOnClickListener { finish() }
    }

    private fun buildDots() {
        dotsContainer.removeAllViews()
        for (i in 0 until pageCount) {
            val dot = ImageView(this)
            val size = (8 * resources.displayMetrics.density).toInt()
            val params = LinearLayout.LayoutParams(size, size)
            params.marginStart = (4 * resources.displayMetrics.density).toInt()
            params.marginEnd = (4 * resources.displayMetrics.density).toInt()
            dot.layoutParams = params
            dot.setImageResource(if (i == currentPage) R.drawable.dot_active else R.drawable.dot_inactive)
            dotsContainer.addView(dot)
        }
    }

    private fun updateDots() {
        for (i in 0 until dotsContainer.childCount) {
            val dot = dotsContainer.getChildAt(i) as ImageView
            dot.setImageResource(if (i == currentPage) R.drawable.dot_active else R.drawable.dot_inactive)
        }
    }

    private fun updateButtons() {
        if (currentPage == pageCount - 1) {
            nextButton.text = "Başla"
            skipButton.visibility = View.INVISIBLE
        } else {
            nextButton.text = "İleri"
            skipButton.visibility = View.VISIBLE
        }
    }

    private fun goToNextPage() {
        if (currentPage == pageCount - 1) {
            finish()
            return
        }
        flipper.inAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_in_right)
        flipper.outAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_out_left)
        flipper.showNext()
        currentPage++
        updateDots()
        updateButtons()
    }
}
