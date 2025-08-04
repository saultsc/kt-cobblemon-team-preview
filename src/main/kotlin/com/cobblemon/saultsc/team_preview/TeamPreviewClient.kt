package com.cobblemon.saultsc.team_preview

import com.cobblemon.saultsc.team_preview.client.gui.battle.BattlePreview
import com.cobblemon.saultsc.team_preview.network.battle.s2c.BattlePreviewPacket
import com.cobblemon.saultsc.team_preview.network.battle.s2c.BattleTimerUpdatePacket
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking

class TeamPreviewClient : ClientModInitializer {
  override fun onInitializeClient() {
    ClientPlayNetworking.registerGlobalReceiver(BattlePreviewPacket.ID) { payload, context ->
      context.client().execute {
        val battlePreview = BattlePreview(
          payload.battleId,
          payload.opponentTeam,
          payload.opponentName,
          payload.playerTeam,
          payload.playerName
        )
        context.client().setScreen(battlePreview)
      }
    }

    ClientPlayNetworking.registerGlobalReceiver(BattleTimerUpdatePacket.ID) { payload, context ->
      context.client().execute {
        val currentScreen = context.client().currentScreen
        if (currentScreen is BattlePreview) {
          currentScreen.updateTimer(payload)
        }
      }
    }
  }
}
