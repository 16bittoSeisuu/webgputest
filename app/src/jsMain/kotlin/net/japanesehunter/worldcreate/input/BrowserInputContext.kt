package net.japanesehunter.worldcreate.input

import kotlinx.browser.document
import kotlinx.browser.window
import net.japanesehunter.worldcreate.world.EventSink
import net.japanesehunter.worldcreate.world.EventSource
import net.japanesehunter.worldcreate.world.EventSubscription
import org.w3c.dom.events.Event
import org.w3c.dom.events.KeyboardEvent
import org.w3c.dom.events.MouseEvent
import org.w3c.dom.events.WheelEvent

/**
 * Creates an input context that listens to browser keyboard and mouse events.
 *
 * Registers event listeners on the document and window. The returned context must be closed when
 * no longer needed to remove the listeners.
 *
 * @param action the block to execute with the input context.
 * @return the result of the action block.
 */
inline fun <R> inputContext(action: context(InputContext) () -> R): R = BrowserInputContext().use { context(it, action) }

/**
 * Provides browser-based input event delivery and key state tracking.
 *
 * Registers listeners on the document for keyboard events and on the window for mouse events.
 * All events are delivered synchronously on the browser main thread. The instance must be closed
 * to remove registered listeners.
 *
 * This implementation is not thread-safe and must only be accessed from the browser main thread.
 */
@PublishedApi
internal class BrowserInputContext :
  InputContext,
  AutoCloseable {
  private val sinks = mutableListOf<EventSink<InputEvent>>()
  private val pressedKeySet = mutableSetOf<String>()

  private val eventSource =
    object : EventSource<InputEvent> {
      override fun subscribe(sink: EventSink<InputEvent>): EventSubscription {
        sinks.add(sink)
        return EventSubscription { sinks.remove(sink) }
      }
    }

  private val keyDownHandler: (Event) -> Unit = { event ->
    val keyEvent = event.unsafeCast<KeyboardEvent>()
    pressedKeySet.add(keyEvent.code)
    emit(KeyDown(keyEvent.code, keyEvent.repeat))
  }

  private val keyUpHandler: (Event) -> Unit = { event ->
    val keyEvent = event.unsafeCast<KeyboardEvent>()
    pressedKeySet.remove(keyEvent.code)
    emit(KeyUp(keyEvent.code))
  }

  private val mouseMoveHandler: (Event) -> Unit = { event ->
    val mouseEvent = event.unsafeCast<MouseEvent>()
    val dx = mouseEvent.asDynamic().movementX as Double
    val dy = mouseEvent.asDynamic().movementY as Double
    emit(MouseMove(dx, dy))
  }

  private val mouseDownHandler: (Event) -> Unit = { event ->
    val mouseEvent = event.unsafeCast<MouseEvent>()
    emit(MouseDown(mouseEvent.button.toInt()))
  }

  private val mouseUpHandler: (Event) -> Unit = { event ->
    val mouseEvent = event.unsafeCast<MouseEvent>()
    emit(MouseUp(mouseEvent.button.toInt()))
  }

  private val wheelHandler: (Event) -> Unit = { event ->
    val wheelEvent = event.unsafeCast<WheelEvent>()
    emit(Wheel(wheelEvent.deltaX, wheelEvent.deltaY))
  }

  init {
    document.addEventListener("keydown", keyDownHandler)
    document.addEventListener("keyup", keyUpHandler)
    window.addEventListener("mousemove", mouseMoveHandler)
    window.addEventListener("mousedown", mouseDownHandler)
    window.addEventListener("mouseup", mouseUpHandler)
    window.addEventListener("wheel", wheelHandler)
  }

  override fun events(): EventSource<InputEvent> = eventSource

  override fun isKeyDown(code: String): Boolean = code in pressedKeySet

  override fun pressedKeys(): Set<String> = pressedKeySet.toSet()

  override fun close() {
    document.removeEventListener("keydown", keyDownHandler)
    document.removeEventListener("keyup", keyUpHandler)
    window.removeEventListener("mousemove", mouseMoveHandler)
    window.removeEventListener("mousedown", mouseDownHandler)
    window.removeEventListener("mouseup", mouseUpHandler)
    window.removeEventListener("wheel", wheelHandler)
    pressedKeySet.clear()
    sinks.clear()
  }

  private fun emit(event: InputEvent) {
    sinks.forEach { it.onEvent(event) }
  }
}
