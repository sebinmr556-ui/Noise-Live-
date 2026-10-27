package com.example.ui.activities

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.example.R
import com.example.data.AppRepository
import com.example.model.FallaGift
import com.example.model.FallaGiftCategory

class GiftSelectionBottomSheetDialog(
    context: Context,
    private val repository: AppRepository,
    private val onGiftSent: (name: String, count: Int, totalCost: Long) -> Unit = { _, _, _ -> },
    private val onTopUpRequested: () -> Unit = {}
) : Dialog(context) {

    private var selectedMultiplier = 1
    private var selectedRecipient = "All"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.bottom_sheet_gift_dialog)

        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        window?.setGravity(Gravity.BOTTOM)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val tvBalance = findViewById<TextView>(R.id.tvSheetCoinBalance)
        val btnTopUp = findViewById<Button>(R.id.btnSheetTopUp)
        val btnBalanceTopUp = findViewById<View>(R.id.btnBalanceTopUp)
        val btnRecipientAll = findViewById<TextView>(R.id.btnRecipientAll)

        val pill1 = findViewById<TextView>(R.id.pillCount1)
        val pill5 = findViewById<TextView>(R.id.pillCount5)
        val pill10 = findViewById<TextView>(R.id.pillCount10)
        val pill66 = findViewById<TextView>(R.id.pillCount66)

        // Coin Balance from repository
        val userCoins = repository.currentUser.value.coinBalance
        tvBalance.text = "${formatCoins(userCoins)} >"

        val openTopUp = {
            dismiss()
            onTopUpRequested()
        }
        btnTopUp.setOnClickListener { openTopUp() }
        btnBalanceTopUp.setOnClickListener { openTopUp() }

        // Recipient Selection
        btnRecipientAll.setOnClickListener {
            selectedRecipient = if (selectedRecipient == "All") "Host" else "All"
            btnRecipientAll.text = selectedRecipient
            Toast.makeText(context, "Gifting targeted to: $selectedRecipient", Toast.LENGTH_SHORT).show()
        }

        // Multipliers
        val pills = listOf(
            pill1 to 1,
            pill5 to 5,
            pill10 to 10,
            pill66 to 66
        )

        fun updateMultiplier(count: Int) {
            selectedMultiplier = count
            pills.forEach { (tv, c) ->
                if (c == count) {
                    tv.setBackgroundResource(R.drawable.bg_edit_button)
                    tv.setTextColor(context.getColor(R.color.white))
                } else {
                    tv.setBackgroundResource(R.drawable.bg_pill_badge)
                    tv.setTextColor(context.getColor(R.color.text_secondary_translucent))
                }
            }
        }

        pills.forEach { (tv, count) ->
            tv.setOnClickListener { updateMultiplier(count) }
        }

        // Gift click handlers (8 items from screenshot)
        val gifts = listOf(
            R.id.giftItem1 to ("UT hart" to 1),
            R.id.giftItem2 to ("Rose" to 5),
            R.id.giftItem3 to ("Star" to 2500),
            R.id.giftItem4 to ("Kiss" to 1200),
            R.id.giftItem5 to ("Love Ring" to 1100),
            R.id.giftItem6 to ("Zoma chat" to 2500),
            R.id.giftItem7 to ("Dog Singing" to 2500),
            R.id.giftItem8 to ("Piggy" to 750)
        )

        gifts.forEach { (viewId, pair) ->
            val (name, unitCost) = pair
            findViewById<View>(viewId)?.setOnClickListener {
                val totalCost = unitCost.toLong() * selectedMultiplier
                val curBalance = repository.currentUser.value.coinBalance
                if (curBalance < totalCost) {
                    Toast.makeText(
                        context,
                        "Insufficient coins ($curBalance). Need $totalCost. Opening Top Up!",
                        Toast.LENGTH_SHORT
                    ).show()
                    openTopUp()
                } else {
                    val giftObj = FallaGift(
                        id = "gift_${System.currentTimeMillis()}",
                        name = name,
                        icon = "🎁",
                        coinCost = unitCost,
                        category = FallaGiftCategory.PREMIUM,
                        animationEffect = "sparkle"
                    )
                    val activeRoom = repository.voiceRooms.value.firstOrNull()
                    if (activeRoom != null) {
                        repository.sendFallaGift(
                            roomId = activeRoom.id,
                            gift = giftObj,
                            receiverId = "host",
                            receiverName = selectedRecipient,
                            quantity = selectedMultiplier
                        )
                    }
                    onGiftSent(name, selectedMultiplier, totalCost)
                    val remaining = repository.currentUser.value.coinBalance
                    tvBalance.text = "${formatCoins(remaining)} >"
                    Toast.makeText(
                        context,
                        "Sent $selectedMultiplier x $name to $selectedRecipient! 💖",
                        Toast.LENGTH_SHORT
                    ).show()
                    dismiss()
                }
            }
        }
    }

    private fun formatCoins(amount: Long): String {
        return when {
            amount >= 1_000_000 -> String.format("%.1fM", amount / 1_000_000.0)
            amount >= 1_000 -> String.format("%.1fK", amount / 1_000.0)
            else -> amount.toString()
        }
    }
}
