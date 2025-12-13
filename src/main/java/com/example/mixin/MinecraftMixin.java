package com.example.mixin;

import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public class MinecraftMixin {
    @Inject(method = "getWindowTitle()Ljava/lang/String;", at = @At("RETURN"), cancellable = true)
    private void windowTitle(CallbackInfoReturnable<String> cir) {
        cir.setReturnValue("Minecraft - SoupCan");
    }
}
