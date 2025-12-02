package net.japanesehunter.webgpu.interop

external interface GPUQueue {
  fun submit(commandBuffers: Array<GPUCommandBuffer>)
}
