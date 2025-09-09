package com.cobblemon.saultsc.team_preview.network.battle.s2c

import com.cobblemon.saultsc.team_preview.TeamPreview
import net.minecraft.network.RegistryByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.packet.CustomPayload
import net.minecraft.util.Identifier
import java.util.*

data class MoveTimerUpdatePacket(
    val battleId: UUID,
    val player1Timer: PlayerTimerInfo,
    val player2Timer: PlayerTimerInfo,
    val currentTurnPlayerId: UUID? = null,
    val battleEnded: Boolean = false,
    val winnerId: UUID? = null
) : CustomPayload {

    data class PlayerTimerInfo(
        val playerId: UUID,
        val timeRemaining: Int
    )

    companion object {
        val ID: CustomPayload.Id<MoveTimerUpdatePacket> = CustomPayload.Id(Identifier.of(TeamPreview.MODID, "move_timer_update"))

        val CODEC: PacketCodec<RegistryByteBuf, MoveTimerUpdatePacket> = PacketCodec.of(::write, ::read)

        private fun write(packet: MoveTimerUpdatePacket, buf: RegistryByteBuf) {
            buf.writeUuid(packet.battleId)
            
            // Write player1 timer info
            buf.writeUuid(packet.player1Timer.playerId)
            buf.writeInt(packet.player1Timer.timeRemaining)
            
            // Write player2 timer info
            buf.writeUuid(packet.player2Timer.playerId)
            buf.writeInt(packet.player2Timer.timeRemaining)
            
            // Write current turn player ID (nullable)
            buf.writeBoolean(packet.currentTurnPlayerId != null)
            if (packet.currentTurnPlayerId != null) {
                buf.writeUuid(packet.currentTurnPlayerId)
            }
            
            // Write battle ended flag
            buf.writeBoolean(packet.battleEnded)
            
            // Write winner ID (nullable)
            buf.writeBoolean(packet.winnerId != null)
            if (packet.winnerId != null) {
                buf.writeUuid(packet.winnerId)
            }
        }

        private fun read(buf: RegistryByteBuf): MoveTimerUpdatePacket {
            val battleId = buf.readUuid()
            
            // Read player1 timer info
            val player1Id = buf.readUuid()
            val player1Time = buf.readInt()
            val player1Timer = PlayerTimerInfo(player1Id, player1Time)
            
            // Read player2 timer info
            val player2Id = buf.readUuid()
            val player2Time = buf.readInt()
            val player2Timer = PlayerTimerInfo(player2Id, player2Time)
            
            // Read current turn player ID
            val hasCurrentTurnPlayer = buf.readBoolean()
            val currentTurnPlayerId = if (hasCurrentTurnPlayer) buf.readUuid() else null
            
            // Read battle ended flag
            val battleEnded = buf.readBoolean()
            
            // Read winner ID
            val hasWinner = buf.readBoolean()
            val winnerId = if (hasWinner) buf.readUuid() else null
            
            return MoveTimerUpdatePacket(
                battleId,
                player1Timer,
                player2Timer,
                currentTurnPlayerId,
                battleEnded,
                winnerId
            )
        }
    }

    override fun getId(): CustomPayload.Id<out CustomPayload> = ID
}