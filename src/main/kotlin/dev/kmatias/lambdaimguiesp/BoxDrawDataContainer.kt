package dev.kmatias.lambdaimguiesp

import org.joml.Vector2f

data class BoxDrawDataContainer(
    val position: Vector2f,
    val size: Vector2f = Vector2f(100f, 100f),
    val color: Int = 0xFF00FFFF.toInt(),
    val name: String?,
    val nameColor: Int = 0xFFFFFFFF.toInt(),
    val distance: Double = 0.0,
    val health: Float = 20f,
    val maxHealth: Float = 20f,
    val hasHealth: Boolean = false,
    val healthText: String = "",
    val healthColor: Int = 0xFFFFFFFF.toInt(),
    val textWidth: Float = 0f,
    val textHeight: Float = 0f,
    val healthWidth: Float = 0f,
    val healthNormalized: Float = 1f
)