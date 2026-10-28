package com.example.ui.activities

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.AppRepository

class VoiceChatroomActivity : ComponentActivity() {

    private lateinit var repository: AppRepository

    // Audio Permission Launcher (triggered strictly upon user selection of an open voice slot)
    private lateinit var requestAudioPermissionLauncher: ActivityResultLauncher<String>
    private var pendingSlotIndex: Int? = null
    private var occupiedSlotIndex: Int? = null
    private var isMicMuted = false
    private var isQueenMuted = false
    private var isKingMuted = false

    // Views
    private lateinit var tvRoomTitle: TextView
    private lateinit var tvOnlineCount: TextView
    private lateinit var tvDiamondCounter: TextView
    private lateinit var tvAnnouncementBody: TextView
    private lateinit var btnEditAnnouncement: Button
    private lateinit var btnFollowHost: TextView
    private lateinit var btnModeratorView: TextView
    private lateinit var btnCloseRoom: ImageView
    private lateinit var layoutChatLog: LinearLayout
    private lateinit var etMessageInput: EditText
    private lateinit var btnGiftBox: ImageView
    private lateinit var btnMenuSettings: ImageView
    private lateinit var btnThemeStore: TextView
    private lateinit var btnDiceGame: ImageView
    private lateinit var btnMusicSpeaker: ImageView
    private lateinit var btnMicHandRaise: ImageView
    private lateinit var ivWaveIndicator1: ImageView
    private lateinit var ivWaveIndicator2: ImageView

