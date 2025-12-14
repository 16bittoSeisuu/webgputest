package net.japanesehunter.webgpu

import arrow.fx.coroutines.ResourceScope
import net.japanesehunter.webgpu.interop.GPUDevice
import net.japanesehunter.webgpu.interop.GPUExtent3D
import net.japanesehunter.webgpu.interop.GPUTexture
import net.japanesehunter.webgpu.interop.GPUTextureDescriptor
import net.japanesehunter.webgpu.interop.GPUTextureFormat
import net.japanesehunter.webgpu.interop.GPUTextureUsage

context(device: GPUDevice, canvas: CanvasContext, resource: ResourceScope)
fun createDepthStencilTexture(
  sampleCount: Int,
  format: GPUTextureFormat = GPUTextureFormat.Depth24PlusStencil8,
): () -> GPUTexture {
  var knownWidth = canvas.width
  var knownHeight = canvas.height

  fun create() =
    device.createTexture(
      GPUTextureDescriptor(
        size = GPUExtent3D(width = knownWidth, height = knownHeight),
        format = format,
        sampleCount = sampleCount,
        usage = GPUTextureUsage.RenderAttachment,
        label = "Depth Stencil Texture ${knownWidth}x$knownHeight",
      ),
    )
  var isDestroyed = false
  var texture = create()
  return {
    if (knownWidth != canvas.width || knownHeight != canvas.height) {
      knownWidth = canvas.width
      knownHeight = canvas.height
      texture.destroy()
      texture = create()
    }
    check(!isDestroyed) { "Depth Stencil Texture has been destroyed" }
    texture
  }.also {
    resource.onClose {
      texture.destroy()
      isDestroyed = true
    }
  }
}
