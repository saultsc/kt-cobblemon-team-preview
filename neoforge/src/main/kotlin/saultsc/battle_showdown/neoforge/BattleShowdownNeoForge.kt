package saultsc.battle_showdown.neoforge

import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.fml.common.Mod
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent
import thedarkcolour.kotlinforforge.neoforge.forge.MOD_BUS
import thedarkcolour.kotlinforforge.neoforge.forge.runForDist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent
import net.neoforged.fml.event.lifecycle.FMLDedicatedServerSetupEvent
import saultsc.battle_showdown.BattleShowdown

/**
 * NeoForge entrypoint.
 */
@Mod(BattleShowdown.MODID)
@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
object BattleShowdownNeoForge {
    init {
      BattleShowdown.init()

        val obj = runForDist(
            clientTarget = {
                MOD_BUS.addListener(::onClientSetup)
            },
            serverTarget = {
                MOD_BUS.addListener(::onServerSetup)
            }
        )
    }

    private fun onClientSetup(event: FMLClientSetupEvent) {

    }

    private fun onServerSetup(event: FMLDedicatedServerSetupEvent) {
        //
    }

    @SubscribeEvent
    fun onCommonSetup(event: FMLCommonSetupEvent) {
    }
}