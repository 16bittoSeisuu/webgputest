@file:Suppress("INLINE_CLASS_IN_EXTERNAL_DECLARATION_WARNING")

package net.japanesehunter.webgpu.interop

external interface GPUTexture : GPUObjectBase {
  val width: Int
  val height: Int
  val depthOrArrayLayers: Int
  val mipLevelCount: Int
  val sampleCount: Int
  val dimension: GPUTextureDimension
  val format: GPUTextureFormat
  val usage: GPUTextureUsage
  val textureBindingViewDimension: GPUTextureViewDimension?

  fun createView(descriptor: GPUTextureViewDescriptor = definedExternally): GPUTextureView

  fun destroy()
}
