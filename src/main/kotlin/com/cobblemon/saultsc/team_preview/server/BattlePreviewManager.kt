package com.cobblemon.saultsc.team_preview.server

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
        var player1Selection: Int? = null,
        var player2Selection: Int? = null,
        var selectionTimeRemaining: Int = SELECTION_TIME_LIMIT,
        var preStartTimeRemaining: Int = PRE_START_TIME_LIMIT,
        var phase: BattleTimerUpdatePacket.TimerPhase = BattleTimerUpdatePacket.TimerPhase.SELECTION,
        var isActive: Boolean = true,
        var selectionTimerTask: ScheduledFuture<*>? = null,
        var preStartTimerTask: ScheduledFuture<*>? = null
    )

    fun startBattlePreview(battleId: UUID, player1: ServerPlayerEntity, player2: ServerPlayerEntity) {
        val session = BattleSession(battleId, player1, player2)
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
            session.player1 -> session.player1Selection = selectedIndex
            session.player2 -> session.player2Selection = selectedIndex
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
            session.player1Selection = 0
        }
        if (session.player2Selection == null) {
            session.player2Selection = 0
        }

        session.phase = BattleTimerUpdatePacket.TimerPhase.PRE_START
        startPreStartTimer(session)
    }

    private fun startBattle(session: BattleSession) {
        session.phase = BattleTimerUpdatePacket.TimerPhase.FINISHED
        session.isActive = false
        sendTimerUpdate(session)

        // Cancelar cualquier timer que aún esté corriendo
        session.selectionTimerTask?.cancel(false)
        session.preStartTimerTask?.cancel(false)

        // Aquí integrarías con el sistema de batalla de Cobblemon
        // Pasando las selecciones de pokémon: session.player1Selection y session.player2Selection

        // Ejemplo de cómo podrías iniciar la batalla con los pokémon seleccionados:
        /*
        val battle = BattleBuilder()
            .leadPokemon(session.player1, session.player1Selection ?: 0)
            .leadPokemon(session.player2, session.player2Selection ?: 0)
            .start()
        */

        // Limpiar sesión
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

    fun getPlayerSelection(battleId: UUID, player: ServerPlayerEntity): Int? {
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
