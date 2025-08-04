package com.cobblemon.saultsc.team_preview.network.battle

import com.cobblemon.mod.common.battles.ShowdownPokemon
import com.cobblemon.mod.common.pokemon.Pokemon
import com.cobblemon.saultsc.team_preview.TeamPreview
import net.minecraft.network.RegistryByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.packet.CustomPayload

data class BattlePreviewPacket(
  val playerTeam: List<Pair<ShowdownPokemon, Pokemon>>,
  val playerName: String,
  val opponentTeam: List<Pair<ShowdownPokemon, Pokemon>>,
  val opponentName: String
) : CustomPayload {

  override fun getId(): CustomPayload.Id<*> = ID

  companion object {
    val ID: CustomPayload.Id<BattlePreviewPacket> =
      CustomPayload.Id(TeamPreview.TEAM_PREVIEW_PACKET_ID)

    val CODEC: PacketCodec<RegistryByteBuf, BattlePreviewPacket> = PacketCodec.of(
      { packet, buf -> write(buf, packet) },
      { buf -> read(buf) }
    )

    private fun write(buf: RegistryByteBuf, packet: BattlePreviewPacket) {
      writeTeam(buf, packet.playerTeam)
      writeTeam(buf, packet.opponentTeam)
      buf.writeString(packet.playerName)
      buf.writeString(packet.opponentName)
    }

    private fun read(buf: RegistryByteBuf): BattlePreviewPacket {
      val playerTeam = readTeam(buf)
      val opponentTeam = readTeam(buf)

      val playerName = buf.readString()
      val opponentName = buf.readString()

      return BattlePreviewPacket(playerTeam, playerName, opponentTeam, opponentName)
    }

    private fun writeTeam(buf: RegistryByteBuf, team: List<Pair<ShowdownPokemon, Pokemon>>) {
      buf.writeInt(team.size)
      team.forEach { (showdown, pokemon) ->
        buf.writeNbt(pokemon.saveToNBT(buf.registryManager))
        buf.writeString(showdown.condition)
        buf.writeString(showdown.pokeball)
      }
    }

    private fun readTeam(buf: RegistryByteBuf): List<Pair<ShowdownPokemon, Pokemon>> {
      val size = buf.readInt()
      return (0 until size).map {
        val pokemon = Pokemon.loadFromNBT(buf.registryManager, buf.readNbt()!!)
        val condition = buf.readString()
        val pokeball = buf.readString()
        val showdown = ShowdownPokemon().apply {
          this.condition = condition
          this.pokeball = pokeball
        }
        showdown to pokemon
      }
    }
  }
}
