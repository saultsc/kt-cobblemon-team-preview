package saultsc.battle_showdown.mixin;

import com.cobblemon.mod.common.battles.BattleTypes;
import com.cobblemon.mod.common.battles.ChallengeManager;
import com.cobblemon.mod.common.battles.ShowdownPokemon;
import com.cobblemon.mod.common.net.serverhandling.ChallengeHandler;
import com.cobblemon.mod.common.pokemon.Pokemon;
import kotlin.Pair;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import saultsc.battle_showdown.BattleShowdown;
import saultsc.battle_showdown.util.BattleUtils;

import java.util.List;


@Mixin(value = ChallengeManager.class, remap = false)
public class BattleAcceptMixin {

    @Inject(method = "onAccept(Lcom/cobblemon/mod/common/battles/ChallengeManager$BattleChallenge;)V", at = @At("HEAD"), cancellable = true)
    private void onAccept(ChallengeManager.BattleChallenge challenge, @NotNull CallbackInfo ci) {
        if (!BattleUtils.INSTANCE.getNOT_VALID_BATTLE_TYPES().contains(challenge.getBattleFormat().getBattleType().getName()))
            return;
        ci.cancel();

        List<Pair<ShowdownPokemon, Pokemon>> senderTeam = BattleUtils.INSTANCE.getBattleTeam(challenge, challenge.getSender());
        List<Pair<ShowdownPokemon, Pokemon>> receiverTeam = BattleUtils.INSTANCE.getBattleTeam(challenge, challenge.getReceiver());


    }
}
