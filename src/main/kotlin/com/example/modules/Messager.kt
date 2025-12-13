package com.example.modules

import com.lambda.module.Module
import com.lambda.module.tag.ModuleTag
import com.lambda.util.Communication.info
import com.lambda.util.text.*
import java.awt.Color

object Messager : Module(
    name = "Messager",
    description = "A module that sends messages in the game",
    tag = ModuleTag.CLIENT,
) {
    init {
        onEnable {
            val text = buildText {
                literal("Here is a fun video!")
                clickEvent(ClickEvents.openUrl("https://youtu.be/h0UsrRaGMh4")) {
                    color(Color.GREEN) {
                        literal(" (Click me!)")
                    }
                }
            }

            info(text)
        }
    }
}
