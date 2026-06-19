package com.example.counterclicker

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    private var count = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val label = TextView(this).apply {
            text = getString(R.string.counter_label, count)
            textSize = 32f
            gravity = Gravity.CENTER
        }

        val button = Button(this).apply {
            text = getString(R.string.increment_button)
            setOnClickListener {
                count += 1
                label.text = getString(R.string.counter_label, count)
            }
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(48, 48, 48, 48)
            addView(label)
            addView(button)
        }

        setContentView(layout)
    }
}
