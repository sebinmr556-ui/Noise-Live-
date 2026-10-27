package com.example.ui.activities

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.example.R
import com.example.data.AppRepository

class ThemePickerActivity : ComponentActivity() {

    private lateinit var repository: AppRepository
    private var selectedThemeName = "Desert Gold"
    private val ownedThemes = mutableSetOf("Desert Gold")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_theme_picker)

        repository = AppRepository.getInstance(applicationContext)

        val btnBack = findViewById<ImageView>(R.id.btnBackTheme)
        val btnSave = findViewById<Button>(R.id.btnSaveTheme)

        btnBack.setOnClickListener { finish() }

        val b1 = findViewById<Button>(R.id.btnPurchase1)
        val b2 = findViewById<Button>(R.id.btnPurchase2)
        val b3 = findViewById<Button>(R.id.btnPurchase3)
        val b4 = findViewById<Button>(R.id.btnPurchase4)
        val b5 = findViewById<Button>(R.id.btnPurchase5)
        val b6 = findViewById<Button>(R.id.btnPurchase6)

        val themeButtons = listOf(
            b1 to "Desert Gold",
            b2 to "Desert Moon",
            b3 to "Phoenix Fire",
            b4 to "Waterfall",
            b5 to "Marble Temple",
            b6 to "Cosmic Space"
        )

        fun updateUI() {
            for ((btn, name) in themeButtons) {
                if (btn == null) continue
                if (ownedThemes.contains(name)) {
                    if (selectedThemeName == name) {
                        btn.text = "Selected"
                        btn.setBackgroundResource(R.drawable.bg_save_gradient)
                    } else {
                        btn.text = "Apply"
                        btn.setBackgroundResource(R.drawable.bg_edit_button)
                    }
                } else {
                    btn.text = getString(R.string.action_purchase)
                    btn.setBackgroundResource(R.drawable.bg_edit_button)
                }
            }
        }

        for ((btn, name) in themeButtons) {
            btn?.setOnClickListener {
                if (ownedThemes.contains(name)) {
                    selectedThemeName = name
                    updateUI()
                    Toast.makeText(this, "$name selected! Click Save to apply.", Toast.LENGTH_SHORT).show()
                } else {
                    val balance = repository.currentUser.value.coinBalance
                    if (balance >= 15000L) {
                        repository.deductCoins(15000L, "Purchased room theme: $name")
                        ownedThemes.add(name)
                        selectedThemeName = name
                        updateUI()
                        Toast.makeText(this, "Unlocked $name for 15,000 Coins! ✨", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(
                            this,
                            "Insufficient balance ($balance). Need 15,000 coins.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }

        btnSave.setOnClickListener {
            val prefs = getSharedPreferences("VoiceRoomPrefs", Context.MODE_PRIVATE)
            prefs.edit().putString("active_theme", selectedThemeName).apply()
            Toast.makeText(this, "Theme '$selectedThemeName' saved successfully! 🏎️", Toast.LENGTH_SHORT).show()
            finish()
        }

        updateUI()
    }
}
