package com.cobblemon.saultsc.team_preview.server

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor
import com.cobblemon.mod.common.api.events.battles.BattleEvent
import com.cobblemon.mod.common.battles.BattleBuilder
import com.cobblemon.mod.common.battles.BattleFormat
import com.cobblemon.mod.common.battles.BattleRegistry
import com.cobblemon.mod.common.battles.BattleSide
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor
import com.cobblemon.saultsc.team_preview.network.battle.s2c.BattleTimerUpdatePacket
import com.cobblemon.saultsc.team_preview.server.BattleMoveTimerManager
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
    var preStartTimerTask: ScheduledFuture<*>? = null,
    var winner: ServerPlayerEntity? = null,
  )

  fun startBattlePreview(
    battleId: UUID,
    player1: ServerPlayerEntity,
    player2: ServerPlayerEntity,
    battleFormat: BattleFormat,
  ) {
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
    val p1Selection = session.player1Selection
    val p2Selection = session.player2Selection

    if (p1Selection != null && p2Selection == null) {
      session.winner = session.player1
      endBattle(session)
    } else if (p1Selection == null && p2Selection != null) {
      session.winner = session.player2
      endBattle(session)
    } else {
      cancelBattle(session.battleId)
    }
  }

  private fun startBattle(session: BattleSession) {
    val (actor1, actor2) = listOf(
      session.player1 to session.player1Selection,
      session.player2 to session.player2Selection
    ).map { (player, selected) ->
      PlayerBattleActor(
        player.uuid,
        Cobblemon.storage.getParty(player)
          .toBattleTeam()
          .sortedBy { it.uuid != selected }
          .toMutableList()
      )
    }

    BattleRegistry.startBattle(
      session.battleFormat,
      BattleSide(actor1),
      BattleSide(actor2)
    )

    // Start move timers for the battle
    BattleMoveTimerManager.INSTANCE.startBattleMoveTimer(
      session.battleId,
      session.player1,
      session.player2
    )

    endBattle(session)
  }

  fun cancelBattle(battleId: UUID) {
    val session = battleSessions[battleId]
    if (session != null) {
      endBattle(session)
    }
  }

  private fun endBattle(session: BattleSession) {
    session.isActive = false
    session.phase = BattleTimerUpdatePacket.TimerPhase.FINISHED
    sendTimerUpdate(session)
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
