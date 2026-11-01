package com.example.model

data class YouTubeQueueItem(
    val videoId: String = "",
    val title: String = "",
    val requestedBy: String = "",
    val addedBy: String = requestedBy
)

data class RoomYouTubeStream(
    val videoId: String = "",
    val title: String = "",
    val isPlaying: Boolean = false,
    val currentPositionSec: Int = 0,
    val startedBy: String = "",
    val queue: List<YouTubeQueueItem> = emptyList()
)
