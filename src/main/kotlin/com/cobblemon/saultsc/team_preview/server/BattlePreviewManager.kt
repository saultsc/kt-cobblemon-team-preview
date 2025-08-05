package com.cobblemon.saultsc.team_preview.server

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.battles.BattleBuilder
import com.cobblemon.mod.common.battles.BattleFormat
import com.cobblemon.mod.common.battles.BattleRegistry
import com.cobblemon.mod.common.battles.BattleStartResult
import com.cobblemon.saultsc.team_preview.network.battle.s2c.BattleTimerUpdatePacket
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.server.network.ServerPlayerEntity
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class BattlePreviewManager {
    companion object {
        val INSTANCE = BattlePreviewManager()

        const val SELECTION_TIME_LIMIT = 30
        const val PRE_START_TIME_LIMIT = 5
    }

    private val scheduler: ScheduledExecutorService = Executors.newScheduledThreadPool(2)
    private val battleSessions = ConcurrentHashMap<UUID, BattleSession>()

    data class BattleSession(
        val battleId: UUID,
        val player1: ServerPlayerEntity,
        val player2: ServerPlayerEntity,
        val battleFormat: BattleFormat,
        var player1Selection: UUID? = null,
        var player2Selection: UUID? = null,
        var selectionTimeRemaining: Int = SELECTION_TIME_LIMIT,
        var preStartTimeRemaining: Int = PRE_START_TIME_LIMIT,
        var phase: BattleTimerUpdatePacket.TimerPhase = BattleTimerUpdatePacket.TimerPhase.SELECTION,
        var isActive: Boolean = true,
        var selectionTimerTask: ScheduledFuture<*>? = null,
        var preStartTimerTask: ScheduledFuture<*>? = null
    )

    fun startBattlePreview(battleId: UUID, player1: ServerPlayerEntity, player2: ServerPlayerEntity, battleFormat: BattleFormat) {
        val session = BattleSession(battleId, player1, player2, battleFormat)
        battleSessions[battleId] = session

        startSelectionTimer(session)
    }

    private fun startSelectionTimer(session: BattleSession) {
        session.selectionTimerTask = scheduler.scheduleAtFixedRate({
            if (!session.isActive) {
                session.selectionTimerTask?.cancel(false)
                return@scheduleAtFixedRate
            }

            session.selectionTimeRemaining--

            sendTimerUpdate(session)

            if (session.player1Selection != null && session.player2Selection != null) {
                session.selectionTimerTask?.cancel(false)
                session.phase = BattleTimerUpdatePacket.TimerPhase.PRE_START
                startPreStartTimer(session)
                return@scheduleAtFixedRate
            }

            if (session.selectionTimeRemaining <= 0) {
                session.selectionTimerTask?.cancel(false)
                handleSelectionTimeout(session)
                return@scheduleAtFixedRate
            }

        }, 1, 1, TimeUnit.SECONDS)
    }

    private fun startPreStartTimer(session: BattleSession) {
        session.preStartTimerTask = scheduler.scheduleAtFixedRate({
            if (!session.isActive) {
                session.preStartTimerTask?.cancel(false)
                return@scheduleAtFixedRate
            }

            session.preStartTimeRemaining--

            sendTimerUpdate(session)

            if (session.preStartTimeRemaining <= 0) {
                session.preStartTimerTask?.cancel(false)
                startBattle(session)
                return@scheduleAtFixedRate
            }

        }, 1, 1, TimeUnit.SECONDS)
    }

    private fun sendTimerUpdate(session: BattleSession) {
        val packet = BattleTimerUpdatePacket(
            session.battleId,
            session.selectionTimeRemaining,
            session.preStartTimeRemaining,
            session.phase
        )

        ServerPlayNetworking.send(session.player1, packet)
        ServerPlayNetworking.send(session.player2, packet)
    }

    fun handlePokemonSelection(battleId: UUID, player: ServerPlayerEntity, selectedIndex: Int) {
        val session = battleSessions[battleId] ?: return

        if (session.phase != BattleTimerUpdatePacket.TimerPhase.SELECTION) return

        when (player) {
            session.player1 -> {
                val pokemon = Cobblemon.storage.getParty(player).get(selectedIndex)
                session.player1Selection = pokemon?.uuid
            }
            session.player2 -> {
                val pokemon = Cobblemon.storage.getParty(player).get(selectedIndex)
                session.player2Selection = pokemon?.uuid
            }
        }

        if (session.player1Selection != null && session.player2Selection != null) {
            session.selectionTimerTask?.cancel(true)

            session.phase = BattleTimerUpdatePacket.TimerPhase.PRE_START
            session.preStartTimeRemaining = PRE_START_TIME_LIMIT

            sendTimerUpdate(session)

            startPreStartTimer(session)
        }
    }

    private fun handleSelectionTimeout(session: BattleSession) {
        if (session.player1Selection == null) {
            session.player1Selection = Cobblemon.storage.getParty(session.player1).get(0)?.uuid
        }
        if (session.player2Selection == null) {
            session.player2Selection = Cobblemon.storage.getParty(session.player2).get(0)?.uuid
        }

        session.phase = BattleTimerUpdatePacket.TimerPhase.PRE_START
        startPreStartTimer(session)
    }

    private fun startBattle(session: BattleSession) {
        session.phase = BattleTimerUpdatePacket.TimerPhase.FINISHED
        session.isActive = false
        sendTimerUpdate(session)

        session.selectionTimerTask?.cancel(false)
        session.preStartTimerTask?.cancel(false)

        val player1Pokemon = session.player1Selection?.let { Cobblemon.storage.getParty(session.player1).find { p -> p.uuid == it } }
        val player2Pokemon = session.player2Selection?.let { Cobblemon.storage.getParty(session.player2).find { p -> p.uuid == it } }

        if (player1Pokemon != null && player2Pokemon != null) {
            BattleBuilder.pvp1v1(
                player1 = session.player1,
                player2 = session.player2,
                leadingPokemonPlayer1 = session.player1Selection,
                leadingPokemonPlayer2 = session.player2Selection,
                battleFormat = session.battleFormat
            )
        }

        battleSessions.remove(session.battleId)
    }

    fun cancelBattle(battleId: UUID) {
        val session = battleSessions[battleId]
        if (session != null) {
            session.isActive = false
            session.selectionTimerTask?.cancel(false)
            session.preStartTimerTask?.cancel(false)
            battleSessions.remove(battleId)
        }
    }

    fun getPlayerSelection(battleId: UUID, player: ServerPlayerEntity): UUID? {
        val session = battleSessions[battleId] ?: return null
        return when (player) {
            session.player1 -> session.player1Selection
            session.player2 -> session.player2Selection
            else -> null
        }
    }

    fun shutdown() {
        scheduler.shutdown()
        battleSessions.clear()
    }
}
