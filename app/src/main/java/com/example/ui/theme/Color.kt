package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Instagram Signature Gradients (bg-gradient-to-tr from-[#f09433] via-[#dc2743] to-[#bc1888])
val InstaGradientStart = Color(0xFFF09433)
val InstaGradientMiddle = Color(0xFFDC2743)
val InstaGradientEnd = Color(0xFFBC1888)

val InstagramSignatureGradient = Brush.linearGradient(
    colors = listOf(
        InstaGradientStart,
        InstaGradientMiddle,
        InstaGradientEnd
    )
)

val StoryGradient = InstagramSignatureGradient

// Pure Instagram Dark Mode Palette
val DarkBackground = Color(0xFF000000) // bg-black
val DarkSurface = Color(0xFF121212)    // bg-[#121212]
val DarkSurfaceElevated = Color(0xFF18181B) // zinc-900
val DarkBorder = Color(0xFF27272A)     // border-zinc-800
val DarkTextPrimary = Color(0xFFFFFFFF) // text-white
val DarkTextSecondary = Color(0xFFA1A1AA) // text-zinc-400
val DarkAccent = Color(0xFF0095F6)      // Instagram Blue
val InstaLikeRed = Color(0xFFED4956)

// Live & WebRTC Room Colors
val LiveRed = Color(0xFFFF2D55)
val WebRtcConnectedGreen = Color(0xFF34C759)
val AudioWavePurple = Color(0xFFBC1888)
val GoldStar = Color(0xFFFFD60A)
val InstaPurple = Color(0xFFBC1888)

// Light Theme Fallback (Kept minimal as app is strictly Dark Mode)
val LightBackground = Color(0xFF000000)
val LightSurface = Color(0xFF121212)
val LightSurfaceElevated = Color(0xFF18181B)
val LightBorder = Color(0xFF27272A)
val LightTextPrimary = Color(0xFFFFFFFF)
val LightTextSecondary = Color(0xFFA1A1AA)
val LightAccent = Color(0xFF0095F6)
