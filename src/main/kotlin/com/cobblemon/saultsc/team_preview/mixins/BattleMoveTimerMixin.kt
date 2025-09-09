package com.cobblemon.saultsc.team_preview.mixins

import com.cobblemon.mod.common.api.battles.model.PokemonBattle
import com.cobblemon.mod.common.battles.BattleRegistry
import com.cobblemon.saultsc.team_preview.server.BattleMoveTimerManager
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(BattleRegistry::class, remap = false)
class BattleMoveTimerMixin {

    @Inject(method = ["closeBattle"], at = [At("HEAD")])
    private fun onBattleEnd(battle: PokemonBattle, ci: CallbackInfo) {
        BattleMoveTimerManager.INSTANCE.endBattleTimer(battle.battleId)
    }
}