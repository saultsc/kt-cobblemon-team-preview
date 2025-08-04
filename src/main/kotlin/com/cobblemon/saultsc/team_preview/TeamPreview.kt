package com.cobblemon.saultsc.team_preview

import com.cobblemon.saultsc.team_preview.client.gui.battle.BattlePreview
import com.cobblemon.saultsc.team_preview.network.battle.s2c.BattlePreviewPacket
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.minecraft.util.Identifier

class TeamPreview : ModInitializer {
    companion object {
        const val MODID = "team_preview"
        val TEAM_PREVIEW_PACKET_ID: Identifier = Identifier.of(MODID, "team_preview_packet")
    }

    override fun onInitialize() {
        PayloadTypeRegistry.playS2C().register(BattlePreviewPacket.ID, BattlePreviewPacket.CODEC)
    }
}