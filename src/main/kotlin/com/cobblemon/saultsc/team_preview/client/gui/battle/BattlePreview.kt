package com.cobblemon.saultsc.team_preview.client.gui.battle

import com.cobblemon.mod.common.api.gui.blitk
import com.cobblemon.mod.common.battles.ShowdownPokemon
import com.cobblemon.mod.common.client.render.drawScaledText
import com.cobblemon.mod.common.pokemon.Pokemon
import com.cobblemon.mod.common.util.cobblemonResource
import com.cobblemon.mod.common.util.lang
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.text.Text

class BattlePreview(
  private val opponentTeam: List<Pair<ShowdownPokemon, Pokemon>>,
  private val playerTeam: List<Pair<ShowdownPokemon, Pokemon>>
) : Screen(Text.literal("battle_preview")) {

  companion object {
    const val SLOT_HORIZONTAL_SPACING = 4F
    const val SLOT_VERTICAL_SPACING = 2F
    const val BACKGROUND_HEIGHT = 148
    val underlayTexture = cobblemonResource("textures/gui/battle/selection_underlay.png")
  }

  private lateinit var rivalTeamDisplay: RivalTeamDisplay
  private lateinit var playerTeamSelector: PlayerTeamSelector
  private var backgroundY: Int = 0

  override fun init() {
    super.init()
    backgroundY = if (this.height > 304) (this.height / 2) - (BACKGROUND_HEIGHT / 2)
    else this.height - (BACKGROUND_HEIGHT + 78)

    rivalTeamDisplay = RivalTeamDisplay(opponentTeam, this::getRivalSlotPosition)
    rivalTeamDisplay.init()

    playerTeamSelector = PlayerTeamSelector(playerTeam, this::getPlayerSlotPosition)
    playerTeamSelector.init()
  }

  // Corregido: La posición del equipo del jugador se ha movido más a la izquierda.
  private fun getPlayerSlotPosition(index: Int): Pair<Float, Float> {
    val totalTeamWidth = (PlayerTeamSelector.PlayerTeamTile.TILE_WIDTH * 2) + SLOT_HORIZONTAL_SPACING
    val startX = (width / 2f) - totalTeamWidth - 20
    val startY = backgroundY + 34f
    val row = index / 2
    val column = index % 2
    val slotX = startX + column * (SLOT_HORIZONTAL_SPACING + PlayerTeamSelector.PlayerTeamTile.TILE_WIDTH)
    val slotY = startY + row * (SLOT_VERTICAL_SPACING + PlayerTeamSelector.PlayerTeamTile.TILE_HEIGHT)
    return Pair(slotX, slotY)
  }

  // La posición del equipo rival ahora está a la derecha.
  private fun getRivalSlotPosition(index: Int): Pair<Float, Float> {
    val startX = (width / 2f) + 20
    val startY = backgroundY + 34f
    val row = index / 2
    val column = index % 2
    val slotX = startX + column * (SLOT_HORIZONTAL_SPACING + RivalTeamDisplay.TeamPreviewTile.TILE_WIDTH)
    val slotY = startY + row * (SLOT_VERTICAL_SPACING + RivalTeamDisplay.TeamPreviewTile.TILE_HEIGHT)
    return Pair(slotX, slotY)
  }

  override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
    super.render(context, mouseX, mouseY, delta)
    val matrixStack = context.matrices

    blitk(
      matrixStack = matrixStack,
      texture = underlayTexture,
      x = 0,
      y = backgroundY,
      width = width,
      height = BACKGROUND_HEIGHT
    )



    // Renderiza los slots del equipo del jugador (izquierda)
    for (i in 0 until playerTeam.size) {
      val (playerX, playerY) = getPlayerSlotPosition(i)
      blitk(
        matrixStack = matrixStack,
        texture = PlayerTeamSelector.PlayerTeamTile.tileDisabledTexture,
        x = playerX,
        y = playerY,
        width = PlayerTeamSelector.PlayerTeamTile.TILE_WIDTH,
        height = PlayerTeamSelector.PlayerTeamTile.TILE_HEIGHT,
        vOffset = 0,
        textureHeight = PlayerTeamSelector.PlayerTeamTile.TILE_HEIGHT * 2,
      )
    }

    // Renderiza los slots del equipo rival (derecha)
    for (i in 0 until opponentTeam.size) {
      val (rivalX, rivalY) = getRivalSlotPosition(i)
      blitk(
        matrixStack = matrixStack,
        texture = RivalTeamDisplay.TeamPreviewTile.tileDisabledTexture,
        x = rivalX,
        y = rivalY,
        width = RivalTeamDisplay.TeamPreviewTile.TILE_WIDTH,
        height = RivalTeamDisplay.TeamPreviewTile.TILE_HEIGHT,
        vOffset = 0,
        textureHeight = RivalTeamDisplay.TeamPreviewTile.TILE_HEIGHT * 2,
      )
    }

    playerTeamSelector.render(context, mouseX, mouseY, delta)
    rivalTeamDisplay.render(context, delta)
  }

  override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
    if (playerTeamSelector.mouseClicked(mouseX, mouseY)) {
      return true
    }
    return super.mouseClicked(mouseX, mouseY, button)
  }

  override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
    if (super.keyPressed(keyCode, scanCode, modifiers)) {
      return true
    }
    if (client?.options?.inventoryKey?.matchesKey(keyCode, scanCode) == true) {
      close()
      return true
    }
    return false
  }
}