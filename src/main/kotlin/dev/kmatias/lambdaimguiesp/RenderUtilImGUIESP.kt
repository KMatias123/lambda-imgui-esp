package dev.kmatias.lambdaimguiesp

import net.minecraft.client.MinecraftClient
import net.minecraft.util.math.Vec3d
import net.minecraft.util.math.Box
import org.joml.Matrix4f
import org.joml.Vector2f
import org.joml.Vector4f
import org.joml.times
import kotlin.math.max
import kotlin.math.min

fun worldToScreen(
    worldPos: Vec3d,
    viewMatrix: Matrix4f,
    projectionMatrix: Matrix4f,
    tickDelta: Float
): Vector2f? {
    val mc = MinecraftClient.getInstance()
    val camera = mc.gameRenderer.camera

    val camPos = camera.cameraPos
    val x = (worldPos.x - camPos.x).toFloat()
    val y = (worldPos.y - camPos.y).toFloat()
    val z = (worldPos.z - camPos.z).toFloat()

    val clip = projectionMatrix * (viewMatrix * Vector4f(x, y, z, 1f))

    if (clip.w <= 0f) return null

    val ndcX = clip.x / clip.w
    val ndcY = clip.y / clip.w
    val ndcZ = clip.z / clip.w

    if (ndcX !in -1f..1f || ndcY !in -1f..1f || ndcZ !in -1f..1f) return null

    val scaledW = mc.window.width.toFloat()
    val scaledH = mc.window.height.toFloat()

    val screenX = (ndcX + 1f) * 0.5f * scaledW
    val screenY = (1f - (ndcY + 1f) * 0.5f) * scaledH

    return Vector2f(screenX, screenY)
}

fun getScreenBoundingBox(
    box: Box,
    viewMatrix: Matrix4f,
    projectionMatrix: Matrix4f
): Vector4f? {
    val mc = MinecraftClient.getInstance()
    val camPos = mc.gameRenderer.camera.cameraPos

    val corners = arrayOf(
        Vector4f(box.minX.toFloat(), box.minY.toFloat(), box.minZ.toFloat(), 1f),
        Vector4f(box.minX.toFloat(), box.minY.toFloat(), box.maxZ.toFloat(), 1f),
        Vector4f(box.minX.toFloat(), box.maxY.toFloat(), box.minZ.toFloat(), 1f),
        Vector4f(box.minX.toFloat(), box.maxY.toFloat(), box.maxZ.toFloat(), 1f),
        Vector4f(box.maxX.toFloat(), box.minY.toFloat(), box.minZ.toFloat(), 1f),
        Vector4f(box.maxX.toFloat(), box.minY.toFloat(), box.maxZ.toFloat(), 1f),
        Vector4f(box.maxX.toFloat(), box.maxY.toFloat(), box.minZ.toFloat(), 1f),
        Vector4f(box.maxX.toFloat(), box.maxY.toFloat(), box.maxZ.toFloat(), 1f)
    )

    var minX = Float.MAX_VALUE
    var minY = Float.MAX_VALUE
    var maxX = -Float.MAX_VALUE
    var maxY = -Float.MAX_VALUE

    var anyVisible = false

    for (corner in corners) {
        val x = corner.x - camPos.x.toFloat()
        val y = corner.y - camPos.y.toFloat()
        val z = corner.z - camPos.z.toFloat()

        val clip = projectionMatrix * (viewMatrix * Vector4f(x, y, z, 1f))

        if (clip.w <= 0f) continue

        val ndcX = clip.x / clip.w
        val ndcY = clip.y / clip.w

        val screenX = (ndcX + 1f) * 0.5f * mc.window.width.toFloat()
        val screenY = (1f - (ndcY + 1f) * 0.5f) * mc.window.height.toFloat()

        minX = min(minX, screenX)
        minY = min(minY, screenY)
        maxX = max(maxX, screenX)
        maxY = max(maxY, screenY)
        anyVisible = true
    }

    if (!anyVisible) return null

    return Vector4f(minX, minY, maxX, maxY)
}