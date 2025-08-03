package com.cobblemon.saultsc.team_preview.mixins

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.battles.ChallengeManager
import com.cobblemon.mod.common.battles.ShowdownPokemon
import com.cobblemon.saultsc.team_preview.client.gui.battle.TeamPreviewScreen
import net.minecraft.client.MinecraftClient
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(ChallengeManager::class, remap = false)
class BattleScreenMixin {

  @Inject(method = ["onAccept"], at = [At("HEAD")], cancellable = true)
  fun onAcceptInject(challenge: ChallengeManager.BattleChallenge, ci: CallbackInfo) {
    println("Intercepted onAccept: $challenge")
    ci.cancel()

    val opponentTeam =
      Cobblemon.storage.getParty(challenge.sender).map { pokemon ->
        val condition =
          "${pokemon.currentHealth}/${pokemon.maxHealth}" +
              if (pokemon.isFainted()) " fnt" else ""

        val showdown = ShowdownPokemon().apply {
          this.condition = condition
          this.pokeball = pokemon.caughtBall.name.namespace
        }

        showdown to pokemon
      }

    MinecraftClient.getInstance().execute {
      MinecraftClient.getInstance().setScreen(TeamPreviewScreen(opponentTeam))
    }
  }
}
