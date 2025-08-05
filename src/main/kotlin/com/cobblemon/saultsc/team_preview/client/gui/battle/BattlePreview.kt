package com.cobblemon.saultsc.team_preview.client.gui.battle

import com.cobblemon.mod.common.api.gui.blitk
import com.cobblemon.mod.common.battles.ShowdownPokemon
import com.cobblemon.mod.common.client.render.drawScaledText
import com.cobblemon.mod.common.pokemon.Pokemon
import com.cobblemon.mod.common.util.cobblemonResource
import com.cobblemon.mod.common.util.lang
import com.cobblemon.saultsc.team_preview.network.battle.s2c.PokemonSelectionPacket
import com.cobblemon.saultsc.team_preview.network.battle.s2c.BattleTimerUpdatePacket
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import java.util.*

class BattlePreview(
  private val battleId: UUID,
  private val opponentTeam: List<Pair<ShowdownPokemon, Pokemon>>,
  private val opponentName: String,
  private val playerTeam: List<Pair<ShowdownPokemon, Pokemon>>,
  private val playerName: String
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

  // Timer variables - ahora usando el battleId del servidor
  private var selectionTimeRemaining: Int = 30
  private var preStartTimeRemaining: Int = 5
  private var currentPhase: BattleTimerUpdatePacket.TimerPhase = BattleTimerUpdatePacket.TimerPhase.SELECTION
  private var hasSelectedPokemon: Boolean = false
  private var isFinished: Boolean = false

  override fun shouldCloseOnEsc() = false

  override fun init() {
    super.init()
    backgroundY = if (this.height > 304) (this.height / 2) - (BACKGROUND_HEIGHT / 2)
    else this.height - (BACKGROUND_HEIGHT + 78)

    rivalTeamDisplay = RivalTeamDisplay(opponentTeam, this::getRivalSlotPosition)
    rivalTeamDisplay.init()

    playerTeamSelector = PlayerTeamSelector(playerTeam, this::getPlayerSlotPosition, this::onPokemonSelected)
    playerTeamSelector.init()
  }

  private fun onPokemonSelected(selectedIndex: Int) {
    if (currentPhase == BattleTimerUpdatePacket.TimerPhase.SELECTION && !hasSelectedPokemon) {
      hasSelectedPokemon = true
      // Enviar selección al servidor
      val packet = PokemonSelectionPacket(battleId, selectedIndex)
      ClientPlayNetworking.send(packet)
    }
  }

  fun updateTimer(timerUpdate: BattleTimerUpdatePacket) {
    // Solo actualizar si el battleId coincide con el de esta pantalla
    if (this.battleId != timerUpdate.battleId) {
      return // Ignorar actualizaciones de otras batallas
    }

    this.selectionTimeRemaining = timerUpdate.selectionTimeRemaining
    this.preStartTimeRemaining = timerUpdate.preStartTimeRemaining
    this.currentPhase = timerUpdate.phase

    if (currentPhase == BattleTimerUpdatePacket.TimerPhase.FINISHED) {
      isFinished = true
    }
  }

  private fun formatTime(seconds: Int): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format("%d:%02d", minutes, remainingSeconds)
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
    if (isFinished) {
      client?.execute { close() }
      return
    }
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

    val playerTeamTotalWidth = (PlayerTeamSelector.PlayerTeamTile.TILE_WIDTH * 2) + SLOT_HORIZONTAL_SPACING
    val playerTeamStartX = (width / 2f) - playerTeamTotalWidth - 20
    val playerTitle = Text.literal("$playerName's ").append(lang("ui.party"))
    val playerTitleWidth = textRenderer.getWidth(playerTitle)
    drawScaledText(
      context = context,
      text = playerTitle,
      x = playerTeamStartX + (playerTeamTotalWidth / 2) - (playerTitleWidth / 2f),
      y = backgroundY + 17f,
      shadow = true
    )

    // Renderiza los slots del equipo del jugador (izquierda)
    for (i in 0 until 6) {
      val (playerX, playerY) = getPlayerSlotPosition(i)
      blitk(
        matrixStack = matrixStack,
        texture = PlayerTeamSelector.PlayerTeamTile.tileDisabledTexture,
        x = playerX,
        y = playerY,
        width = PlayerTeamSelector.PlayerTeamTile.TILE_WIDTH,
        height = PlayerTeamSelector.PlayerTeamTile.TILE_HEIGHT,
        vOffset = PlayerTeamSelector.PlayerTeamTile.TILE_HEIGHT,
        textureHeight = PlayerTeamSelector.PlayerTeamTile.TILE_HEIGHT * 2,
      )
    }

    val rivalTeamTotalWidth = (RivalTeamDisplay.TeamPreviewTile.TILE_WIDTH * 2) + SLOT_HORIZONTAL_SPACING
    val rivalTeamStartX = (width / 2f) + 20
    val opponentTitle = Text.literal("$opponentName's ").append(lang("ui.party"))
    val opponentTitleWidth = textRenderer.getWidth(opponentTitle)
    drawScaledText(
      context = context,
      text = opponentTitle,
      x = rivalTeamStartX + (rivalTeamTotalWidth / 2) - (opponentTitleWidth / 2f),
      y = backgroundY + 17f,
      shadow = true
    )
    // Renderiza los slots del equipo rival (derecha)
    for (i in 0 until 6) {
      val (rivalX, rivalY) = getRivalSlotPosition(i)
      blitk(
        matrixStack = matrixStack,
        texture = RivalTeamDisplay.TeamPreviewTile.tileDisabledTexture,
        x = rivalX,
        y = rivalY,
        width = RivalTeamDisplay.TeamPreviewTile.TILE_WIDTH,
        height = RivalTeamDisplay.TeamPreviewTile.TILE_HEIGHT,
        vOffset = RivalTeamDisplay.TeamPreviewTile.TILE_HEIGHT,
        textureHeight = RivalTeamDisplay.TeamPreviewTile.TILE_HEIGHT * 2,
      )
    }

    playerTeamSelector.render(context, mouseX, mouseY, delta)
    rivalTeamDisplay.render(context, delta)


    renderTimer(context)
  }

  private fun renderTimer(context: DrawContext) {
    val centerX = width / 2f
    val instructionY = backgroundY + 138f
    val timerDisplayY = instructionY + 25f // Mueve el temporizador hacia abajo

    when (currentPhase) {
      BattleTimerUpdatePacket.TimerPhase.SELECTION -> {

        val instructionText = if (hasSelectedPokemon) {
          Text.literal("Waiting for the rival").formatted(Formatting.YELLOW)
        } else {
          Text.literal("Select a Pokemon to Start").formatted(Formatting.WHITE)
        }

        val instructionWidth = textRenderer.getWidth(instructionText)
        drawScaledText(
          context = context,
          text = instructionText,
          x = centerX - (instructionWidth / 2f),
          y = instructionY,
          shadow = true
        )

        val timeLabel = Text.literal("Time Remaining").formatted(Formatting.GRAY)
        val timeLabelWidth = textRenderer.getWidth(timeLabel)
        drawScaledText(
          context = context,
          text = timeLabel,
          x = centerX - (timeLabelWidth / 2f),
          y = timerDisplayY,
          shadow = true
        )

        val timerText = Text.literal(formatTime(selectionTimeRemaining))
          .formatted(if (selectionTimeRemaining <= 10) Formatting.RED else Formatting.WHITE)

        val timerWidth = textRenderer.getWidth(timerText)
        drawScaledText(
          context = context,
          text = timerText,
          x = centerX - (timerWidth / 2f),
          y = timerDisplayY + 12f,
          shadow = true
        )
      }

      BattleTimerUpdatePacket.TimerPhase.PRE_START -> {
        val startLabel = Text.literal("Start in").formatted(Formatting.GREEN)
        val startLabelWidth = textRenderer.getWidth(startLabel)
        drawScaledText(
          context = context,
          text = startLabel,
          x = centerX - (startLabelWidth / 2f),
          y = timerDisplayY,
          shadow = true
        )

        val timerText = Text.literal(formatTime(preStartTimeRemaining))
          .formatted(Formatting.GREEN)

        val timerWidth = textRenderer.getWidth(timerText)
        drawScaledText(
          context = context,
          text = timerText,
          x = centerX - (timerWidth / 2f),
          y = timerDisplayY + 12f,
          shadow = true
        )
      }

      BattleTimerUpdatePacket.TimerPhase.FINISHED -> {
        // No se renderiza nada, la pantalla se cierra.
      }
    }
  }

  override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
    if (currentPhase == BattleTimerUpdatePacket.TimerPhase.SELECTION && !hasSelectedPokemon) {
      if (playerTeamSelector.mouseClicked(mouseX, mouseY)) {
        return true
      }
    }
    return super.mouseClicked(mouseX, mouseY, button)
  }

  override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
    return super.keyPressed(keyCode, scanCode, modifiers)
  }
}