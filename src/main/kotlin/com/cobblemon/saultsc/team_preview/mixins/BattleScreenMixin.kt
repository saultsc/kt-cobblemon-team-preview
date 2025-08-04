package com.cobblemon.saultsc.team_preview.mixins

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.battles.ChallengeManager
import com.cobblemon.mod.common.battles.ShowdownPokemon
import com.cobblemon.saultsc.team_preview.network.battle.s2c.BattlePreviewPacket
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(ChallengeManager::class, remap = false)
class BattleScreenMixin {

  @Inject(method = ["onAccept"], at = [At("HEAD")], cancellable = true)
  private fun onAcceptInject(challenge: ChallengeManager.BattleChallenge, ci: CallbackInfo) {
    // Cancelamos el evento original para manejarlo nosotros.
    ci.cancel()

    // Obtenemos los jugadores del desafío. Esto se ejecuta en el servidor.
    val sender = challenge.sender
    val receiver = challenge.receiver

    // Creamos la lista de Pokémon para el equipo del remitente.
    val senderTeam = Cobblemon.storage.getParty(sender).map { pokemon ->
      val condition = "${pokemon.currentHealth}/${pokemon.maxHealth}" + if (pokemon.isFainted()) " fnt" else ""
      ShowdownPokemon().apply {
        this.condition = condition
        this.pokeball = pokemon.caughtBall.name.toString()
      } to pokemon
    }

    // Creamos la lista de Pokémon para el equipo del receptor.
    val receiverTeam = Cobblemon.storage.getParty(receiver).map { pokemon ->
      val condition = "${pokemon.currentHealth}/${pokemon.maxHealth}" + if (pokemon.isFainted()) " fnt" else ""
      ShowdownPokemon().apply {
        this.condition = condition
        this.pokeball = pokemon.caughtBall.name.toString()
      } to pokemon
    }

    // Enviamos el paquete al remitente con su equipo y el del oponente.
    ServerPlayNetworking.send(sender, BattlePreviewPacket(playerTeam = senderTeam, opponentTeam = receiverTeam))
    // Enviamos el paquete al receptor con su equipo y el del oponente.
    ServerPlayNetworking.send(receiver, BattlePreviewPacket(playerTeam = receiverTeam, opponentTeam = senderTeam))
  }
}