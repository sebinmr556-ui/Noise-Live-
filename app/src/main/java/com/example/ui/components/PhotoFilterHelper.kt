package com.example.ui.components

import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import com.example.model.PhotoFilter

object PhotoFilterHelper {
    fun getColorFilter(filter: PhotoFilter): ColorFilter? {
        return when (filter) {
            PhotoFilter.NORMAL -> null
            PhotoFilter.CLARENDON -> {
                // Boost contrast & cool tones
                val matrix = ColorMatrix().apply {
                    setToSaturation(1.35f)
                }
                ColorFilter.colorMatrix(matrix)
            }
            PhotoFilter.JUNO -> {
                // Warm punchy saturation
                val matrix = ColorMatrix(
                    floatArrayOf(
                        1.2f, 0f, 0f, 0f, 15f,
                        0f, 1.1f, 0f, 0f, 10f,
                        0f, 0f, 0.9f, 0f, -10f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                ColorFilter.colorMatrix(matrix)
            }
            PhotoFilter.VALENCIA -> {
                // Warm faded antique look
                val matrix = ColorMatrix(
                    floatArrayOf(
                        1.15f, 0.05f, 0f, 0f, 20f,
                        0f, 1.05f, 0.05f, 0f, 15f,
                        0f, 0f, 0.85f, 0f, 5f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                ColorFilter.colorMatrix(matrix)
            }
            PhotoFilter.SLUMBER -> {
                // Desaturated dreamy vintage
                val matrix = ColorMatrix().apply {
                    setToSaturation(0.65f)
                }
                ColorFilter.colorMatrix(matrix)
            }
            PhotoFilter.NOIR -> {
                // High contrast Black & White
                val matrix = ColorMatrix().apply {
                    setToSaturation(0f)
                }
                ColorFilter.colorMatrix(matrix)
            }
            PhotoFilter.CYBERPUNK -> {
                // Neon magenta and cyan highlights
                val matrix = ColorMatrix(
                    floatArrayOf(
                        1.3f, 0f, 0.2f, 0f, 25f,
                        0f, 0.9f, 0.1f, 0f, -5f,
                        0.2f, 0.1f, 1.4f, 0f, 35f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                ColorFilter.colorMatrix(matrix)
            }
            PhotoFilter.RETRO_90S -> {
                // Golden nostalgic tint
                val matrix = ColorMatrix(
                    floatArrayOf(
                        1.2f, 0.1f, 0f, 0f, 25f,
                        0.05f, 1.1f, 0f, 0f, 20f,
                        0f, 0f, 0.8f, 0f, -15f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                ColorFilter.colorMatrix(matrix)
            }
        }
    }
}
