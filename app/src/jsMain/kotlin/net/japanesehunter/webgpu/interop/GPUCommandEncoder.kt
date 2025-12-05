package net.japanesehunter.webgpu.interop

external interface GPUCommandEncoder : GPUObjectBase {
  fun beginRenderPass(descriptor: GPURenderPassDescriptor): GPURenderPassEncoder

  fun copyBufferToBuffer(
    source: GPUBuffer,
    sourceOffset: Int,
    destination: GPUBuffer,
    destinationOffset: Int,
    size: Int,
  )

  fun finish(descriptor: GPUCommandBufferDescriptor = definedExternally): GPUCommandBuffer
}
