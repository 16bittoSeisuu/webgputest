package net.japanesehunter.webgpu.interop

import kotlin.js.Promise

external interface GPUDevice : GPUObjectBase {
  val adapterInfo: GPUAdapterInfo
  val queue: GPUQueue

  fun createCommandEncoder(descriptor: GPUCommandEncoderDescriptor = definedExternally): GPUCommandEncoder

  fun createRenderPipelineAsync(descriptor: GPURenderPipelineDescriptor): Promise<GPURenderPipeline>

  fun createShaderModule(descriptor: GPUShaderModuleDescriptor): GPUShaderModule

  fun destroy()
}
