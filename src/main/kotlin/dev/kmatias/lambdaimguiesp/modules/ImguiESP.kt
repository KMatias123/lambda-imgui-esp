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
import imgui.flag.ImDrawFlags
import imgui.flag.ImGuiCol
import imgui.flag.ImGuiStyleVar
import net.minecraft.client.render.entity.state.*
import net.minecraft.entity.Entity
import net.minecraft.entity.ItemEntity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.SpawnGroup
import net.minecraft.entity.mob.AmbientEntity
import net.minecraft.entity.mob.HostileEntity
import net.minecraft.entity.mob.WaterCreatureEntity
import net.minecraft.entity.passive.FishEntity
import net.minecraft.entity.passive.PassiveEntity
import net.minecraft.entity.player.PlayerEntity
import org.joml.Vector2f
import java.awt.Color
import java.lang.Math.clamp
import kotlin.math.max

object ImguiESP : Module(
    name = "ImguiESP",
    description = "Draws 2D boxes around entities in the world.",
    tag = ModuleTag.RENDER,
) {
    val rangeSetting = setting("Range", 255.0, 0.0..512.0, 1.0) { true }

    val mode = setting("Mode", Modes.Hitbox) { true }
    val colorMode = setting("ColorMode", ColorModes.Normal) { true }

    enum class Modes {
        Basic, Hitbox
    }

    enum class ColorModes {
        Static, Normal
    }

    val staticColor = setting("Static Color", Color(255, 255, 0, 255)) { colorMode.value == ColorModes.Static }

    val friendColor = setting("Friend Color", Color(0, 255, 255, 255)) { colorMode.value == ColorModes.Normal }
    val playerColor = setting("Player Color", Color(255, 0, 0, 255)) { colorMode.value == ColorModes.Normal }
    val hostileColor = setting("Hostile Color", Color(255, 100, 0, 255)) { colorMode.value == ColorModes.Normal }
    val passiveColor = setting("Passive Color", Color(0, 255, 0, 255)) { colorMode.value == ColorModes.Normal }
    val itemColor = setting("Item Color", Color(255, 183, 0, 255)) { colorMode.value == ColorModes.Normal }
    val ambientColor = setting("Ambient Color", Color(255, 255, 255, 255)) { colorMode.value == ColorModes.Normal }
    val fishColor = setting("Fish Color", Color(187, 0, 255, 255)) { colorMode.value == ColorModes.Normal }
    val otherColor = setting("Other Color", Color(0x00FFA6FF)) { colorMode.value == ColorModes.Normal }

    val scaleMinimum = setting("Scale Minimum", 5.0, 0.0..100.0, 1.0) { mode.value == Modes.Basic }
    val scaleSetting = setting("Scale", 15.0, 0.001..100.0, 1.0) { mode.value == Modes.Basic }
    val scaleMaximum = setting("Scale Maximum", 100.0, 0.0..100.0, 1.0) { mode.value == Modes.Basic }

    val padding = setting("Padding", 10.0, 0.0..20.0, 1.0) { mode.value == Modes.Hitbox }
    val borderThickness = setting("Border Thickness", 1f, 1f..8f, .1f) { true }

    val enablePlayers = setting("Enable Players", true) { true }
    val enableFriends = setting("Enable Friends", true) { true }
    val enableMonsters = setting("Enable Mobs", true) { true }
    val enableAnimals = setting("Enable Animals", true) { true }
    val enableFish = setting("Enable Fish", true) { true }
    val enableItems = setting("Enable Items", true) { true }
    val enableAmbient = setting("Enable Ambient", true) { true }
    val enableOther = setting("Enable Other", true) { true }

    val nametagsFriends = setting("Show Friend Nametags", true) { true }
    val nametagsPlayers = setting("Show Player Nametags", true) { true }
    val nametagsMonsters = setting("Show Monster Nametags", true) { true }
    val nametagsAnimals = setting("Show Animal Nametags", true) { true }
    val nametagsFishes = setting("Show Fish Nametags", true) { true }
    val nametagsItems = setting("Show Item Nametags", true) { true }
    val nametagsAmbient = setting("Show Ambient Nametags", true) { true }
    val nametagsOther = setting("Show Other Nametags", true) { true }

    private val boxes = mutableListOf<BoxDrawDataContainer>()

    // todo: fix precision issues (change 3d stuff from float to double)

    fun getColorForEntity(entity: Entity): Color {
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

            is FishEntity -> {
                fishColor.value
            }

            is PassiveEntity -> {
                passiveColor.value
            }

            is ItemEntity -> {
                itemColor.value
            }

            is AmbientEntity -> {
                ambientColor.value
            }

            else -> {
                otherColor.value
            }
        }
    }

    fun shouldDrawNametag(entity: Entity): Boolean {
        return shouldDrawNametag(entity::class.java, FriendManager.isFriend(entity.uuid))
    }

    fun shouldDrawNametag(entityClass: Class<out Entity>, isFriend: Boolean = false): Boolean {
        return when {
            PlayerEntity::class.java.isAssignableFrom(entityClass) -> {
                if (isFriend) return nametagsFriends.value
                nametagsPlayers.value
            }

            HostileEntity::class.java.isAssignableFrom(entityClass) -> nametagsMonsters.value
            FishEntity::class.java.isAssignableFrom(entityClass) -> nametagsFishes.value
            PassiveEntity::class.java.isAssignableFrom(entityClass) -> nametagsAnimals.value
            AmbientEntity::class.java.isAssignableFrom(entityClass) -> nametagsAmbient.value
            ItemEntity::class.java.isAssignableFrom(entityClass) -> nametagsItems.value
            else -> nametagsOther.value
        }
    }

    fun shouldDrawNametag(state: EntityRenderState): Boolean {
        if (state is PlayerEntityRenderState) {
            // this is not an uuid because this was easier to implement
            //  in a way that allows the mixin to also check with this
            val isFriend = FriendManager.isFriend(state.playerName?.string ?: "")
            return shouldDrawNametag(PlayerEntity::class.java, isFriend)
        }

        if (state is ItemEntityRenderState) {
            return nametagsItems.value
        }

        val spawnGroup = state.entityType?.spawnGroup
        return when (spawnGroup) {
            SpawnGroup.MONSTER -> nametagsMonsters.value
            SpawnGroup.CREATURE,
            SpawnGroup.AXOLOTLS -> nametagsAnimals.value
            SpawnGroup.AMBIENT -> nametagsAmbient.value
            SpawnGroup.WATER_CREATURE,
            SpawnGroup.WATER_AMBIENT,
            SpawnGroup.UNDERGROUND_WATER_CREATURE -> nametagsFishes.value
            else -> nametagsOther.value
        }
    }

    private fun Color.toImGui(): Int {
        return ImGui.getColorU32(this.red / 255f, this.green / 255f, this.blue / 255f, this.alpha / 255f)
    }

    init {
        listen<GuiEvent.NewFrame> { _ ->
            ImGui.pushStyleColor(ImGuiCol.Border, 0xFF00FFFF.toInt())
            ImGui.pushStyleColor(ImGuiCol.Text, 0xFF00FFFF.toInt())
            ImGui.pushStyleColor(ImGuiCol.FrameBg, 0xFF00FFFF.toInt())
            ImGui.pushStyleVar(ImGuiStyleVar.Alpha, 1f)
            ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, 3f, 0f)
            ImGui.pushStyleVar(ImGuiStyleVar.ItemSpacing, 0f, 0f)
            ImGui.pushStyleVar(ImGuiStyleVar.WindowMinSize, 10f, 10f)

            boxes.clear()
            entitySearch<Entity>(rangeSetting.value) {
                return@entitySearch when (it) {
                    mc.player -> return@entitySearch false
                    is PlayerEntity -> {
                        if (FriendManager.isFriend(it.uuid)) return@entitySearch enableFriends.value
                        return@entitySearch enablePlayers.value
                    }

                    is HostileEntity -> return@entitySearch enableMonsters.value
                    is PassiveEntity -> return@entitySearch enableAnimals.value
                    is WaterCreatureEntity -> return@entitySearch enableFish.value
                    is AmbientEntity -> return@entitySearch enableAmbient.value
                    is ItemEntity -> return@entitySearch enableItems.value
                    else -> return@entitySearch enableOther.value
                }
            }.forEach { entity ->
                val distance = mc.cameraEntity?.distanceTo(entity)?.toDouble() ?: 100.0
                val color = getColorForEntity(entity).toImGui()

                when (mode.value) {
                    Modes.Basic -> {
                        worldToScreen(
                            entity.pos,
                            RenderMain.modelViewMatrix,
                            RenderMain.projectionMatrix,
                            Lambda.mc.tickDelta
                        )?.let { coordinate ->
                            val sizeVal = 100 / (clamp(
                                (mc.player?.distanceTo(entity) ?: 100.0).toDouble(),
                                scaleMinimum.value,
                                scaleMaximum.value
                            ) / scaleSetting.value.toFloat()).toFloat()

                            val name = entity.name.string
                            val textWidth = ImGui.calcTextSizeX(name)
                            val textHeight = ImGui.calcTextSizeY(name)

                            var healthText = ""
                            var healthWidth = 0f
                            var healthColor = 0
                            val hasHealth = entity is LivingEntity
                            val healthNormalized = if (hasHealth) entity.health / entity.maxHealth else 1f
                            if (hasHealth) {
                                healthText = " ${entity.health}"
                                healthWidth = ImGui.calcTextSizeX(healthText)
                                healthColor = ImGui.getColorU32(1f - healthNormalized, healthNormalized, 0f, 1f)
                            }

                            val nameColor =
                                if (entity is LivingEntity && entity.isSneaking) ImGui.getColorU32(1f, 0f, 0f, 1f)
                                else ImGui.getColorU32(1f, 1f, 1f, 1f)

                            boxes.add(
                                BoxDrawDataContainer(
                                    coordinate,
                                    Vector2f(sizeVal, sizeVal),
                                    color = color,
                                    name = name,
                                    nameColor = nameColor,
                                    distance = distance,
                                    health = if (hasHealth) entity.health else 10f,
                                    maxHealth = if (hasHealth) entity.maxHealth else 10f,
                                    hasHealth = hasHealth,
                                    healthText = healthText,
                                    healthColor = healthColor,
                                    textWidth = textWidth,
                                    textHeight = textHeight,
                                    healthWidth = healthWidth,
                                    healthNormalized = healthNormalized
                                )
                            )
                        }
                    }

                    Modes.Hitbox -> {
                        val lastrenderX = if (entity.lastRenderX == 0.0) entity.x else entity.lastRenderX
                        val lastrenderY = if (entity.lastRenderY == 0.0) entity.y else entity.lastRenderY
                        val lastrenderZ = if (entity.lastRenderZ == 0.0) entity.z else entity.lastRenderZ

                        val box = entity.boundingBox.offset(
                            -entity.x,
                            -entity.y,
                            -entity.z
                        ).offset(
                            lastrenderX + (entity.x - lastrenderX) * mc.tickDelta,
                            lastrenderY + (entity.y - lastrenderY) * mc.tickDelta,
                            lastrenderZ + (entity.z - lastrenderZ) * mc.tickDelta
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
                            
                            val name = if (shouldDrawNametag(entity)) entity.name.string else null
                            val textWidth = if (name != null) ImGui.calcTextSizeX(name) else 0f
                            val textHeight = if (name != null) ImGui.calcTextSizeY(name) else 0f

                            var healthText = ""
                            var healthWidth = 0f
                            var healthColor = 0
                            val hasHealth = entity is LivingEntity
                            val healthNormalized = if (hasHealth) entity.health / entity.maxHealth else 1f
                            if (hasHealth) {
                                healthText = " ${entity.health}"
                                healthWidth = ImGui.calcTextSizeX(healthText)
                                healthColor = ImGui.getColorU32(1f - healthNormalized, healthNormalized, 0f, 1f)
                            }

                            val nameColor =
                                if (entity is LivingEntity && entity.isSneaking) ImGui.getColorU32(1f, 0f, 0f, 1f)
                                else ImGui.getColorU32(1f, 1f, 1f, 1f)

                            boxes.add(
                                BoxDrawDataContainer(
                                    Vector2f(minX + (maxX - minX) / 2f, minY + (maxY - minY) / 2f),
                                    Vector2f(w, h),
                                    color = color,
                                    name = name,
                                    nameColor = nameColor,
                                    distance = distance,
                                    health = if (hasHealth) entity.health else 10f,
                                    maxHealth = if (hasHealth) entity.maxHealth else 10f,
                                    hasHealth = hasHealth,
                                    healthText = healthText,
                                    healthColor = healthColor,
                                    textWidth = textWidth,
                                    textHeight = textHeight,
                                    healthWidth = healthWidth,
                                    healthNormalized = healthNormalized
                                )
                            )
                        }
                    }
                }
            }

            boxes.sortByDescending { it.distance }

            boxes.forEach { drawTask ->
                val minX = drawTask.position.x - drawTask.size.x / 2
                val minY = drawTask.position.y - drawTask.size.y / 2
                val maxX = drawTask.position.x + drawTask.size.x / 2
                val maxY = drawTask.position.y + drawTask.size.y / 2

                if (drawTask.name != null) {
                    val textY = max(minY - drawTask.textHeight - 10, 0f)
                    val textMaxX = minX + drawTask.textWidth + drawTask.healthWidth

                    ImGui.getBackgroundDrawList()
                        .addRectFilled(
                            minX,
                            textY,
                            textMaxX,
                            textY + drawTask.textHeight,
                            ImGui.getColorU32(0f, 0f, 0f, 0.5f)
                        )
                    ImGui.getBackgroundDrawList()
                        .addText(
                            minX,
                            textY,
                            drawTask.nameColor,
                            drawTask.name
                        )
                    if (drawTask.hasHealth) {
                        ImGui.getBackgroundDrawList()
                            .addText(
                                minX + drawTask.textWidth,
                                textY,
                                drawTask.healthColor,
                                drawTask.healthText
                            )
                    }
                }

                ImGui.getBackgroundDrawList()
                    .addRect(
                        minX,
                        minY,
                        maxX,
                        maxY,
                        drawTask.color,
                        0f, // todo: rounding
                        ImDrawFlags.None,
                        borderThickness.value
                    )
            }

            ImGui.popStyleVar(4)
            ImGui.popStyleColor(3)
        }
    }
}
