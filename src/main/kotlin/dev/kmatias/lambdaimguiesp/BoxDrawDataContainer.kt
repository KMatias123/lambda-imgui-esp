package dev.kmatias.lambdaimguiesp

import org.joml.Vector2f
import java.awt.Color

/**
 * Health is 0..1
 */
data class BoxDrawDataContainer(val position: Vector2f, val size: Vector2f = Vector2f(100f, 100f),
    val color: Color = Color(0xFF00FFFF.toInt()), val padding: Float = 0f, val name: String?,
    val health: Float? = null)
