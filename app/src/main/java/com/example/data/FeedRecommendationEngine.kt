package com.example.data

import com.example.model.Post
import com.example.model.RecommendationReason
import com.example.model.UserProfile
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.roundToInt

class FeedRecommendationEngine {

    // User's interest affinity tags with implicit weights
    private val defaultUserAffinities = mapOf(
        "photography" to 0.95f,
        "landscape" to 0.90f,
        "audio" to 0.85f,
        "architecture" to 0.80f,
        "music" to 0.75f,
        "travel" to 0.70f,
        "tech" to 0.65f,
        "minimalism" to 0.60f
    )

    /**
     * Algorithmic recommendation formula:
     * Score = (TopicAffinity * 35) + (CreatorAffinity * 25) + (EngagementVelocity * 20) + (FreshnessDecay * 20)
     */
    fun rankForYou(
        posts: List<Post>,
        currentUser: UserProfile,
        followedUserIds: Set<String>,
        userLikedPostIds: Set<String>,
        userBookmarkedPostIds: Set<String>
    ): List<Post> {
        val scoredPosts = posts.map { post ->
            val (score, reason) = computeScoreAndReason(
                post = post,
                currentUser = currentUser,
                isFollowed = followedUserIds.contains(post.userId),
                isLiked = userLikedPostIds.contains(post.id) || post.isLiked,
                isBookmarked = userBookmarkedPostIds.contains(post.id) || post.isBookmarked
            )
            post.copy(recommendationReason = reason) to score
        }

        return scoredPosts.sortedByDescending { it.second }.map { it.first }
    }

    fun getFollowingFeed(posts: List<Post>, followedUserIds: Set<String>): List<Post> {
        // Chronological order of followed accounts
        return posts
            .filter { followedUserIds.contains(it.userId) || it.userId == "user_me" }
            .sortedByDescending { it.timestampMillis }
    }

    fun getTrendingFeed(posts: List<Post>): List<Post> {
        // Ranked by viral velocity (likes + 2 * comments)
        return posts.sortedByDescending { it.likesCount + it.commentsCount * 3 }
    }

    private fun computeScoreAndReason(
        post: Post,
        currentUser: UserProfile,
        isFollowed: Boolean,
        isLiked: Boolean,
        isBookmarked: Boolean
    ): Pair<Float, RecommendationReason> {
        // 1. Topic Affinity
        var topicScore = 0.2f
        var matchedTopic = "General"
        for (tag in post.tags) {
            val affinity = defaultUserAffinities[tag.lowercase()]
            if (affinity != null && affinity > topicScore) {
                topicScore = affinity
                matchedTopic = tag.replaceFirstChar { it.uppercase() }
            }
        }

        // 2. Creator Affinity
        val creatorScore = when {
            post.userId == currentUser.id -> 1.0f
            isFollowed -> 0.85f
            else -> 0.35f
        }

        // 3. Engagement Velocity
        val rawEngagement = post.likesCount + (post.commentsCount * 4)
        val engagementScore = (ln((rawEngagement + 1).toDouble()) / ln(6000.0)).toFloat().coerceIn(0.1f, 1.0f)

        // 4. Freshness Decay (half-life of 24h)
        val ageHours = ((System.currentTimeMillis() - post.timestampMillis) / 3600000.0).coerceAtLeast(0.1)
        val freshnessScore = exp(-0.03 * ageHours).toFloat().coerceIn(0.1f, 1.0f)

        // Weighted total
        val totalScore = (topicScore * 35f) + (creatorScore * 25f) + (engagementScore * 20f) + (freshnessScore * 20f)
        val matchPercentage = totalScore.roundToInt().coerceIn(60, 99)

        val factors = mutableListOf<String>()
        if (isFollowed) {
            factors.add("You follow @${post.username}")
        }
        if (topicScore > 0.6f) {
            factors.add("High affinity with #${matchedTopic.lowercase()}")
        }
        if (isBookmarked) {
            factors.add("Saved to your bookmarks collection")
        }
        if (isLiked) {
            factors.add("You interacted with similar posts")
        }
        if (freshnessScore > 0.8f) {
            factors.add("Freshly published within the community")
        }
        if (factors.isEmpty()) {
            factors.add("Popular across the NOICE LIVE network")
        }

        val primaryReason = when {
            isFollowed && topicScore > 0.7f -> "Based on your interest in $matchedTopic & following @${post.username}"
            topicScore > 0.7f -> "Recommended from your engagement with $matchedTopic content"
            isFollowed -> "From creator @${post.username} whom you follow"
            else -> "Trending across NOICE LIVE communities today"
        }

        return totalScore to RecommendationReason(
            matchPercentage = matchPercentage,
            primaryReason = primaryReason,
            factors = factors
        )
    }
}
