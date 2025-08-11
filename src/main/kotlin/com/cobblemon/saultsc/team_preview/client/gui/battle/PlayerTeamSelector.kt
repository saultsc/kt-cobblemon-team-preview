package com.cobblemon.saultsc.team_preview.client.gui.battle

import com.cobblemon.mod.common.CobblemonSounds
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
import net.minecraft.client.sound.PositionedSoundInstance
import net.minecraft.client.sound.SoundManager
import org.joml.Quaternionf
import org.joml.Vector3f

class PlayerTeamSelector(
    private val playerTeam: List<Pair<ShowdownPokemon, Pokemon>>,
    private val getSlotPosition: (Int) -> Pair<Float, Float>,
    private val onPokemonSelected: (Int) -> Unit = {} // Callback para cuando se selecciona un pokémon
) {
    val tiles = mutableListOf<PlayerTeamTile>()
    var selectedPokemon: Pokemon? = null

    fun init() {
        tiles.clear()
        selectedPokemon = null
        playerTeam.forEachIndexed { index, (showdownPokemon, pokemon) ->
            val (slotX, slotY) = getSlotPosition(index)
            val isFainted = "fnt" in showdownPokemon.condition
            tiles.add(PlayerTeamTile(this, slotX, slotY, pokemon, showdownPokemon, isFainted, index))
        }
    }

    fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        tiles.forEach { it.render(context, mouseX.toDouble(), mouseY.toDouble(), delta) }
    }

    fun mouseClicked(mouseX: Double, mouseY: Double): Boolean {
        val clickedTile = tiles.find { it.isHovered(mouseX, mouseY) && !it.isFainted }
        if (clickedTile != null) {
            selectedPokemon = clickedTile.pokemon
            onPokemonSelected(clickedTile.index) // Llamar callback con el índice
            playDownSound(MinecraftClient.getInstance().soundManager)
            return true
        }
        return false
    }

    fun playDownSound(soundManager: SoundManager) {
      soundManager.play(
        PositionedSoundInstance.master(
          CobblemonSounds.GUI_CLICK,
          1.0F // pitch
        )
      )
    }


  class PlayerTeamTile(
        private val selector: PlayerTeamSelector,
        private val x: Float,
        private val y: Float,
        val pokemon: Pokemon,
        private val showdownPokemon: ShowdownPokemon,
        val isFainted: Boolean,
        val index: Int // Agregar índice para identificar el pokémon
    ) {
        companion object {
            const val TILE_WIDTH = 94
            const val TILE_HEIGHT = 29
            const val SCALE = 0.5F
            val tileTexture = cobblemonResource("textures/gui/battle/party_select.png")
            val tileDisabledTexture = cobblemonResource("textures/gui/battle/party_select_disabled.png")
        }

        private val isSelected = selector.selectedPokemon == pokemon
        private val state = FloatingState()

        fun isHovered(mouseX: Double, mouseY: Double) = mouseX in x.toDouble()..(x + TILE_WIDTH).toDouble() && mouseY in y.toDouble()..(y + TILE_HEIGHT).toDouble()

        fun render(context: DrawContext, mouseX: Double, mouseY: Double, deltaTicks: Float) {
            state.currentAspects = pokemon.aspects
            val matrixStack = context.matrices

            val healthRatioSplits = showdownPokemon.condition.split(" ")[0].split("/")
            val (hp, maxHp) = if (healthRatioSplits.size == 1) 0 to 0
            else healthRatioSplits[0].toInt() to pokemon.maxHealth

            val hpRatio = if (maxHp > 0) hp / maxHp.toFloat() else 0f
            val isSelected = selector.selectedPokemon == pokemon

            // Renderizar tile base
            blitk(
                matrixStack = matrixStack,
                texture = if (!isFainted && !isSelected) tileTexture else tileDisabledTexture,
                x = x,
                y = y,
                width = TILE_WIDTH,
                height = TILE_HEIGHT,
                vOffset = if (isFainted) 0 else if (!isSelected && isHovered(mouseX, mouseY)) 0 else TILE_HEIGHT,
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
                x = (x + 85) / RivalTeamDisplay.TeamPreviewTile.Companion.SCALE,
                y = (y - 3) / RivalTeamDisplay.TeamPreviewTile.Companion.SCALE,
                height = ballHeight,
                width = 18,
                vOffset = if (isSelected) ballHeight else 0,
                textureHeight = ballHeight * 2,
                scale = RivalTeamDisplay.TeamPreviewTile.Companion.SCALE
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

            // Held Item
            val heldItem = pokemon.heldItem()
            if (!heldItem.isEmpty) {
                renderScaledGuiItemIcon(
                    matrixStack = matrixStack,
                    itemStack = heldItem,
                    x = x + 81.0,
                    y = y + 11.0,
                    scale = 0.5
                )
            }

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
            drawScaledText(
                context = context,
                text = "$hp/$maxHp".text(),
                x = x + 14,
                y = y + 24.5,
                scale = SCALE,
                centered = true
            )

            matrixStack.pop()
        }
    }
}