package com.cobblemon.saultsc.team_preview

import com.cobblemon.saultsc.team_preview.network.battle.s2c.BattlePreviewPacket
import com.cobblemon.saultsc.team_preview.network.battle.s2c.BattleTimerUpdatePacket
import com.cobblemon.saultsc.team_preview.network.battle.s2c.MoveTimerUpdatePacket
import com.cobblemon.saultsc.team_preview.network.battle.s2c.PokemonSelectionPacket
import com.cobblemon.saultsc.team_preview.server.BattlePreviewManager
import com.cobblemon.saultsc.team_preview.server.BattleMoveTimerManager
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking

class TeamPreview : ModInitializer {
    companion object {
        const val MODID = "team_preview"
    }

    override fun onInitialize() {
        // Registrar packets
        PayloadTypeRegistry.playS2C().register(BattlePreviewPacket.ID, BattlePreviewPacket.CODEC)
        PayloadTypeRegistry.playS2C().register(BattleTimerUpdatePacket.ID, BattleTimerUpdatePacket.CODEC)
        PayloadTypeRegistry.playS2C().register(MoveTimerUpdatePacket.ID, MoveTimerUpdatePacket.CODEC)
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