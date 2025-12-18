package dev.kmatias.lambdaimguiesp

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.loader.api.FabricLoader
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

object LambdaImGuiESPClient : ClientModInitializer {
    const val MOD_NAME = "ImguiESP"
    const val MOD_ID = "lambda-imgui-esp"
    const val SYMBOL = "imgui-esp"

    val VERSION: String = FabricLoader.getInstance()
        .getModContainer(MOD_ID).orElseThrow()
        .metadata.version.friendlyString

    val LOG: Logger = LogManager.getLogger(SYMBOL)

    // You do not need to register modules or commands as the mod depends on lambda, they will loaded
    // into the class path and Lambda will use ClassGraph to scan the class path and load the classes.
    // See com/lambda/util/reflections/Reflections.kt:39
    // Realistically, you could even make an addon that isn't even a minecraft mod.
    override fun onInitializeClient() = LOG.info("Plugin $MOD_NAME $VERSION initialized.")
}
