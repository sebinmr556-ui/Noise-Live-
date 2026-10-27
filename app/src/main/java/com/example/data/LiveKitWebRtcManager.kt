package com.example.data

import com.example.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

class LiveKitWebRtcManager(
    private val coroutineScope: CoroutineScope
) {
    private val _connectionState = MutableStateFlow(WebRtcConnectionState.DISCONNECTED)
    val connectionState: StateFlow<WebRtcConnectionState> = _connectionState.asStateFlow()

    private val _currentRoom = MutableStateFlow<VoiceRoom?>(null)
    val currentRoom: StateFlow<VoiceRoom?> = _currentRoom.asStateFlow()

    private val _isMicrophoneMuted = MutableStateFlow(true)
    val isMicrophoneMuted: StateFlow<Boolean> = _isMicrophoneMuted.asStateFlow()

    private val _isHandRaised = MutableStateFlow(false)
    val isHandRaised: StateFlow<Boolean> = _isHandRaised.asStateFlow()

    private val _roomStats = MutableStateFlow(RoomStats())
    val roomStats: StateFlow<RoomStats> = _roomStats.asStateFlow()

    private val _lastSoundEffect = MutableStateFlow<String?>(null)
    val lastSoundEffect: StateFlow<String?> = _lastSoundEffect.asStateFlow()

    private var audioMeterJob: Job? = null

    fun connectToRoom(room: VoiceRoom, asSpeaker: Boolean = false) {
        coroutineScope.launch {
            _connectionState.value = WebRtcConnectionState.CONNECTING
            _isMicrophoneMuted.value = !asSpeaker
            _isHandRaised.value = false

            delay(400)
            _connectionState.value = WebRtcConnectionState.CONNECTED

            val userParticipant = Participant(
                id = SampleData.currentUser.id,
                name = SampleData.currentUser.fullName,
                username = SampleData.currentUser.username,
                avatarUrl = SampleData.currentUser.avatarUrl,
                role = if (asSpeaker) ParticipantRole.SPEAKER else ParticipantRole.LISTENER,
                vipTier = VipTier.GOLD,
                userLevel = 12,
                wealthLevel = 8,
                charmLevel = 9,
                isMuted = _isMicrophoneMuted.value,
                isTalking = false,
                audioLevel = 0f,
                slotIndex = if (asSpeaker) 0 else -1
            )

            val updatedSpeakers = if (asSpeaker) {
                room.speakers.filter { it.id != userParticipant.id } + userParticipant
            } else {
                room.speakers.filter { it.id != userParticipant.id }
            }

            val updatedListeners = if (!asSpeaker) {
                room.listeners.filter { it.id != userParticipant.id } + userParticipant
            } else {
                room.listeners.filter { it.id != userParticipant.id }
            }

            _currentRoom.value = room.copy(
                isJoined = true,
                connectionState = WebRtcConnectionState.CONNECTED,
                speakers = updatedSpeakers,
                listeners = updatedListeners,
                isMuted = _isMicrophoneMuted.value,
                isHandRaised = false
            )

            startAudioLevelMetering()
        }
    }

    fun claimSpeakerSlot(slotIndex: Int) {
        val room = _currentRoom.value ?: return
        if (room.lockedSlotIndices.contains(slotIndex)) return // Locked slot

        _isMicrophoneMuted.value = false
        val userParticipant = Participant(
            id = SampleData.currentUser.id,
            name = SampleData.currentUser.fullName,
            username = SampleData.currentUser.username,
            avatarUrl = SampleData.currentUser.avatarUrl,
            role = ParticipantRole.SPEAKER,
            vipTier = VipTier.GOLD,
            userLevel = 12,
            wealthLevel = 8,
            charmLevel = 9,
            isMuted = false,
            isTalking = true,
            audioLevel = 0.5f,
            slotIndex = slotIndex
        )
        val updatedSpeakers = room.speakers.filter { it.id != userParticipant.id } + userParticipant
        val updatedListeners = room.listeners.filter { it.id != userParticipant.id }
        _currentRoom.value = room.copy(
            speakers = updatedSpeakers,
            listeners = updatedListeners,
            isMuted = false
        )
        startAudioLevelMetering()
    }

    fun releaseSpeakerSlot() {
        val room = _currentRoom.value ?: return
        _isMicrophoneMuted.value = true
        val userParticipant = Participant(
            id = SampleData.currentUser.id,
            name = SampleData.currentUser.fullName,
            username = SampleData.currentUser.username,
            avatarUrl = SampleData.currentUser.avatarUrl,
            role = ParticipantRole.LISTENER,
            vipTier = VipTier.GOLD,
            isMuted = true,
            isTalking = false,
            audioLevel = 0f,
            slotIndex = -1
        )
        val updatedSpeakers = room.speakers.filter { it.id != userParticipant.id }
        val updatedListeners = room.listeners.filter { it.id != userParticipant.id } + userParticipant
        _currentRoom.value = room.copy(
            speakers = updatedSpeakers,
            listeners = updatedListeners,
            isMuted = true
        )
    }

    fun toggleSeatLock(slotIndex: Int) {
        val room = _currentRoom.value ?: return
        val updatedLocks = if (room.lockedSlotIndices.contains(slotIndex)) {
            room.lockedSlotIndices - slotIndex
        } else {
            // If someone is in the slot, kick them out of the slot
            val updatedSpeakers = room.speakers.filter { it.slotIndex != slotIndex }
            room.lockedSlotIndices + slotIndex
        }
        _currentRoom.value = room.copy(lockedSlotIndices = updatedLocks)
    }

    fun kickSpeaker(participantId: String) {
        val room = _currentRoom.value ?: return
        val kicked = room.speakers.find { it.id == participantId } ?: return
        val listener = kicked.copy(role = ParticipantRole.LISTENER, slotIndex = -1, isMuted = true)
        _currentRoom.value = room.copy(
            speakers = room.speakers.filter { it.id != participantId },
            listeners = room.listeners + listener
        )
    }

    fun muteSpeakerByMod(participantId: String) {
        val room = _currentRoom.value ?: return
        val updated = room.speakers.map {
            if (it.id == participantId) it.copy(isMuted = true, isTalking = false, audioLevel = 0f) else it
        }
        _currentRoom.value = room.copy(speakers = updated)
    }

    fun playSoundEffect(sound: String) {
        _lastSoundEffect.value = sound
        coroutineScope.launch {
            delay(2500)
            if (_lastSoundEffect.value == sound) {
                _lastSoundEffect.value = null
            }
        }
    }

    fun reconnectWebRtc() {
        val room = _currentRoom.value ?: return
        coroutineScope.launch {
            _connectionState.value = WebRtcConnectionState.RECONNECTING
            delay(800)
            _connectionState.value = WebRtcConnectionState.CONNECTED
        }
    }

    fun disconnect() {
        leaveRoom()
    }

    fun leaveRoom() {
        audioMeterJob?.cancel()
        audioMeterJob = null
        _connectionState.value = WebRtcConnectionState.DISCONNECTED
        _currentRoom.value = null
        _isHandRaised.value = false
        _isMicrophoneMuted.value = true
        _lastSoundEffect.value = null
    }

    fun toggleMute() {
        val newMuted = !_isMicrophoneMuted.value
        _isMicrophoneMuted.value = newMuted
        val room = _currentRoom.value ?: return

        val updatedSpeakers = room.speakers.map { participant ->
            if (participant.id == SampleData.currentUser.id) {
                participant.copy(isMuted = newMuted, audioLevel = if (newMuted) 0f else participant.audioLevel)
            } else {
                participant
            }
        }
        _currentRoom.value = room.copy(isMuted = newMuted, speakers = updatedSpeakers)
    }

    fun toggleRaiseHand() {
        val newHand = !_isHandRaised.value
        _isHandRaised.value = newHand
        val room = _currentRoom.value ?: return

        val updatedListeners = room.listeners.map { participant ->
            if (participant.id == SampleData.currentUser.id) {
                participant.copy(handRaised = newHand)
            } else {
                participant
            }
        }
        _currentRoom.value = room.copy(isHandRaised = newHand, listeners = updatedListeners)
    }

    fun promoteListenerToSpeaker(participantId: String, slotIndex: Int) {
        val room = _currentRoom.value ?: return
        val listener = room.listeners.find { it.id == participantId } ?: return
        val promoted = listener.copy(role = ParticipantRole.SPEAKER, handRaised = false, isMuted = false, slotIndex = slotIndex)

        _currentRoom.value = room.copy(
            speakers = room.speakers + promoted,
            listeners = room.listeners.filter { it.id != participantId }
        )
    }

    private fun startAudioLevelMetering() {
        audioMeterJob?.cancel()
        audioMeterJob = coroutineScope.launch {
            while (isActive) {
                delay(120)
                val room = _currentRoom.value ?: break
                if (room.connectionState != WebRtcConnectionState.CONNECTED) break

                val updatedSpeakers = room.speakers.map { speaker ->
                    if (speaker.isMuted) {
                        speaker.copy(isTalking = false, audioLevel = 0f)
                    } else if (speaker.id == SampleData.currentUser.id && !_isMicrophoneMuted.value) {
                        val level = (Random.nextFloat() * 0.7f + 0.15f)
                        speaker.copy(isTalking = level > 0.25f, audioLevel = level)
                    } else {
                        val willTalk = Random.nextFloat() > 0.4f
                        val level = if (willTalk) Random.nextFloat() * 0.8f else 0.05f
                        speaker.copy(isTalking = level > 0.2f, audioLevel = level)
                    }
                }

                val jitterLatency = (20 + Random.nextInt(8))
                _roomStats.value = _roomStats.value.copy(latencyMs = jitterLatency)

                _currentRoom.value = room.copy(speakers = updatedSpeakers, stats = _roomStats.value)
            }
        }
    }
}
