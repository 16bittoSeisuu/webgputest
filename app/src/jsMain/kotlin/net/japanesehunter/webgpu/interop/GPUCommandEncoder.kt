package net.japanesehunter.webgpu.interop

external interface GPUCommandEncoder : GPUObjectBase {
  fun beginRenderPass(descriptor: GPURenderPassDescriptor): GPURenderPassEncoder

  fun finish(descriptor: GPUCommandBufferDescriptor = definedExternally): GPUCommandBuffer
}
