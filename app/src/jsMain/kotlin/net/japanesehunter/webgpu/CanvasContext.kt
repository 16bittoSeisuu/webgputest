package net.japanesehunter.webgpu

import kotlinx.browser.document
import kotlinx.browser.window
import net.japanesehunter.webgpu.interop.GPU
import net.japanesehunter.webgpu.interop.GPUCanvasConfiguration
import net.japanesehunter.webgpu.interop.GPUCanvasContext
import net.japanesehunter.webgpu.interop.GPUTexture
import net.japanesehunter.webgpu.interop.GPUTextureFormat
import net.japanesehunter.webgpu.interop.navigator
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.events.Event

inline fun <R> canvasContext(
  id: String = "main",
  createOnMissing: Boolean = true,
  action: context(CanvasContext) () -> R,
): R =
  CanvasContextImpl(
    id,
    createOnMissing,
    navigator.gpu ?: throw UnsupportedBrowserException(),
  ).use {
    context(
      it,
      action,
    )
  }

interface CanvasContext {
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

  override fun close() {
    autoResize.forEach(AutoCloseable::close)
  }
}
