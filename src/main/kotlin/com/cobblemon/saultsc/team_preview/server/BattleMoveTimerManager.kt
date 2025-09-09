package com.cobblemon.saultsc.team_preview.server

import com.cobblemon.mod.common.battles.BattleRegistry
import com.cobblemon.saultsc.team_preview.network.battle.s2c.MoveTimerUpdatePacket
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.server.network.ServerPlayerEntity
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class BattleMoveTimerManager {
    companion object {
        val INSTANCE = BattleMoveTimerManager()
        const val MOVE_TIME_LIMIT = 60 // 60 seconds per move
    }

    private val scheduler: ScheduledExecutorService = Executors.newScheduledThreadPool(4)
    private val battleTimers = ConcurrentHashMap<UUID, BattleMoveTimer>()

    data class BattleMoveTimer(
        val battleId: UUID,
        val player1: ServerPlayerEntity,
        val player2: ServerPlayerEntity,
        var player1TimeRemaining: Int = MOVE_TIME_LIMIT,
        var player2TimeRemaining: Int = MOVE_TIME_LIMIT,
        var currentTurnPlayer: ServerPlayerEntity? = null,
        var isActive: Boolean = true,
        var timerTask: ScheduledFuture<*>? = null
    )

    data class PlayerMoveTimer(
        val player: ServerPlayerEntity,
        var timeRemaining: Int
    )

    fun startBattleMoveTimer(battleId: UUID, player1: ServerPlayerEntity, player2: ServerPlayerEntity) {
        val timer = BattleMoveTimer(battleId, player1, player2)
        battleTimers[battleId] = timer
        
        // Start with player1's turn by default - this can be adjusted based on battle logic
        timer.currentTurnPlayer = player1
        startTimerForCurrentPlayer(timer)
    }

    // Simple alternating turn system - switches every 60 seconds
    private fun switchToNextPlayer(timer: BattleMoveTimer) {
        timer.currentTurnPlayer = when (timer.currentTurnPlayer) {
            timer.player1 -> timer.player2
            timer.player2 -> timer.player1
            else -> timer.player1 // Default to player1 if null
        }
        
        // Reset the current player's timer
        when (timer.currentTurnPlayer) {
            timer.player1 -> timer.player1TimeRemaining = MOVE_TIME_LIMIT
            timer.player2 -> timer.player2TimeRemaining = MOVE_TIME_LIMIT
        }
        
        // Start timer for the next player
        startTimerForCurrentPlayer(timer)
    }

    private fun startTimerForCurrentPlayer(timer: BattleMoveTimer) {
        if (!timer.isActive || timer.currentTurnPlayer == null) return
        
        sendTimerUpdate(timer)
        
        timer.timerTask = scheduler.scheduleAtFixedRate({
            if (!timer.isActive) {
                timer.timerTask?.cancel(false)
                return@scheduleAtFixedRate
            }
            
            // Decrement time for current player
            when (timer.currentTurnPlayer) {
                timer.player1 -> timer.player1TimeRemaining--
                timer.player2 -> timer.player2TimeRemaining--
            }
            
            sendTimerUpdate(timer)
            
            // Check if current player ran out of time
            val currentPlayerTimeRemaining = when (timer.currentTurnPlayer) {
                timer.player1 -> timer.player1TimeRemaining
                timer.player2 -> timer.player2TimeRemaining
                else -> Int.MAX_VALUE
            }
            
            if (currentPlayerTimeRemaining <= 0) {
                timer.timerTask?.cancel(false)
                // Switch to next player instead of ending battle
                switchToNextPlayer(timer)
                return@scheduleAtFixedRate
            }
            
        }, 1, 1, TimeUnit.SECONDS)
    }

    private fun sendTimerUpdate(timer: BattleMoveTimer) {
        val packet = MoveTimerUpdatePacket(
            timer.battleId,
            MoveTimerUpdatePacket.PlayerTimerInfo(timer.player1.uuid, timer.player1TimeRemaining),
            MoveTimerUpdatePacket.PlayerTimerInfo(timer.player2.uuid, timer.player2TimeRemaining),
            timer.currentTurnPlayer?.uuid
        )
        
        ServerPlayNetworking.send(timer.player1, packet)
        ServerPlayNetworking.send(timer.player2, packet)
    }

    private fun handleTimerExpiration(timer: BattleMoveTimer) {
        if (!timer.isActive) return
        
        timer.isActive = false
        
        // Determine winner (player who didn't run out of time)
        val winner = when (timer.currentTurnPlayer) {
            timer.player1 -> timer.player2
            timer.player2 -> timer.player1
            else -> null
        }
        
        // Find and close the battle
        val battle = BattleRegistry.getBattle(timer.battleId)
        if (battle != null && winner != null) {
            // Force end the battle with the winner
            battle.end()
            
            // Send final timer update indicating battle ended
            sendFinalTimerUpdate(timer, winner)
        }
        
        // Clean up
        battleTimers.remove(timer.battleId)
    }

    private fun sendFinalTimerUpdate(timer: BattleMoveTimer, winner: ServerPlayerEntity) {
        val packet = MoveTimerUpdatePacket(
            timer.battleId,
            MoveTimerUpdatePacket.PlayerTimerInfo(timer.player1.uuid, timer.player1TimeRemaining),
            MoveTimerUpdatePacket.PlayerTimerInfo(timer.player2.uuid, timer.player2TimeRemaining),
            null, // No current turn player since battle ended
            true, // Battle ended due to timer
            winner.uuid
        )
        
        ServerPlayNetworking.send(timer.player1, packet)
        ServerPlayNetworking.send(timer.player2, packet)
    }

    fun endBattleTimer(battleId: UUID) {
        val timer = battleTimers[battleId]
        if (timer != null) {
            timer.isActive = false
            timer.timerTask?.cancel(false)
            battleTimers.remove(battleId)
        }
    }

    fun shutdown() {
        scheduler.shutdown()
        battleTimers.values.forEach { timer ->
            timer.isActive = false
            timer.timerTask?.cancel(false)
        }
        battleTimers.clear()
    }
}