package net.japanesehunter.webgpu.interop

external interface GPUCanvasContext {
  fun configure(config: GPUCanvasConfiguration)

  fun getCurrentTexture(): GPUTexture
}
