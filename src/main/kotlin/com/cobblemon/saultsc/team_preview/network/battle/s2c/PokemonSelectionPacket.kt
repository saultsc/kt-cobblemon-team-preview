package com.cobblemon.saultsc.team_preview.network.battle.s2c

import com.cobblemon.saultsc.team_preview.TeamPreview
import net.minecraft.network.RegistryByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.packet.CustomPayload
import net.minecraft.util.Identifier
import java.util.UUID

data class PokemonSelectionPacket(
  val battleId: UUID,
  val selectedPokemonIndex: Int
) : CustomPayload {
    companion object {
        val ID: CustomPayload.Id<PokemonSelectionPacket> = CustomPayload.Id(Identifier.of(TeamPreview.Companion.MODID, "pokemon_selection"))

        val CODEC: PacketCodec<RegistryByteBuf, PokemonSelectionPacket> = PacketCodec.of(::write, ::read)

        private fun write(packet: PokemonSelectionPacket, buf: RegistryByteBuf) {
            buf.writeUuid(packet.battleId)
            buf.writeInt(packet.selectedPokemonIndex)
        }

        private fun read(buf: RegistryByteBuf): PokemonSelectionPacket {
            val battleId = buf.readUuid()
            val selectedPokemonIndex = buf.readInt()
            return PokemonSelectionPacket(battleId, selectedPokemonIndex)
        }
    }

    override fun getId(): CustomPayload.Id<out CustomPayload> = ID
}