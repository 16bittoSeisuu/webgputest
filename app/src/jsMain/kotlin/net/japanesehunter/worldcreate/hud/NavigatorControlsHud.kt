package net.japanesehunter.worldcreate.hud

import kotlinx.browser.document
import kotlinx.browser.window
import net.japanesehunter.worldcreate.CameraNavigator.Settings
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.events.Event
import org.w3c.dom.events.KeyboardEvent

/**
 * Represents a heads-up display that exposes camera navigator controls for key bindings and sensitivity.
 *
 * The container remains attached to the document body while the instance is alive, and it must be used on the browser main thread because it manipulates the DOM directly.
 * @param settings navigator configuration that is mutated in response to user interaction.
 */
class NavigatorControlsHud(
  private val settings: Settings,
) : AutoCloseable {
  private val container: HTMLDivElement =
    (
      (document.getElementById(CONTAINER_ID) as? HTMLDivElement)
        ?: (document.createElement("div") as HTMLDivElement).also {
          it.id = CONTAINER_ID
          document.body?.appendChild(it)
            ?: error("Document body is not available")
        }
    ).apply {
      style.apply {
        position = "fixed"
        top = "12px"
        right = "12px"
        width = "fit-content"
        padding = "0.75rem"
        backgroundColor = "rgba(0, 0, 0, 0.68)"
        color = "#FFF"
        borderRadius = "0.5rem"
        fontFamily = "monospace, system-ui"
        fontSize = "0.85rem"
        zIndex = "2"
      }
    }

  private val teardown: MutableList<() -> Unit> = mutableListOf()

  init {
    container.appendChild(createSectionTitle("Movement Keys"))
    createKeyRow("Move Forward", { settings.forwardKey }, { settings.forwardKey = it }).also { (row, close) ->
      addRow(row, close)
    }
    createKeyRow("Move Backward", { settings.backwardKey }, { settings.backwardKey = it }).also { (row, close) ->
      addRow(row, close)
    }
    createKeyRow("Strafe Left", { settings.leftKey }, { settings.leftKey = it }).also { (row, close) ->
      addRow(row, close)
    }
    createKeyRow("Strafe Right", { settings.rightKey }, { settings.rightKey = it }).also { (row, close) ->
      addRow(row, close)
    }
    createKeyRow("Move Up", { settings.upKey }, { settings.upKey = it }).also { (row, close) ->
      addRow(row, close)
    }
    createKeyRow("Move Down", { settings.downKey }, { settings.downKey = it }).also { (row, close) ->
      addRow(row, close)
    }

    container.appendChild(createSectionTitle("Speed"))
    createSliderRow(
      label = "Horizontal",
      min = "0",
      max = "10",
      step = "0.1",
      initial = settings.horizontalSpeedMetersPerSecond,
      suffix = "m/s",
      onChange = { settings.horizontalSpeedMetersPerSecond = it },
    ).also { (row, close) ->
      addRow(row, close)
    }
    createSliderRow(
      label = "Vertical",
      min = "0",
      max = "10",
      step = "0.1",
      initial = settings.verticalSpeedMetersPerSecond,
      suffix = "m/s",
      onChange = { settings.verticalSpeedMetersPerSecond = it },
    ).also { (row, close) ->
      addRow(row, close)
    }

    container.appendChild(createSectionTitle("Mouse"))
    createSliderRow(
      label = "Sensitivity",
      min = "0",
      max = "5",
      step = "0.01",
      initial = settings.mouseSensitivityDegPerDot,
      suffix = "deg/dot",
      onChange = { settings.mouseSensitivityDegPerDot = it },
    ).also { (row, close) ->
      addRow(row, close)
    }
  }

  private fun createSectionTitle(text: String): HTMLDivElement =
    (document.createElement("div") as HTMLDivElement).apply {
      this.textContent = text
      style.marginBottom = "0.35rem"
      style.fontWeight = "700"
    }

  private fun createSliderRow(
    label: String,
    min: String,
    max: String,
    step: String,
    initial: Double,
    suffix: String,
    onChange: (Double) -> Unit,
  ): Pair<HTMLDivElement, () -> Unit> {
    require(initial.isFinite()) { "Initial slider value must be finite" }
    val row = document.createElement("div") as HTMLDivElement
    row.style.marginBottom = "0.6rem"
    val title = document.createElement("div") as HTMLDivElement
    title.textContent = label
    title.style.marginBottom = "0.2rem"
    val valueLabel = document.createElement("div") as HTMLDivElement
    valueLabel.style.marginTop = "0.2rem"
    val slider = document.createElement("input") as HTMLInputElement
    slider.type = "range"
    slider.min = min
    slider.max = max
    slider.step = step
    slider.value = initial.toString()
    slider.style.width = "75%"

    fun updateLabel(value: Double) {
      valueLabel.textContent = "${value.toString().take(5)} $suffix"
    }
    updateLabel(initial)
    val handler: (Event) -> Unit = {
      slider.value.toDoubleOrNull()?.let { value ->
        onChange(value)
        updateLabel(value)
      }
    }
    slider.addEventListener("input", handler)
    row.appendChild(title)
    row.appendChild(slider)
    row.appendChild(valueLabel)
    return row to { slider.removeEventListener("input", handler) }
  }

  private fun keyLabel(code: String): String = code.removePrefix("Key").removePrefix("Digit").ifEmpty { code }

  private fun createKeyRow(
    label: String,
    getter: () -> String,
    setter: (String) -> Unit,
  ): Pair<HTMLDivElement, () -> Unit> {
    val row = document.createElement("div") as HTMLDivElement
    row.style.marginBottom = "0.5rem"
    val title = document.createElement("div") as HTMLDivElement
    title.textContent = label
    title.style.marginBottom = "0.2rem"
    val button = document.createElement("button") as HTMLButtonElement
    button.textContent = keyLabel(getter())
    button.style.padding = "0.35rem 0.5rem"
    button.style.borderRadius = "0.35rem"
    button.style.border = "1px solid rgba(255, 255, 255, 0.25)"
    button.style.background = "rgba(255, 255, 255, 0.08)"
    button.style.color = "#FFF"
    button.style.cursor = "pointer"
    var pending: ((Event) -> Unit)? = null
    button.onclick = {
      button.textContent = "Press key..."
      pending?.let {
        window.removeEventListener("keydown", it)
      }
      var capture: ((Event) -> Unit)? = null
      capture =
        listener@{ event ->
          val keyEvent = event as? KeyboardEvent ?: return@listener
          keyEvent.preventDefault()
          keyEvent.stopPropagation()
          setter(keyEvent.code)
          button.textContent = keyLabel(keyEvent.code)
          capture?.let { window.removeEventListener("keydown", it) }
          pending = null
        }
      pending = capture
      capture.let { window.addEventListener("keydown", it) }
    }
    row.appendChild(title)
    row.appendChild(button)
    return row to {
      pending?.let {
        window.removeEventListener("keydown", it)
        pending = null
      }
    }
  }

  private fun addRow(
    row: HTMLDivElement,
    close: () -> Unit = {},
  ) {
    container.appendChild(row)
    teardown.add(close)
  }

  override fun close() {
    teardown.forEach { it.invoke() }
    container.remove()
  }
}

private const val CONTAINER_ID = "camera-controls"