    // Slot containers mapping
    private val slotContainers = mutableMapOf<Int, LinearLayout>()
    private val slotIcons = mutableMapOf<Int, ImageView>()
    private val slotNumbers = mutableMapOf<Int, TextView>()
    private val lockedSlots = mutableSetOf(4, 8, 10)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_voice_chatroom)

        repository = AppRepository.getInstance(applicationContext)

        initViews()
        setupAudioPermissionLauncher()
        setupSlotClickListeners()
        setupToolbarActions()
        setupChatInput()
    }

    private fun initViews() {
        tvRoomTitle = findViewById(R.id.tvRoomTitle)
        tvOnlineCount = findViewById(R.id.tvOnlineCount)
        tvDiamondCounter = findViewById(R.id.tvDiamondCounter)
        tvAnnouncementBody = findViewById(R.id.tvAnnouncementBody)
        btnEditAnnouncement = findViewById(R.id.btnEditAnnouncement)
        btnFollowHost = findViewById(R.id.btnFollowHost)
        btnModeratorView = findViewById(R.id.btnModeratorView)
        btnCloseRoom = findViewById(R.id.btnCloseRoom)
        layoutChatLog = findViewById(R.id.layoutChatLog)
        etMessageInput = findViewById(R.id.etMessageInput)
        btnGiftBox = findViewById(R.id.btnGiftBox)
        btnMenuSettings = findViewById(R.id.btnMenuSettings)
        btnThemeStore = findViewById(R.id.btnThemeStore)
        btnDiceGame = findViewById(R.id.btnDiceGame)
        btnMusicSpeaker = findViewById(R.id.btnMusicSpeaker)
        btnMicHandRaise = findViewById(R.id.btnMicHandRaise)
        ivWaveIndicator1 = findViewById(R.id.ivWaveIndicator1)
        ivWaveIndicator2 = findViewById(R.id.ivWaveIndicator2)

        // Map interactive slots (3 to 10)
        val containerIds = listOf(
            3 to R.id.slotContainer3,
            4 to R.id.slotContainer4,
            5 to R.id.slotContainer5,
            6 to R.id.slotContainer6,
            7 to R.id.slotContainer7,
            8 to R.id.slotContainer8,
            9 to R.id.slotContainer9,
            10 to R.id.slotContainer10
        )
        val iconIds = listOf(
            3 to R.id.ivSlotIcon3,
            4 to R.id.ivSlotIcon4,
            5 to R.id.ivSlotIcon5,
            6 to R.id.ivSlotIcon6,
            7 to R.id.ivSlotIcon7,
            8 to R.id.ivSlotIcon8,
            9 to R.id.ivSlotIcon9,
            10 to R.id.ivSlotIcon10
        )
        val numberIds = listOf(
            3 to R.id.tvSlotNumber3,
            4 to R.id.tvSlotNumber4,
            5 to R.id.tvSlotNumber5,
            6 to R.id.tvSlotNumber6,
            7 to R.id.tvSlotNumber7,
            8 to R.id.tvSlotNumber8,
            9 to R.id.tvSlotNumber9,
            10 to R.id.tvSlotNumber10
        )

        containerIds.forEach { (slot, id) -> slotContainers[slot] = findViewById(id) }
        iconIds.forEach { (slot, id) -> slotIcons[slot] = findViewById(id) }
        numberIds.forEach { (slot, id) -> slotNumbers[slot] = findViewById(id) }
    }

    /**
     * Initializes the ActivityResultLauncher for Manifest.permission.RECORD_AUDIO.
     * This ensures runtime permission is requested dynamically upon user interaction with open voice slots.
     */
    private fun setupAudioPermissionLauncher() {
        requestAudioPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            val slot = pendingSlotIndex
            pendingSlotIndex = null

            if (isGranted) {
                if (slot != null) {
                    occupySlot(slot)
                    Toast.makeText(
                        this,
                        "Microphone access granted! You are now live on Slot $slot 🎙️",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } else {
                Toast.makeText(
                    this,
                    getString(R.string.permission_audio_rationale),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    /**
     * Attaches click listeners to all voice slots matching the screenshot.
     * Clicking an open slot (+) triggers runtime audio permission dynamically.
     * Clicking a locked slot (padlock) informs the user of admin lock.
     */
    private fun setupSlotClickListeners() {
        // Featured duo on top (Slots 1 & 2)
        findViewById<View>(R.id.slotView1)?.setOnClickListener {
            Toast.makeText(this, "Queen (Host) is speaking live 👑", Toast.LENGTH_SHORT).show()
        }
        findViewById<View>(R.id.slotView2)?.setOnClickListener {
            Toast.makeText(this, "King (Co-host) is on stage 🦁", Toast.LENGTH_SHORT).show()
        }

        // Stage Slots 3 through 10
        for (slot in 3..10) {
            val container = slotContainers[slot] ?: continue

            container.setOnClickListener {
                if (lockedSlots.contains(slot)) {
                    // Locked slot clicked
                    showLockedSlotDialog(slot)
                } else if (occupiedSlotIndex == slot) {
                    // Clicking already occupied slot allows leaving or toggling mute
                    showSlotOptionsDialog(slot)
                } else {
                    // Open slot clicked: Check audio recording permission dynamically!
                    handleOpenSlotClick(slot)
                }
            }
        }
    }

    /**
     * Triggered strictly when user selects an open (+) voice slot.
     * Complies with the requirement that audio permission is only requested upon seat selection.
     */
    private fun handleOpenSlotClick(slot: Int) {
        repository.liveKitManager.requestClaimSeat(
            context = this,
            slotIndex = slot,
            onPermissionRequired = {
                pendingSlotIndex = slot
                requestAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            },
            onSeatClaimed = { success, errorMsg ->
                if (success) {
                    occupySlot(slot)
                    Toast.makeText(this, "Joined Slot $slot as live speaker! 🎙️", Toast.LENGTH_SHORT).show()
                } else if (errorMsg != null) {
                    Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun occupySlot(slot: Int) {
        // Release any previous slot
        occupiedSlotIndex?.let { prevSlot ->
            resetSlotUI(prevSlot)
        }

        occupiedSlotIndex = slot
        val user = repository.currentUser.value

        val icon = slotIcons[slot]
        val label = slotNumbers[slot]

        // Update UI to show speaking audio indicator
        icon?.setImageResource(R.drawable.ic_sound_wave)
        icon?.setBackgroundResource(R.drawable.bg_edit_button)
        icon?.setPadding(10, 10, 10, 10)

        label?.text = user.username
        label?.setTextColor(getColor(R.color.room_gold))

        // Notify room
        addChatMessagePill("You joined slot $slot as speaker 🎙️")
        repository.claimSpeakerSlot(slot, this)
    }

    private fun resetSlotUI(slot: Int) {
        val icon = slotIcons[slot]
        val label = slotNumbers[slot]

        icon?.setImageResource(R.drawable.ic_plus_circle)
        icon?.setBackgroundResource(R.drawable.bg_slot_circle)
        icon?.setPadding(14, 14, 14, 14)

        label?.text = slot.toString()
        label?.setTextColor(getColor(R.color.text_secondary_translucent))
    }

    private fun showLockedSlotDialog(slot: Int) {
        AlertDialog.Builder(this)
            .setTitle("Slot $slot is Locked")
            .setMessage("This seat has been locked by the Room Host. You must be an approved moderator or have VIP tier 3+ to sit here.")
            .setPositiveButton("OK", null)
            .setNeutralButton("Request Host") { _, _ ->
                addChatMessagePill("Sent seat unlock request to host for Slot $slot")
                Toast.makeText(this, "Request sent to host!", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun showSlotOptionsDialog(slot: Int) {
        val options = if (isMicMuted) arrayOf("Unmute Mic", "Leave Seat") else arrayOf("Mute Mic", "Leave Seat")
        AlertDialog.Builder(this)
            .setTitle("Seat $slot Controls")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        isMicMuted = !isMicMuted
                        val msg = if (isMicMuted) "Muted microphone" else "Unmuted microphone"
                        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                    }
                    1 -> {
                        resetSlotUI(slot)
                        occupiedSlotIndex = null
                        repository.releaseSpeakerSlot()
                        addChatMessagePill("You stepped down from Slot $slot")
                        Toast.makeText(this, "Left seat $slot", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .show()
    }

    private fun setupToolbarActions() {
        btnCloseRoom.setOnClickListener { finish() }

        btnModeratorView.setOnClickListener {
            showModeratorViewDialog()
        }

        btnFollowHost.setOnClickListener {
            val isFollowing = btnFollowHost.text == "✓"
            if (isFollowing) {
                btnFollowHost.text = "+"
                btnFollowHost.setBackgroundResource(R.drawable.bg_edit_button)
                Toast.makeText(this, "Unfollowed Host", Toast.LENGTH_SHORT).show()
            } else {
                btnFollowHost.text = "✓"
                btnFollowHost.setBackgroundResource(R.drawable.bg_save_gradient)
                Toast.makeText(this, "Following Host! 🌟", Toast.LENGTH_SHORT).show()
            }
        }

        btnEditAnnouncement.setOnClickListener {
            val input = EditText(this)
            input.setText(tvAnnouncementBody.text)
            AlertDialog.Builder(this)
                .setTitle("Edit Announcement")
                .setView(input)
                .setPositiveButton("Save") { _, _ ->
                    val newText = input.text.toString().trim()
                    if (newText.isNotBlank()) {
                        tvAnnouncementBody.text = newText
                        addChatMessagePill("Announcement updated: $newText")
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // Open Gift Bottom Sheet
        btnGiftBox.setOnClickListener {
            val giftDialog = GiftSelectionBottomSheetDialog(
                context = this,
                repository = repository,
                onGiftSent = { name, count, cost ->
                    addChatMessagePill("Sent $count x $name (🪙 $cost) to room! 🎁")
                },
                onTopUpRequested = {
                    Toast.makeText(this, "Redirecting to Top Up Wallet...", Toast.LENGTH_SHORT).show()
                }
            )
            giftDialog.show()
        }

        // Open Theme Store
        val openThemes = {
            val intent = Intent(this, ThemePickerActivity::class.java)
            startActivity(intent)
        }
        btnMenuSettings.setOnClickListener { openThemes() }
        btnThemeStore.setOnClickListener { openThemes() }

        // Dice Game
        btnDiceGame.setOnClickListener {
            val die1 = (1..6).random()
            val die2 = (1..6).random()
            val sum = die1 + die2
            addChatMessagePill("🎲 @${repository.currentUser.value.username} rolled [$die1, $die2] = $sum!")
            Toast.makeText(this, "Rolled $sum!", Toast.LENGTH_SHORT).show()
        }

        // Music Speaker toggle
        btnMusicSpeaker.setOnClickListener {
            Toast.makeText(this, "Room Audio & Background Music toggled", Toast.LENGTH_SHORT).show()
        }

        // Mic / Hand Raise
        btnMicHandRaise.setOnClickListener {
            if (occupiedSlotIndex != null) {
                isMicMuted = !isMicMuted
                val state = if (isMicMuted) "Mic muted" else "Mic active"
                Toast.makeText(this, state, Toast.LENGTH_SHORT).show()
            } else {
                addChatMessagePill("✋ @${repository.currentUser.value.username} raised hand for speaker slot")
                Toast.makeText(this, "Hand raised! Waiting for host approval.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Moderator View for Room Owners:
     * Allows muting/unmuting individual stage speakers, muting all speakers,
     * unlocking/locking seats, or ending the live broadcast session.
     */
    private fun showModeratorViewDialog() {
        val queenAction = if (isQueenMuted) "🔊 Unmute Slot 1 (Queen - Host)" else "🔇 Mute Slot 1 (Queen - Host)"
        val kingAction = if (isKingMuted) "🔊 Unmute Slot 2 (King - Co-Host)" else "🔇 Mute Slot 2 (King - Co-Host)"
        val activeSlotLabel = occupiedSlotIndex?.let { slot ->
            if (isMicMuted) "🔊 Unmute Slot $slot (@${repository.currentUser.value.username})"
            else "🔇 Mute Slot $slot (@${repository.currentUser.value.username})"
        } ?: "🔒 Toggle Seat 4 Lock (${if (lockedSlots.contains(4)) "Unlock" else "Lock"})"

        val options = arrayOf(
            queenAction,
            kingAction,
            activeSlotLabel,
            "🔇 Mute All Stage Speakers",
            "🛑 End Broadcast Session"
        )

        AlertDialog.Builder(this)
            .setTitle("🛡️ Room Owner & Moderator View")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        isQueenMuted = !isQueenMuted
                        ivWaveIndicator1.alpha = if (isQueenMuted) 0.25f else 1.0f
                        val msg = if (isQueenMuted) "🛡️ Moderator muted Queen (Slot 1)" else "🛡️ Moderator unmuted Queen (Slot 1)"
                        addChatMessagePill(msg)
                        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                    }
                    1 -> {
                        isKingMuted = !isKingMuted
                        ivWaveIndicator2.alpha = if (isKingMuted) 0.25f else 1.0f
                        val msg = if (isKingMuted) "🛡️ Moderator muted King (Slot 2)" else "🛡️ Moderator unmuted King (Slot 2)"
                        addChatMessagePill(msg)
                        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                    }
                    2 -> {
                        val slot = occupiedSlotIndex
                        if (slot != null) {
                            isMicMuted = !isMicMuted
                            repository.liveKitManager.toggleMute()
                            val msg = if (isMicMuted) "🛡️ Moderator muted Slot $slot" else "🛡️ Moderator unmuted Slot $slot"
                            addChatMessagePill(msg)
                            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                        } else {
                            if (lockedSlots.contains(4)) {
                                lockedSlots.remove(4)
                                slotIcons[4]?.setImageResource(R.drawable.ic_plus_circle)
                                slotIcons[4]?.setBackgroundResource(R.drawable.bg_slot_circle)
                                addChatMessagePill("🔓 Moderator unlocked Slot 4")
                            } else {
                                lockedSlots.add(4)
                                slotIcons[4]?.setImageResource(R.drawable.ic_padlock)
                                slotIcons[4]?.setBackgroundResource(R.drawable.bg_slot_locked)
                                addChatMessagePill("🔒 Moderator locked Slot 4")
                            }
                        }
                    }
                    3 -> {
                        isQueenMuted = true
                        isKingMuted = true
                        isMicMuted = true
                        ivWaveIndicator1.alpha = 0.25f
                        ivWaveIndicator2.alpha = 0.25f
                        repository.liveKitManager.muteAllSpeakers()
                        addChatMessagePill("🛡️ Room owner muted all stage speakers")
                        Toast.makeText(this, "All stage speakers muted", Toast.LENGTH_SHORT).show()
                    }
                    4 -> {
                        confirmEndBroadcastDialog()
                    }
                }
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun confirmEndBroadcastDialog() {
        AlertDialog.Builder(this)
            .setTitle("🛑 End Broadcast Session?")
            .setMessage("Are you sure you want to end this live voice broadcast? All speakers and listeners will be disconnected immediately.")
            .setPositiveButton("End Broadcast") { _, _ ->
                repository.liveKitManager.endBroadcastSession {
                    Toast.makeText(
                        this,
                        "Broadcast session ended by room owner.",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupChatInput() {
        etMessageInput.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEND ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
            ) {
                val text = etMessageInput.text.toString().trim()
                if (text.isNotBlank()) {
                    addChatMessagePill("@${repository.currentUser.value.username}: $text")
                    etMessageInput.text.clear()
                }
                true
            } else {
                false
            }
        }
    }

    private fun addChatMessagePill(text: String) {
        val tv = TextView(this)
        tv.text = text
        tv.setTextColor(getColor(R.color.white))
        tv.textSize = 12f
        tv.setBackgroundResource(R.drawable.bg_pill_badge)
        tv.setPadding(24, 12, 24, 12)

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.topMargin = 12
        tv.layoutParams = params

        layoutChatLog.addView(tv)
    }

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, VoiceChatroomActivity::class.java)
            context.startActivity(intent)
        }
    }
}
