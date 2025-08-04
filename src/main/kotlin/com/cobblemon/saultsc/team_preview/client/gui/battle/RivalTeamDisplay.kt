package com.cobblemon.saultsc.team_preview.client.gui.battle

import com.cobblemon.mod.common.api.gui.blitk
import com.cobblemon.mod.common.api.text.bold
import com.cobblemon.mod.common.api.text.font
import com.cobblemon.mod.common.api.text.text
import com.cobblemon.mod.common.battles.ShowdownPokemon
import com.cobblemon.mod.common.client.CobblemonResources
import com.cobblemon.mod.common.client.gui.drawProfilePokemon
import com.cobblemon.mod.common.client.render.drawScaledText
import com.cobblemon.mod.common.client.render.getDepletableRedGreen
import com.cobblemon.mod.common.client.render.models.blockbench.FloatingState
import com.cobblemon.mod.common.pokemon.Gender
import com.cobblemon.mod.common.pokemon.Pokemon
import com.cobblemon.mod.common.util.cobblemonResource
import com.cobblemon.mod.common.util.lang
import com.cobblemon.mod.common.util.math.fromEulerXYZDegrees
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import org.joml.Quaternionf
import org.joml.Vector3f

/**
 * Un componente que renderiza la lista de Pokémon de un equipo rival.
 * No gestiona el fondo ni elementos interactivos, solo muestra los tiles de los Pokémon.
 */
class RivalTeamDisplay(
  private val opponentTeam: List<Pair<ShowdownPokemon, Pokemon>>,
  private val getSlotPosition: (Int) -> Pair<Float, Float>
) {
  private val tiles = mutableListOf<TeamPreviewTile>()

  fun init() {
    tiles.clear()
    opponentTeam.forEachIndexed { index, (showdownPokemon, pokemon) ->
      val (slotX, slotY) = getSlotPosition(index)
      tiles.add(TeamPreviewTile(slotX, slotY, pokemon, showdownPokemon))
    }
  }

  fun render(context: DrawContext, delta: Float) {
    tiles.forEach { it.render(context, delta) }
  }

  class TeamPreviewTile(
    private val x: Float,
    private val y: Float,
    private val pokemon: Pokemon,
    private val showdownPokemon: ShowdownPokemon
  ) {
    companion object {
      const val TILE_WIDTH = 94
      const val TILE_HEIGHT = 29
      const val SCALE = 0.5F
      val tileDisabledTexture = cobblemonResource("textures/gui/battle/party_select_disabled.png")
    }

    private val state = FloatingState()

    fun render(context: DrawContext, deltaTicks: Float) {
      state.currentAspects = pokemon.aspects
      val matrixStack = context.matrices

      val healthRatioSplits = showdownPokemon.condition.split(" ")[0].split("/")
      val (hp, maxHp) = if (healthRatioSplits.size == 1) 0 to 0
      else healthRatioSplits[0].toInt() to pokemon.maxHealth

      val hpRatio = if (maxHp > 0) hp / maxHp.toFloat() else 0f
      val isFainted = "fnt" in showdownPokemon.condition

      // Renderizar tile base deshabilitado
      blitk(
        matrixStack = matrixStack,
        texture = tileDisabledTexture,
        x = x,
        y = y,
        width = TILE_WIDTH,
        height = TILE_HEIGHT,
        vOffset = if (isFainted) 0 else TILE_HEIGHT,
        textureHeight = TILE_HEIGHT * 2,
      )

      // Status effect
      val status = pokemon.status?.status?.showdownName
      if (hpRatio > 0F && status != null) {
        blitk(
          matrixStack = matrixStack,
          texture = cobblemonResource("textures/gui/interact/party_select_status_$status.png"),
          x = x + 27,
          y = y + 24,
          height = 5,
          width = 37
        )

        drawScaledText(
          context = context,
          text = lang("ui.status.$status").bold(),
          x = x + 32.5,
          y = y + 24.5,
          shadow = true,
          scale = SCALE
        )
      }

      // Pokéball icon
      val ballIcon = cobblemonResource("textures/gui/ball/" + pokemon.caughtBall.name.path + ".png")
      val ballHeight = 22
      blitk(
        matrixStack = matrixStack,
        texture = ballIcon,
        x = (x + 85) / SCALE,
        y = (y - 3) / SCALE,
        height = ballHeight,
        width = 18,
        vOffset = 0,
        textureHeight = ballHeight * 2,
        scale = SCALE
      )

      // Renderizar Pokémon
      matrixStack.push()
      matrixStack.translate(x + TILE_WIDTH - (25 / 2.0) - 4, y - 1.0, 0.0)
      matrixStack.scale(2.5F, 2.5F, 1F)
      drawProfilePokemon(
        species = pokemon.species.resourceIdentifier,
        matrixStack = matrixStack,
        rotation = Quaternionf().fromEulerXYZDegrees(Vector3f(13F, 35F, 0F)),
        state = state,
        scale = 4.5F,
        partialTicks = deltaTicks
      )
      matrixStack.pop()

      // Elementos de UI encima del Pokémon
      matrixStack.push()
      matrixStack.translate(0.0, 0.0, 100.0)

      val textOpacity = if (isFainted) 0.7F else 1F

      // Level
      drawScaledText(
        context = context,
        font = CobblemonResources.DEFAULT_LARGE,
        text = lang("ui.lv").bold(),
        x = x + 5,
        y = y + 4,
        opacity = textOpacity,
        shadow = true
      )
      drawScaledText(
        context = context,
        font = CobblemonResources.DEFAULT_LARGE,
        text = pokemon.level.toString().text().bold(),
        x = x + 5 + 13,
        y = y + 4,
        opacity = textOpacity,
        shadow = true
      )

      // Nombre del Pokémon
      val displayText = pokemon.getDisplayName().bold()
      drawScaledText(
        context = context,
        font = CobblemonResources.DEFAULT_LARGE,
        text = displayText,
        x = x + 5,
        y = y + 11,
        opacity = textOpacity,
        shadow = true
      )

      // Género
      val gender = pokemon.gender
      if (gender != Gender.GENDERLESS) {
        val pokemonDisplayNameWidth =
          MinecraftClient.getInstance().textRenderer.getWidth(displayText.font(CobblemonResources.DEFAULT_LARGE))
        val isMale = gender == Gender.MALE
        val textSymbol = if (isMale) "♂".text().bold() else "♀".text().bold()
        drawScaledText(
          context = context,
          font = CobblemonResources.DEFAULT_LARGE,
          text = textSymbol,
          x = x + 6 + pokemonDisplayNameWidth,
          y = y + 11,
          colour = if (isMale) 0x32CBFF else 0xFC5454,
          opacity = textOpacity,
          shadow = true
        )
      }

      // Barra de HP
      val barWidthMax = 90
      val barWidth = (hpRatio * barWidthMax).toInt()
      val (red, green) = getDepletableRedGreen(hpRatio)

      blitk(
        matrixStack = matrixStack,
        texture = CobblemonResources.WHITE,
        x = x + 1,
        y = y + 22,
        width = barWidth,
        height = 1,
        red = red * 0.8F,
        green = green * 0.8F,
        blue = 0.27F
      )

      // Texto HP
      val hpPercentage = (hpRatio * 100).toInt()
      drawScaledText(
        context = context,
        text = "$hpPercentage%".text(),
        x = x + 14,
        y = y + 24.5,
        scale = SCALE,
        centered = true
      )

      matrixStack.pop()
    }
  }
}