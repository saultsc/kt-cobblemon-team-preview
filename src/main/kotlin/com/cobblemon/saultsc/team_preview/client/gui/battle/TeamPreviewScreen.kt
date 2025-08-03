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
import com.cobblemon.mod.common.client.render.renderScaledGuiItemIcon
import com.cobblemon.mod.common.pokemon.Gender
import com.cobblemon.mod.common.pokemon.Pokemon
import com.cobblemon.mod.common.util.cobblemonResource
import com.cobblemon.mod.common.util.lang
import com.cobblemon.mod.common.util.math.fromEulerXYZDegrees
import net.minecraft.client.MinecraftClient


import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.text.Text
import org.joml.Quaternionf
import org.joml.Vector3f

class TeamPreviewScreen(
  private val opponentTeam: List<Pair<ShowdownPokemon, Pokemon>>
) : Screen(Text.literal("team_preview_screen")) {

  companion object {
    const val SLOT_HORIZONTAL_SPACING = 4F
    const val SLOT_VERTICAL_SPACING = 2F
    const val BACKGROUND_HEIGHT = 148
    val underlayTexture = cobblemonResource("textures/gui/battle/selection_underlay.png")
  }

  private val tiles = mutableListOf<TeamPreviewTile>()

  override fun init() {
    tiles.clear()

    opponentTeam.forEachIndexed { index, (showdownPokemon, pokemon) ->
      val (slotX, slotY) = getSlotPosition(index)
      tiles.add(TeamPreviewTile(slotX, slotY, pokemon, showdownPokemon))
    }
  }

  private fun getSlotPosition(index: Int): Pair<Float, Float> {
    val startX = ((width / 2) - TeamPreviewTile.TILE_WIDTH - 1)
    val startY = 34
    val row = index / 2
    val column = index % 2
    val slotX = startX.toFloat() + column * (SLOT_HORIZONTAL_SPACING + TeamPreviewTile.TILE_WIDTH)
    val slotY = startY.toFloat() + row * (SLOT_VERTICAL_SPACING + TeamPreviewTile.TILE_HEIGHT)
    return Pair(slotX, slotY)
  }

  override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
    super.render(context, mouseX, mouseY, delta)

    val matrixStack = context.matrices

    // Renderizar fondo
    blitk(
      matrixStack = matrixStack,
      texture = underlayTexture,
      x = 0,
      y = 0,
      width = width,
      height = BACKGROUND_HEIGHT
    )

    // Título
    val titleText = lang("ui.party")
    val textWidth = textRenderer.getWidth(titleText)
    drawScaledText(
      context = context,
      text = titleText,
      x = (width - textWidth) / 2,
      y = 17,
      shadow = true
    )

    // Renderizar slots vacíos
    for (index in 0 until 6) {
      val (slotX, slotY) = getSlotPosition(index)
      blitk(
        matrixStack = matrixStack,
        texture = TeamPreviewTile.tileDisabledTexture,
        x = slotX,
        y = slotY,
        width = TeamPreviewTile.TILE_WIDTH,
        height = TeamPreviewTile.TILE_HEIGHT - 7,
        vOffset = TeamPreviewTile.TILE_HEIGHT,
        textureHeight = TeamPreviewTile.TILE_HEIGHT * 2,
      )
    }

    // Renderizar Pokémon
    tiles.forEach { it.render(context, delta) }
  }

  override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
//    if (keyCode == 256) { // ESC key
//      close()
//      return true
//    }
    return super.keyPressed(keyCode, scanCode, modifiers)
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
      val tileTexture = cobblemonResource("textures/gui/battle/party_select.png")
      val tileDisabledTexture = cobblemonResource("textures/gui/battle/party_select_disabled.png")
    }

    private val state = FloatingState()

    fun render(context: DrawContext, deltaTicks: Float) {
      state.currentAspects = pokemon.aspects
      val matrixStack = context.matrices

      val healthRatioSplits = showdownPokemon.condition.split(" ")[0].split("/")
      val (hp, maxHp) = if (healthRatioSplits.size == 1) 0 to 0
      else healthRatioSplits[0].toInt() to pokemon.maxHealth

      val hpRatio = hp / maxHp.toFloat()
      val isFainted = "fnt" in showdownPokemon.condition

      // Renderizar tile base
      blitk(
        matrixStack = matrixStack,
        texture = tileTexture,
        x = x,
        y = y,
        width = TILE_WIDTH,
        height = TILE_HEIGHT,
        vOffset = 0,
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

//      This ui no aplly show item because the is party rival
//      val heldItem = pokemon.heldItem().copy()
//      if (!heldItem.isEmpty) {
//        renderScaledGuiItemIcon(
//          matrixStack = matrixStack,
//          itemStack = heldItem,
//          x = x + 81.0,
//          y = y + 11.0,
//          scale = 0.5
//        )
//      }

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
        val pokemonDisplayNameWidth = MinecraftClient.getInstance().textRenderer.getWidth(displayText.font(CobblemonResources.DEFAULT_LARGE))
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
      val barWidth = hpRatio * barWidthMax
      val (red, green) = getDepletableRedGreen(hpRatio)

      blitk(
        matrixStack = matrixStack,
        texture = CobblemonResources.WHITE,
        x = x + 1,
        y = y + 22,
        width = barWidth,
        height = 1,
        textureWidth = barWidth / hpRatio,
        uOffset = barWidthMax - barWidth,
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