package com.example.ui.components

import android.os.SystemClock
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import kotlin.math.PI
import kotlin.math.sin

/**
 * Immutable snapshot of synchronized VIP border rotation angles and glow intensities
 * for a single animation frame across all chatroom and voice stage components.
 */
@Immutable
data class VIPAnimationSyncState(
    val tier1RotationDegrees: Float = 0f,
    val tier2RotationDegrees: Float = 0f,
    val tier3RotationDegrees: Float = 0f,
    val tier1GlowAlpha: Float = 0.7f,
    val tier2GlowAlpha: Float = 0.8f,
    val tier3GlowAlpha: Float = 0.9f
) {
    fun rotationForTier(vipTier: Int): Float = when (vipTier.coerceIn(0, 3)) {
        3 -> tier3RotationDegrees
        2 -> tier2RotationDegrees
        1 -> tier1RotationDegrees
        else -> 0f
    }

    fun glowAlphaForTier(vipTier: Int): Float = when (vipTier.coerceIn(0, 3)) {
        3 -> tier3GlowAlpha
        2 -> tier2GlowAlpha
        1 -> tier1GlowAlpha
        else -> 0f
    }
}

val LocalVIPAnimationSync = compositionLocalOf<VIPAnimationSyncState?> { null }

/**
 * Centralized manager that synchronizes VIP border rotations and glow pulses across
 * all chatroom messages, speaker seats, and profile badges.
 *
 * Uses a 12,000ms master harmonic cycle (LCM of Tier 1 = 6000ms, Tier 2 = 4000ms, Tier 3 = 2000ms)
 * so all three tiers rotate continuously without phase jumps and share a single animation ticker.
 */
object VIPAnimationManager {
    const val MASTER_CYCLE_DURATION_MS = 12_000
    const val TIER_1_PERIOD_MS = 6_000L
    const val TIER_2_PERIOD_MS = 4_000L
    const val TIER_3_PERIOD_MS = 2_000L

    private val startUptimeMillis: Long = try {
        SystemClock.uptimeMillis()
    } catch (_: Throwable) {
        0L
    }

    /**
     * Deterministically computes the rotation angle [0f, 360f) for a given [vipTier]
     * and [elapsedMillis], ensuring phase-locked angles across components.
     */
    fun computeRotationDegrees(vipTier: Int, elapsedMillis: Long = currentElapsedMillis()): Float {
        val safeElapsed = elapsedMillis.coerceAtLeast(0L)
        val period = when (vipTier.coerceIn(0, 3)) {
            3 -> TIER_3_PERIOD_MS
            2 -> TIER_2_PERIOD_MS
            1 -> TIER_1_PERIOD_MS
            else -> return 0f
        }
        return ((safeElapsed % period).toFloat() / period.toFloat()) * 360f
    }

    /**
     * Deterministically computes the pulsing glow alpha [0.55f, 1.0f] for a given [vipTier]
     * and [elapsedMillis].
     */
    fun computeGlowAlpha(vipTier: Int, elapsedMillis: Long = currentElapsedMillis()): Float {
        val tier = vipTier.coerceIn(0, 3)
        if (tier == 0) return 0f
        val safeElapsed = elapsedMillis.coerceAtLeast(0L)
        val period = when (tier) {
            3 -> TIER_3_PERIOD_MS
            2 -> TIER_2_PERIOD_MS
            else -> TIER_1_PERIOD_MS
        }
        val radians = ((safeElapsed % period).toDouble() / period.toDouble()) * 2.0 * PI
        val normalizedWave = ((sin(radians) + 1.0) / 2.0).toFloat()
        return 0.55f + (normalizedWave * 0.45f)
    }

    /**
     * Converts a master harmonic progress value in [0f, 1f) (spanning 12,000ms)
     * into a complete [VIPAnimationSyncState] for all tiers.
     */
    fun stateFromMasterProgress(masterProgress: Float): VIPAnimationSyncState {
        val clamped = masterProgress.coerceIn(0f, 1f)
        val elapsedEquivalentMs = (clamped * MASTER_CYCLE_DURATION_MS).toLong()
        return VIPAnimationSyncState(
            tier1RotationDegrees = (clamped * 2f * 360f) % 360f,
            tier2RotationDegrees = (clamped * 3f * 360f) % 360f,
            tier3RotationDegrees = (clamped * 6f * 360f) % 360f,
            tier1GlowAlpha = computeGlowAlpha(1, elapsedEquivalentMs),
            tier2GlowAlpha = computeGlowAlpha(2, elapsedEquivalentMs),
            tier3GlowAlpha = computeGlowAlpha(3, elapsedEquivalentMs)
        )
    }

    fun currentElapsedMillis(): Long {
        return try {
            (SystemClock.uptimeMillis() - startUptimeMillis).coerceAtLeast(0L)
        } catch (_: Throwable) {
            0L
        }
    }

    /**
     * Returns the shared [VIPAnimationSyncState] from [LocalVIPAnimationSync] if provided
     * by a parent [ProvideVIPAnimationSync], or creates a single shared harmonic ticker.
     */
    @Composable
    fun rememberSyncedState(): VIPAnimationSyncState {
        val existing = LocalVIPAnimationSync.current
        if (existing != null) {
            return existing
        }
        val masterProgress by rememberMasterProgressState()
        return remember(masterProgress) {
            stateFromMasterProgress(masterProgress)
        }
    }

    @Composable
    private fun rememberMasterProgressState(): State<Float> {
        val infiniteTransition = rememberInfiniteTransition(label = "vip_animation_manager_master")
        return infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = MASTER_CYCLE_DURATION_MS,
                    easing = LinearEasing
                ),
                repeatMode = RepeatMode.Restart
            ),
            label = "vip_master_harmonic_phase"
        )
    }
}

/**
 * Provides a single centralized [VIPAnimationSyncState] to all descendant composables
 * (voice stage slots, chatroom message avatars, username badges, and headers) so that
 * every VIP border rotates in lockstep with a single animation ticker.
 */
@Composable
fun ProvideVIPAnimationSync(
    content: @Composable () -> Unit
) {
    val syncedState = VIPAnimationManager.rememberSyncedState()
    CompositionLocalProvider(LocalVIPAnimationSync provides syncedState) {
        content()
    }
}
