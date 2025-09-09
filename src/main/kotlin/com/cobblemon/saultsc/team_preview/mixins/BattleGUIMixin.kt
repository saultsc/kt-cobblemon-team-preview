package com.cobblemon.saultsc.team_preview.mixins

import com.cobblemon.mod.common.client.gui.battle.BattleGUI
import com.cobblemon.saultsc.team_preview.client.gui.battle.MoveTimerOverlay
import net.minecraft.client.gui.DrawContext
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(BattleGUI::class, remap = false)
class BattleGUIMixin {

    @Inject(method = ["render"], at = [At("TAIL")])
    private fun onRender(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float, ci: CallbackInfo) {
        // Get screen dimensions from Minecraft client instead of shadowed fields
        val client = net.minecraft.client.MinecraftClient.getInstance()
        val screenWidth = client.window.scaledWidth
        val screenHeight = client.window.scaledHeight
        
        // Render the move timer overlay on top of the battle GUI
        MoveTimerOverlay.INSTANCE.render(context, screenWidth, screenHeight)
    }
}