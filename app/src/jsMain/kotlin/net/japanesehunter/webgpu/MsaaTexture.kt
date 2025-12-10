package net.japanesehunter.webgpu

import arrow.fx.coroutines.ResourceScope
import net.japanesehunter.webgpu.interop.GPUDevice
import net.japanesehunter.webgpu.interop.GPUExtent3D
import net.japanesehunter.webgpu.interop.GPUTextureDescriptor
import net.japanesehunter.webgpu.interop.GPUTextureUsage
import net.japanesehunter.webgpu.interop.GPUTextureView

context(device: GPUDevice, canvas: CanvasContext, resource: ResourceScope)
fun createMsaaTexture(sampleCount: Int = 4): MsaaTexture {
  var knownWidth = canvas.width
  var knownHeight = canvas.height

  fun create() =
    device.createTexture(
      GPUTextureDescriptor(
        size = GPUExtent3D(width = knownWidth, height = knownHeight),
        sampleCount = sampleCount,
        format = canvas.preferredFormat,
        usage = GPUTextureUsage.RenderAttachment,
        label = "MSAA Texture ${knownWidth}x$knownHeight",
      ),
    )
  var isDestroyed = false
  var texture = create()
  return object : MsaaTexture {
    override val sampleCount: Int = sampleCount

    override fun provide(): GPUTextureView {
      if (knownWidth != canvas.width || knownHeight != canvas.height) {
        knownWidth = canvas.width
        knownHeight = canvas.height
        texture.destroy()
        texture = create()
      }
      check(!isDestroyed) { "MSAA Texture has been destroyed" }
      return texture.createView()
    }
  }.also {
    resource.onClose {
      texture.destroy()
      isDestroyed = true
    }
  }
}

interface MsaaTexture {
  val sampleCount: Int

  fun provide(): GPUTextureView
}
