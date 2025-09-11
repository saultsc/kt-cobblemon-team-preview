package saultsc.battle_showdown.util

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.battles.BattleTypes
import com.cobblemon.mod.common.battles.ChallengeManager.BattleChallenge
import com.cobblemon.mod.common.battles.ShowdownPokemon
import com.cobblemon.mod.common.pokemon.Pokemon
import net.minecraft.server.network.ServerPlayerEntity


object BattleUtils {
  val NOT_VALID_BATTLE_TYPES: List<String> = listOf(
    BattleTypes.SINGLES.name,
//    BattleTypes.MULTI.name,
//    BattleTypes.ROYAL.name
  )

  fun getBattleTeam(
    challenge: BattleChallenge,
    player: ServerPlayerEntity
  ): List<Pair<ShowdownPokemon, Pokemon>> {
    return Cobblemon.storage.getParty(player).map { pokemon ->
      val battlePokemon = pokemon.clone()
      if (challenge.battleFormat.adjustLevel > 1) battlePokemon.level = challenge.battleFormat.adjustLevel
      if (battlePokemon.canBeHealed()) battlePokemon.heal()

      val condition =
        "${battlePokemon.currentHealth}/${battlePokemon.maxHealth}" + if (battlePokemon.isFainted()) " fnt" else ""

      ShowdownPokemon().apply {
        this.condition = condition
        this.pokeball = battlePokemon.caughtBall.name.toString()
      } to battlePokemon
    }
  }
}