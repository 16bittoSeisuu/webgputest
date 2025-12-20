package net.japanesehunter.webgpu

import kotlinx.browser.document
import kotlinx.browser.window
import net.japanesehunter.webgpu.interop.GPU
import net.japanesehunter.webgpu.interop.GPUCanvasConfiguration
import net.japanesehunter.webgpu.interop.GPUCanvasContext
import net.japanesehunter.webgpu.interop.GPUTexture
import net.japanesehunter.webgpu.interop.GPUTextureFormat
import net.japanesehunter.webgpu.interop.navigator
import net.japanesehunter.worldcreate.input.PointerLock
import net.japanesehunter.worldcreate.input.PointerLockEvent
import net.japanesehunter.worldcreate.world.EventSink
import net.japanesehunter.worldcreate.world.EventSource
import net.japanesehunter.worldcreate.world.EventSubscription
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.events.Event
import kotlin.time.ComparableTimeMark
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

inline fun <R> canvasContext(
  id: String = "main",
  createOnMissing: Boolean = true,
  timeSource: TimeSource.WithComparableMarks = TimeSource.Monotonic,
  action: context(CanvasContext) () -> R,
): R =
  CanvasContextImpl(
    id,
    createOnMissing,
    navigator.gpu ?: throw UnsupportedBrowserException(),
    timeSource,
  ).use {
    context(
      it,
      action,
    )
  }

interface CanvasContext : PointerLock {
  val width: Int
  val height: Int

  val canvas: HTMLCanvasElement
  val preferredFormat: GPUTextureFormat

  fun onResize(action: () -> Unit): AutoCloseable

  fun getCurrentTexture(): GPUTexture

  fun configure(configuration: GPUCanvasConfiguration): AutoCloseable
}

@PublishedApi
internal class CanvasContextImpl(
  id: String,
  createOnMissing: Boolean,
  private val gpu: GPU,
  private val timeSource: TimeSource.WithComparableMarks,
) : CanvasContext,
  AutoCloseable {
  private val canvasElement: HTMLCanvasElement =
    (
      (document.getElementById(id) as? HTMLCanvasElement)
        ?: if (createOnMissing) {
          val canvas = document.createElement("canvas") as HTMLCanvasElement
          canvas.id = id
          document.body?.appendChild(canvas)
          canvas
        } else {
          error("Canvas element with id '$id' not found")
        }
    ).apply {
      style.width = "100%"
      style.height = "100%"
      width = window.innerWidth
      height = window.innerHeight
    }

  private val autoResize =
    mutableListOf<AutoCloseable>().apply {
      add(
        onResize {
          canvasElement.width = window.innerWidth
          canvasElement.height = window.innerHeight
        },
      )
    }

  private val canvasContext: GPUCanvasContext =
    canvasElement
      .getContext("webgpu")
      ?.unsafeCast<GPUCanvasContext>()
      ?: throw UnsupportedBrowserException()

  private val pointerLockSinks = mutableListOf<EventSink<PointerLockEvent>>()
  private var lastUnlockMark: ComparableTimeMark = timeSource.markNow()

  private val pointerLockChangeHandler: (Event) -> Unit = {
    val locked = document.asDynamic().pointerLockElement == canvasElement
    if (!locked) {
      lastUnlockMark = timeSource.markNow()
    }
    val event = PointerLockEvent(locked)
    pointerLockSinks.forEach { it.onEvent(event) }
  }

  private val pointerLockErrorHandler: (Event) -> Unit = {
    val event = PointerLockEvent(false)
    pointerLockSinks.forEach { it.onEvent(event) }
  }

  private val pointerLockEventSource =
    object : EventSource<PointerLockEvent> {
      override fun subscribe(sink: EventSink<PointerLockEvent>): EventSubscription {
        pointerLockSinks.add(sink)
        return EventSubscription { pointerLockSinks.remove(sink) }
      }
    }

  init {
    document.addEventListener("pointerlockchange", pointerLockChangeHandler)
    document.addEventListener("pointerlockerror", pointerLockErrorHandler)
  }

  override val canvas: HTMLCanvasElement
    get() = canvasElement

  override val width: Int
    get() = canvasElement.width

  override val height: Int
    get() = canvasElement.height

  override val preferredFormat: GPUTextureFormat
    get() = gpu.getPreferredCanvasFormat()

  override fun onResize(action: () -> Unit): AutoCloseable {
    val handler: (Event) -> Unit = {
      action()
    }
    window.addEventListener("resize", handler)
    return AutoCloseable {
      window.removeEventListener("resize", handler)
    }
  }

  override fun getCurrentTexture(): GPUTexture = canvasContext.getCurrentTexture()

  override fun configure(configuration: GPUCanvasConfiguration): AutoCloseable {
    canvasContext.configure(configuration)
    return AutoCloseable {
      canvasContext.unconfigure()
    }
  }

  override val isPointerLocked: Boolean
    get() = document.asDynamic().pointerLockElement == canvasElement

  override fun pointerLockEvents(): EventSource<PointerLockEvent> = pointerLockEventSource

  override fun requestPointerLock(): Boolean {
    if (lastUnlockMark.elapsedNow() < POINTER_LOCK_COOLDOWN) {
      return false
    }
    canvasElement.asDynamic().requestPointerLock()
    return true
  }

  override fun exitPointerLock() {
    if (isPointerLocked) {
      document.asDynamic().exitPointerLock()
    }
  }

  override fun close() {
    document.removeEventListener("pointerlockchange", pointerLockChangeHandler)
    document.removeEventListener("pointerlockerror", pointerLockErrorHandler)
    autoResize.forEach(AutoCloseable::close)
  }
}

private val POINTER_LOCK_COOLDOWN = 2.seconds
