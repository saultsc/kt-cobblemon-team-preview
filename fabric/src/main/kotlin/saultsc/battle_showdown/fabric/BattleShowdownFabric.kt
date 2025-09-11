package saultsc.battle_showdown.fabric

import net.fabricmc.api.ModInitializer
import saultsc.battle_showdown.BattleShowdown

/**
 * Fabric entrypoint.
 */
object BattleShowdownFabric : ModInitializer {

  override fun onInitialize() {
    BattleShowdown.init()
  }
}
