package com.example.modules

import com.lambda.event.events.TickEvent
import com.lambda.event.listener.SafeListener.Companion.listen
import com.lambda.event.listener.SafeListener.Companion.listenOnce
import com.lambda.interaction.material.StackSelection.Companion.selectStack
import com.lambda.interaction.request.inventory.InventoryRequest.Companion.inventoryRequest
import com.lambda.module.Module
import com.lambda.module.tag.ModuleTag
import com.lambda.util.item.ItemStackUtils.inventoryIndex
import com.lambda.util.item.ItemStackUtils.slotId
import com.lambda.util.player.SlotUtils.hotbarAndStorage
import net.minecraft.item.Items

object InventoryExample : Module(
    name = "InventoryExample",
    description = "Example module that shows how to do inventory transactions",
    tag = ModuleTag.CLIENT,
) {
    init {
        listen<TickEvent.Pre> {
            val selector = selectStack { isItem(Items.AIR).not() }
            val stacks = selector.filterStacks(player.hotbarAndStorage)

            val first = stacks.getOrNull(0)?.slotId ?: return@listen
            val second = stacks.getOrNull(1)?.slotId ?: return@listen

            inventoryRequest {
                throwStack(first)
                throwStack(second)
            }.submit(true)
        }
    }
}