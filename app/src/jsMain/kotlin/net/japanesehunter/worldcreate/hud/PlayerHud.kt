package net.japanesehunter.worldcreate.hud

import arrow.fx.coroutines.ResourceScope
import kotlinx.browser.document
import net.japanesehunter.worldcreate.entity.Player
import org.w3c.dom.HTMLDivElement

/**
 * Represents a heads-up display that renders the player position and velocity in a fixed overlay.
 *
 * The container remains attached to the document body with HUD styling while the instance is alive.
 * The class is not thread-safe and is intended for use on the browser main thread.
 *
 * @param container overlay element used to present the HUD content.
 * @param player the player whose state is displayed.
 */
class PlayerHud internal constructor(
  private val container: HTMLDivElement,
  private val player: Player,
) : AutoCloseable {
  /**
   * Updates the HUD text to show the current player position and velocity.
   *
   * Mutates the container innerHTML when the displayed values change.
   */
  fun update() {
    val pos = player.position
    val vel = player.velocity

    val x = pos.x.toString(unit = null, decimals = 2)
    val y = pos.y.toString(unit = null, decimals = 2)
    val z = pos.z.toString(unit = null, decimals = 2)
    val vx = vel.vx.toString(unit = null, decimals = 2)
    val vy = vel.vy.toString(unit = null, decimals = 2)
    val vz = vel.vz.toString(unit = null, decimals = 2)

    val text =
      buildString {
        append("<strong>Position:</strong><br>X: $x<br>Y: $y<br>Z: $z<br><br>")
        append("<strong>Velocity:</strong><br>X: $vx<br>Y: $vy<br>Z: $vz<br>")
      }

    if (container.innerHTML == text) return
    container.innerHTML = text
  }

  override fun close() {
    container.remove()
  }
}

/**
 * Creates a player state HUD attached to the document body.
 *
 * Inserts the HUD container when missing, reapplies overlay styles when the element already exists,
 * and registers removal with the surrounding resource scope while mutating the DOM structure.
 *
 * @param player the player whose state is displayed.
 * @param x horizontal offset from the left edge in pixels.
 *
 *   NaN: rejected
 *
 *   Infinity: rejected
 * @param y vertical offset from the top edge in pixels.
 *
 *   NaN: rejected
 *
 *   Infinity: rejected
 * @param scale overlay scale factor.
 *
 *   range: scale > 0.0
 *
 *   NaN: rejected
 *
 *   Infinity: rejected
 * @return HUD instance managing the overlay container.
 * @throws IllegalStateException when the document body is not available.
 */
context(resource: ResourceScope)
fun PlayerHud(
  player: Player,
  x: Double = 12.0,
  y: Double = 40.0,
  scale: Double = 1.0,
): PlayerHud {
  val body = document.body ?: error("Document body is not available")
  require(x.isFinite()) { "x must be finite" }
  require(y.isFinite()) { "y must be finite" }
  require(scale.isFinite() && scale > 0.0) { "Scale must be positive and finite" }
  val container =
    (document.getElementById(CONTAINER_ID) as? HTMLDivElement)
      ?: (document.createElement("div") as HTMLDivElement).also { element ->
        element.id = CONTAINER_ID
        body.appendChild(element)
      }
  container.style.apply {
    position = "fixed"
    top = "${y}px"
    left = "${x}px"
    padding = "0.35rem 0.65rem"
    backgroundColor = "rgba(0, 0, 0, 0.75)"
    color = "#FFF"
    borderRadius = "0.35rem"
    fontFamily = "monospace, system-ui"
    fontSize = "0.85rem"
    transformOrigin = "top left"
    transform = "scale($scale)"
    setProperty("pointer-events", "none")
    zIndex = "1"
  }
  return PlayerHud(container, player).also { hud ->
    resource.onClose {
      hud.close()
    }
  }
}

private const val CONTAINER_ID = "player-state"
