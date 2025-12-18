package dev.kmatias.lambdaimguiesp.modules

import com.lambda.Lambda
import com.lambda.event.events.GuiEvent
import com.lambda.event.listener.SafeListener.Companion.listen
import com.lambda.friend.FriendManager
import com.lambda.graphics.RenderMain
import com.lambda.module.Module
import com.lambda.module.tag.ModuleTag
import com.lambda.util.extension.tickDelta
import com.lambda.util.world.entitySearch
import dev.kmatias.lambdaimguiesp.BoxDrawDataContainer
import dev.kmatias.lambdaimguiesp.getScreenBoundingBox
import dev.kmatias.lambdaimguiesp.worldToScreen
import imgui.ImGui
import imgui.flag.ImGuiCol
import imgui.flag.ImGuiStyleVar
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.mob.HostileEntity
import net.minecraft.entity.player.PlayerEntity
import org.joml.Vector2f
import java.awt.Color
import java.lang.Math.clamp
import kotlin.math.max
import kotlin.math.min

object ImguiESP : Module(
    name = "ImguiESP",
    description = "Draws 2D boxes around entities in the world.",
    tag = ModuleTag.RENDER,
) {
    val rangeSetting = setting("Range", 255.0, 0.0..512.0, 1.0) { true }

    val mode = setting("Mode", Modes.Basic) { true }
    val colorMode = setting("ColorMode", ColorModes.Normal) { true }

    enum class Modes {
        Basic, Hitbox
    }

    enum class ColorModes {
        Static, Normal
    }

    val staticColor = setting("Static Color", Color(255, 255, 0, 255)) { colorMode.value == ColorModes.Static }

    val playerColor = setting("Player Color", Color(255, 0, 0, 255)) { colorMode.value == ColorModes.Normal }
    val friendColor = setting("Friend Color", Color(0, 255, 255, 255)) { colorMode.value == ColorModes.Normal }
    val hostileColor = setting("Hostile Color", Color(255, 100, 0, 255)) { colorMode.value == ColorModes.Normal }
    val passiveColor = setting("Passive Color", Color(0, 255, 0, 255)) { colorMode.value == ColorModes.Normal }

    val scaleMinimum = setting("Scale Minimum", 5.0, 0.0..100.0, 1.0) { mode.value == Modes.Basic }
    val scaleSetting = setting("Scale", 15.0, 0.001..100.0, 1.0) { mode.value == Modes.Basic }
    val scaleMaximum = setting("Scale Maximum", 100.0, 0.0..100.0, 1.0) { mode.value == Modes.Basic }

    val padding = setting("Padding", 10.0, 0.0..100.0, 1.0) { mode.value == Modes.Hitbox }

    private val boxes = mutableListOf<BoxDrawDataContainer>()

    // todo: nametags

    fun getColorForEntity(entity: LivingEntity): Color {
        if (colorMode.value == ColorModes.Static) {
            return staticColor.value
        }

        return when (entity) {
            is PlayerEntity -> {
                if (FriendManager.isFriend(entity.uuid)) {
                    friendColor.value
                }

                playerColor.value
            }

            is HostileEntity -> {
                hostileColor.value
            }

            else -> {
                passiveColor.value
            }
        }
    }

    init {
        listen<GuiEvent.NewFrame> { _ ->
            ImGui.pushStyleColor(ImGuiCol.Border, 0xFF00FFFF.toInt())
            ImGui.pushStyleColor(ImGuiCol.Text, 0xFF00FFFF.toInt())
            ImGui.pushStyleColor(ImGuiCol.FrameBg, 0xFF00FFFF.toInt())
            ImGui.pushStyleVar(ImGuiStyleVar.Alpha, 0.5f)
            ImGui.pushStyleVar(ImGuiStyleVar.WindowBorderSize, 4f)
            ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, 3f, 0f)
            ImGui.pushStyleVar(ImGuiStyleVar.WindowRounding, 0f)
            ImGui.pushStyleVar(ImGuiStyleVar.ItemSpacing, 0f, 0f)
            ImGui.pushStyleVar(ImGuiStyleVar.WindowMinSize, 10f, 10f)

            boxes.clear()
            entitySearch<LivingEntity>(rangeSetting.value) {
                return@entitySearch it !is PlayerEntity
            }.forEach { entity ->
                worldToScreen(
                    entity.pos,
                    RenderMain.modelViewMatrix,
                    RenderMain.projectionMatrix,
                    Lambda.mc.tickDelta
                )?.let { coordinate ->
                    when (mode.value) {
                        Modes.Basic -> {
                            boxes.add(
                                BoxDrawDataContainer(
                                    coordinate,
                                    Vector2f(
                                        100 / (clamp(
                                            (mc.player?.distanceTo(entity) ?: 100.0).toDouble(),
                                            scaleMinimum.value,
                                            scaleMaximum.value
                                        ) / scaleSetting.value.toFloat()).toFloat(),
                                        100 / (clamp(
                                            (mc.player?.distanceTo(entity) ?: 100.0).toDouble(),
                                            scaleMinimum.value,
                                            scaleMaximum.value
                                        ) / scaleSetting.value.toFloat()).toFloat()
                                    ),
                                    padding = 0f,
                                    color = getColorForEntity(entity),
                                    name = entity.name.string
                                )
                            )
                        }

                        Modes.Hitbox -> {
                            val box = entity.boundingBox.offset(
                                -entity.x,
                                -entity.y,
                                -entity.z
                            ).offset(
                                entity.lastRenderX + (entity.x - entity.lastRenderX) * mc.tickDelta,
                                entity.lastRenderY + (entity.y - entity.lastRenderY) * mc.tickDelta,
                                entity.lastRenderZ + (entity.z - entity.lastRenderZ) * mc.tickDelta
                            )

                            getScreenBoundingBox(
                                box,
                                RenderMain.modelViewMatrix,
                                RenderMain.projectionMatrix
                            )?.let { rect ->
                                val minX = rect.x
                                val minY = rect.y
                                val maxX = rect.z
                                val maxY = rect.w

                                val w = maxX - minX + padding.value.toFloat()
                                val h = maxY - minY + padding.value.toFloat()
                                boxes.add(
                                    BoxDrawDataContainer(
                                        Vector2f(minX + (maxX - minX) / 2f, minY + (maxY - minY) / 2f),
                                        Vector2f(w, h),
                                        color = getColorForEntity(entity),
                                        padding = padding.value.toFloat(),
                                        name = entity.name.string
                                    )
                                )
                            }
                        }
                    }
                }
            }

            boxes.forEach { drawTask ->
                val minX = max(drawTask.position.x - drawTask.size.x / 2, 0f)
                val minY = max(drawTask.position.y - drawTask.size.y / 2, 0f)
                val maxX = min(drawTask.position.x + drawTask.size.x / 2, Lambda.mc.window.width.toFloat())
                val maxY = min(drawTask.position.y + drawTask.size.y / 2, Lambda.mc.window.height.toFloat())
                if (drawTask.name != null) {
                    ImGui.getBackgroundDrawList()
                        .addText(
                            minX,
                            max(minY - ImGui.calcTextSizeY(drawTask.name) - 10, 0f),
                            ImGui.getColorU32(1f, 1f, 1f, 1f),
                            drawTask.name
                        )
                }

                ImGui.getBackgroundDrawList()
                    .addRect(
                        minX,
                        minY,
                        maxX,
                        maxY,
                        ImGui.getColorU32(
                            drawTask.color.red / 255f,
                            drawTask.color.green / 255f,
                            drawTask.color.blue / 255f,
                            drawTask.color.alpha / 255f
                        )
                    )

            }

            ImGui.popStyleVar(6)
            ImGui.popStyleColor(3)
        }
    }
}
