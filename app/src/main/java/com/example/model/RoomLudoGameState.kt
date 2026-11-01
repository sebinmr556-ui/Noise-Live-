package com.example.model

data class RoomLudoSeat(
    val colorName: String = "",
    val playerId: String = "",
    val playerName: String = "",
    val userName: String = playerName,
    val playerAvatar: String = ""
)

data class RoomLudoGameState(
    val isActive: Boolean = false,
    val seats: Map<String, RoomLudoSeat> = emptyMap(),
    val tokenSteps: Map<String, List<Int>> = emptyMap(),
    val currentTurnColor: String = "RED",
    val diceValue: Int = 1,
    val hasRolled: Boolean = false,
    val turnStartedAtMillis: Long = System.currentTimeMillis(),
    val winnerName: String? = null,
    val statusMessage: String = ""
)
