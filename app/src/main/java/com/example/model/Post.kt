package com.example.model

enum class PhotoFilter(val displayName: String, val description: String) {
    NORMAL("Normal", "Original photo balance"),
    CLARENDON("Clarendon", "High contrast with cool vivid shadows"),
    JUNO("Juno", "Warm punchy tones and boosted saturation"),
    VALENCIA("Valencia", "Warm faded antique film aesthetic"),
    SLUMBER("Slumber", "Dreamy desaturated vintage tones"),
    NOIR("Noir", "Dramatic monochrome with deep shadows"),
    CYBERPUNK("Cyber", "Futuristic neon magenta and cyan highlights"),
    RETRO_90S("90s Retro", "Golden nostalgic grain and rich shadows")
}

enum class FeedFilter(val title: String) {
    FOR_YOU("For You"),
    FOLLOWING("Following"),
    TRENDING("Trending")
}

data class RecommendationReason(
    val matchPercentage: Int,
    val primaryReason: String,
    val factors: List<String>
)

data class Post(
    val id: String,
    val userId: String,
    val username: String,
    val userAvatar: String,
    val userCategory: String = "Creator",
    val isVerified: Boolean = false,
    val imageUrl: String,
    val caption: String,
    val location: String = "",
    val timestampText: String = "Just now",
    val timestampMillis: Long = System.currentTimeMillis(),
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val isLiked: Boolean = false,
    val isBookmarked: Boolean = false,
    val tags: List<String> = emptyList(),
    val filterApplied: PhotoFilter = PhotoFilter.NORMAL,
    val recommendationReason: RecommendationReason? = null
)

data class Comment(
    val id: String,
    val postId: String,
    val userId: String,
    val username: String,
    val userAvatar: String,
    val text: String,
    val timestampText: String = "Just now",
    val likesCount: Int = 0,
    val isLiked: Boolean = false
)

data class Story(
    val id: String,
    val userId: String,
    val username: String,
    val userAvatar: String,
    val mediaUrl: String,
    val caption: String = "",
    val timestampMillis: Long = System.currentTimeMillis(),
    val isSeen: Boolean = false,
    val durationSeconds: Int = 5
) {
    val isExpired: Boolean
        get() = (System.currentTimeMillis() - timestampMillis) >= (24 * 60 * 60 * 1000L)

    val formattedTimestamp: String
        get() {
            val diffHours = ((System.currentTimeMillis() - timestampMillis) / (60 * 60 * 1000L)).coerceAtLeast(0)
            return if (diffHours == 0L) {
                val diffMins = ((System.currentTimeMillis() - timestampMillis) / (60 * 1000L)).coerceAtLeast(1)
                "${diffMins}m"
            } else {
                "${diffHours}h"
            }
        }
}

data class StoryHighlight(
    val id: String,
    val title: String,
    val coverUrl: String,
    val storiesCount: Int = 1
)

enum class ActivityType {
    LIKE,
    COMMENT,
    FOLLOW
}

data class ActivityNotification(
    val id: String,
    val type: ActivityType,
    val actorName: String,
    val actorUsername: String,
    val actorAvatar: String,
    val text: String,
    val postPreviewUrl: String? = null,
    val timestampText: String = "2h",
    val timestampMillis: Long = System.currentTimeMillis() - 7200000,
    val isFollowedBack: Boolean = false
)
