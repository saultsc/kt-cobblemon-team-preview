package com.cobblemon.saultsc.team_preview

import com.cobblemon.saultsc.team_preview.client.gui.battle.BattlePreview
import com.cobblemon.saultsc.team_preview.network.battle.BattlePreviewPacket
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking

class TeamPreviewClient : ClientModInitializer {
    override fun onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(BattlePreviewPacket.ID) { payload, context ->
            context.client().execute {
                context.client().setScreen(BattlePreview(payload.opponentTeam, payload.playerTeam))
            }
        }
    }
}
