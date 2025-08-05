package com.cobblemon.saultsc.team_preview.mixins

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.battles.ChallengeManager
import com.cobblemon.mod.common.battles.ShowdownPokemon
import com.cobblemon.saultsc.team_preview.network.battle.s2c.BattlePreviewPacket
import com.cobblemon.saultsc.team_preview.server.BattlePreviewManager
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import java.util.*

@Mixin(ChallengeManager::class, remap = false)
class BattleScreenMixin {

    @Inject(method = ["onAccept"], at = [At("HEAD")], cancellable = true)
    private fun onAcceptInject(challenge: ChallengeManager.BattleChallenge, ci: CallbackInfo) {

        if (challenge !is ChallengeManager.SinglesBattleChallenge) return

        ci.cancel()

        val sender = challenge.sender
        val receiver = challenge.receiver

        val battleId = UUID.randomUUID()

        BattlePreviewManager.INSTANCE.startBattlePreview(battleId, sender, receiver, challenge.battleFormat)

        val senderTeam = Cobblemon.storage.getParty(sender).map { pokemon ->
            val battlePokemon = pokemon.clone()
            if (challenge.battleFormat.adjustLevel > 1) battlePokemon.level = challenge.battleFormat.adjustLevel
            if(battlePokemon.canBeHealed()) battlePokemon.heal()

            val condition = "${battlePokemon.currentHealth}/${battlePokemon.maxHealth}" + if (battlePokemon.isFainted()) " fnt" else ""

            ShowdownPokemon().apply {
                this.condition = condition
                this.pokeball = battlePokemon.caughtBall.name.toString()
            } to battlePokemon
        }

        val receiverTeam = Cobblemon.storage.getParty(receiver).map { pokemon ->
            val battlePokemon = pokemon.clone()
            if (challenge.battleFormat.adjustLevel > 1) battlePokemon.level = challenge.battleFormat.adjustLevel
            if(battlePokemon.canBeHealed()) battlePokemon.heal()

            val condition = "${battlePokemon.currentHealth}/${battlePokemon.maxHealth}" + if (battlePokemon.isFainted()) " fnt" else ""

            ShowdownPokemon().apply {
                this.condition = condition
                this.pokeball = battlePokemon.caughtBall.name.toString()
            } to battlePokemon
        }

        ServerPlayNetworking.send(
            sender, BattlePreviewPacket(
                battleId = battleId,
                playerTeam = senderTeam,
                playerName = sender.name.string,
                opponentTeam = receiverTeam,
                opponentName = receiver.name.string
            )
        )

        ServerPlayNetworking.send(
            receiver, BattlePreviewPacket(
                battleId = battleId,
                playerTeam = receiverTeam,
                playerName = receiver.name.string,
                opponentTeam = senderTeam,
                opponentName = sender.name.string
            )
        )
    }
}