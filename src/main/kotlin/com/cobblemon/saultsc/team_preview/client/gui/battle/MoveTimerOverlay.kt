package com.cobblemon.saultsc.team_preview.client.gui.battle

import com.cobblemon.saultsc.team_preview.network.battle.s2c.MoveTimerUpdatePacket
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.text.Text
import java.util.*

class MoveTimerOverlay {
    companion object {
        val INSTANCE = MoveTimerOverlay()
    }

    private var currentBattleId: UUID? = null
    private var player1Timer: MoveTimerUpdatePacket.PlayerTimerInfo? = null
    private var player2Timer: MoveTimerUpdatePacket.PlayerTimerInfo? = null
    private var currentTurnPlayerId: UUID? = null
    private var battleEnded: Boolean = false
    private var winnerId: UUID? = null

    fun onMoveTimerUpdate(packet: MoveTimerUpdatePacket) {
        currentBattleId = packet.battleId
        player1Timer = packet.player1Timer
        player2Timer = packet.player2Timer
        currentTurnPlayerId = packet.currentTurnPlayerId
        battleEnded = packet.battleEnded
        winnerId = packet.winnerId
    }

    fun render(context: DrawContext, width: Int, height: Int) {
        if (currentBattleId == null || player1Timer == null || player2Timer == null) return
        
        val client = MinecraftClient.getInstance()
        val textRenderer = client.textRenderer
        
        // Position timers in the top corners
        val margin = 10
        
        // Draw player 1 timer (top left)
        drawPlayerTimer(
            context, 
            textRenderer, 
            player1Timer!!, 
            margin, 
            margin, 
            isCurrentTurn = currentTurnPlayerId == player1Timer!!.playerId,
            client
        )
        
        // Draw player 2 timer (top right) 
        val timer2X = width - 150 - margin
        drawPlayerTimer(
            context,
            textRenderer,
            player2Timer!!,
            timer2X,
            margin,
            isCurrentTurn = currentTurnPlayerId == player2Timer!!.playerId,
            client
        )
        
        // Draw battle result if ended
        if (battleEnded && winnerId != null) {
            drawBattleResult(context, textRenderer, width, height, client)
        }
    }
    
    private fun drawPlayerTimer(
        context: DrawContext,
        textRenderer: net.minecraft.client.font.TextRenderer,
        timer: MoveTimerUpdatePacket.PlayerTimerInfo,
        x: Int,
        y: Int,
        isCurrentTurn: Boolean,
        client: MinecraftClient
    ) {
        val minutes = timer.timeRemaining / 60
        val seconds = timer.timeRemaining % 60
        val timeText = String.format("%d:%02d", minutes, seconds)
        
        // Get player name
        val playerName = if (client.player?.uuid == timer.playerId) {
            "You"
        } else {
            "Opponent"
        }
        
        // Choose colors based on current turn and time remaining
        val backgroundColor = if (isCurrentTurn) {
            if (timer.timeRemaining <= 10) 0x88FF0000.toInt() // Red background when low time and current turn
            else 0x8800FF00.toInt() // Green background for current turn
        } else {
            0x88808080.toInt() // Gray background for waiting
        }
        
        val textColor = if (timer.timeRemaining <= 10) 0xFFFF0000.toInt() else 0xFFFFFFFF.toInt()
        
        // Draw background
        context.fill(x, y, x + 150, y + 35, backgroundColor)
        
        // Draw border for current turn
        if (isCurrentTurn) {
            context.drawBorder(x - 1, y - 1, 152, 37, 0xFFFFFFFF.toInt())
        }
        
        // Draw player name
        context.drawText(textRenderer, Text.literal(playerName), x + 5, y + 5, 0xFFFFFFFF.toInt(), true)
        
        // Draw timer
        context.drawText(textRenderer, Text.literal(timeText), x + 5, y + 20, textColor, true)
        
        // Draw turn indicator
        if (isCurrentTurn) {
            context.drawText(textRenderer, Text.literal("YOUR TURN"), x + 80, y + 15, 0xFFFFFF00.toInt(), true)
        }
    }
    
    private fun drawBattleResult(
        context: DrawContext,
        textRenderer: net.minecraft.client.font.TextRenderer,
        width: Int,
        height: Int,
        client: MinecraftClient
    ) {
        val isWinner = client.player?.uuid == winnerId
        val resultText = if (isWinner) "YOU WIN! (Opponent timed out)" else "YOU LOSE! (You timed out)"
        val textColor = if (isWinner) 0xFF00FF00.toInt() else 0xFFFF0000.toInt()
        
        // Center the text
        val textWidth = textRenderer.getWidth(resultText)
        val x = (width - textWidth) / 2
        val y = height / 2 - 50
        
        // Draw background
        context.fill(x - 10, y - 5, x + textWidth + 10, y + 15, 0xAA000000.toInt())
        
        // Draw result text
        context.drawText(textRenderer, Text.literal(resultText), x, y, textColor, true)
    }
    
    fun clearTimer() {
        currentBattleId = null
        player1Timer = null
        player2Timer = null
        currentTurnPlayerId = null
        battleEnded = false
        winnerId = null
    }
}