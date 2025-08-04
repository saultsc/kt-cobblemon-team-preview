package com.cobblemon.saultsc.team_preview.network.battle.s2c

import com.cobblemon.saultsc.team_preview.TeamPreview
import net.minecraft.network.RegistryByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.packet.CustomPayload
import net.minecraft.util.Identifier
import java.util.*

data class BattleTimerUpdatePacket(
    val battleId: UUID,
    val selectionTimeRemaining: Int, // Tiempo restante para selección en segundos
    val preStartTimeRemaining: Int,  // Tiempo restante antes de iniciar batalla en segundos
    val phase: TimerPhase
) : CustomPayload {

    enum class TimerPhase {
        SELECTION,
        PRE_START,
        FINISHED
    }

    companion object {
        val ID: CustomPayload.Id<BattleTimerUpdatePacket> = CustomPayload.Id(Identifier.of(TeamPreview.MODID, "battle_timer_update"))

        val CODEC: PacketCodec<RegistryByteBuf, BattleTimerUpdatePacket> = PacketCodec.of(::write, ::read)

        private fun write(packet: BattleTimerUpdatePacket, buf: RegistryByteBuf) {
            buf.writeUuid(packet.battleId)
            buf.writeInt(packet.selectionTimeRemaining)
            buf.writeInt(packet.preStartTimeRemaining)
            buf.writeEnumConstant(packet.phase)
        }

        private fun read(buf: RegistryByteBuf): BattleTimerUpdatePacket {
            val battleId = buf.readUuid()
            val selectionTimeRemaining = buf.readInt()
            val preStartTimeRemaining = buf.readInt()
            val phase = buf.readEnumConstant(TimerPhase::class.java)
            return BattleTimerUpdatePacket(battleId, selectionTimeRemaining, preStartTimeRemaining, phase)
        }
    }

    override fun getId(): CustomPayload.Id<out CustomPayload> = ID
}
