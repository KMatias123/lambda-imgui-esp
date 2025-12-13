package com.example.modules

import com.lambda.event.events.TickEvent
import com.lambda.event.listener.SafeListener.Companion.listen
import com.lambda.module.Module
import com.lambda.module.tag.ModuleTag
import com.lambda.util.world.entitySearch
import net.minecraft.entity.Entity

object Range : Module(
    name = "Range",
    description = "Print all the entities in a range",
    tag = ModuleTag.PLAYER,
) {
    private val range by setting("Range", 10.0, 1.0..32.0, 1.0)

    private val iterator: (Entity) -> Unit = { entity ->
        // This property was accessed using the Access Wideners
        // How to access other properties?
        //
        // Intellij:
        // - Go to the definition of the property
        // - Right-click on the property
        // - Click on "Copy / Paste Special"
        // - Click on "AW Reference"
        // - Paste the reference in the access widener file
        // - Click the refresh arrows in the Gradle tool window
        val onFire = entity.hasVisualFire
        println("Entity: ${entity.displayName} | On Fire?: $onFire")
    }

    init {
        listen<TickEvent.Pre> {
            entitySearch<Entity>(range)
                .forEach {
                    iterator(it)
                }
        }
    }
}
