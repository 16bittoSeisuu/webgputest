package net.japanesehunter.webgpu.interop

external interface GPUTexture {
  fun createView(descriptor: GPUTextureViewDescriptor = definedExternally): GPUTextureView

  fun destroy()
}
