package net.japanesehunter.worldcreate.hud

import arrow.fx.coroutines.ResourceScope
import kotlinx.browser.document
import net.japanesehunter.math.Direction16
import org.w3c.dom.HTMLDivElement

/**
 * Represents a heads-up display that renders the camera direction in a fixed overlay.
 *
 * The container remains attached to the document body with HUD styling while the instance is alive.
 * The class is not thread-safe and is intended for use on the browser main thread.
 * @param container overlay element used to present the HUD content.
 */
class CameraHud internal constructor(
  private val container: HTMLDivElement,
) : AutoCloseable {
  /**
   * Updates the HUD text to show the provided compass direction.
   *
   * Mutates the container text content when the displayed direction changes.
   * @param direction compass heading to render in the HUD.
   */
  fun update(direction: Direction16) {
    val label = direction.displayName()
    val text = "Direction: $label"
    if (container.textContent == text) return
    container.textContent = text
  }

  override fun close() {
    container.remove()
  }
}

/**
 * Creates a camera direction HUD attached to the document body.
 *
 * Inserts the HUD container when missing, reapplies overlay styles when the element already exists, and registers removal with the surrounding resource scope while mutating the DOM structure.
 * @return HUD instance managing the overlay container.
 * @throws IllegalStateException when the document body is not available.
 */
context(resource: ResourceScope)
fun CameraHud(): CameraHud {
  val body = document.body ?: error("Document body is not available")
  val container =
    (document.getElementById(CONTAINER_ID) as? HTMLDivElement)
      ?: (document.createElement("div") as HTMLDivElement).also { element ->
        element.id = CONTAINER_ID
        body.appendChild(element)
      }
  container.style.apply {
    position = "fixed"
    top = "12px"
    left = "12px"
    padding = "0.35rem 0.65rem"
    backgroundColor = "rgba(0, 0, 0, 0.75)"
    color = "#FFF"
    borderRadius = "0.35rem"
    fontFamily = "monospace, system-ui"
    fontSize = "0.85rem"
    setProperty("pointer-events", "none")
    zIndex = "1"
  }
  return CameraHud(container).also { hud ->
    resource.onClose {
      hud.close()
    }
  }
}

private const val CONTAINER_ID = "camera-direction"

private fun Direction16.displayName(): String =
  name
    .split('_')
    .joinToString(" ") { segment ->
      segment.lowercase().replaceFirstChar(Char::titlecaseChar)
    }
