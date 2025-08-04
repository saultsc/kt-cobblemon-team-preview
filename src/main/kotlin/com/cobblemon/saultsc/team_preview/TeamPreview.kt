package com.cobblemon.saultsc.team_preview

import com.cobblemon.saultsc.team_preview.network.battle.s2c.BattlePreviewPacket
import com.cobblemon.saultsc.team_preview.network.battle.s2c.BattleTimerUpdatePacket
import com.cobblemon.saultsc.team_preview.network.battle.s2c.PokemonSelectionPacket
import com.cobblemon.saultsc.team_preview.server.BattlePreviewManager
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.util.Identifier

class TeamPreview : ModInitializer {
    companion object {
        const val MODID = "team_preview"
        val TEAM_PREVIEW_PACKET_ID: Identifier = Identifier.of(MODID, "team_preview_packet")
    }

    override fun onInitialize() {
        // Registrar packets
        PayloadTypeRegistry.playS2C().register(BattlePreviewPacket.ID, BattlePreviewPacket.CODEC)
        PayloadTypeRegistry.playS2C().register(BattleTimerUpdatePacket.ID, BattleTimerUpdatePacket.CODEC)
        PayloadTypeRegistry.playC2S().register(PokemonSelectionPacket.ID, PokemonSelectionPacket.CODEC)

        // Registrar manejador para selección de pokémon en el servidor
        ServerPlayNetworking.registerGlobalReceiver(PokemonSelectionPacket.ID) { packet, context ->
            context.server().execute {
                BattlePreviewManager.INSTANCE.handlePokemonSelection(
                    packet.battleId,
                    context.player(),
                    packet.selectedPokemonIndex
                )
            }
        }
    }
}