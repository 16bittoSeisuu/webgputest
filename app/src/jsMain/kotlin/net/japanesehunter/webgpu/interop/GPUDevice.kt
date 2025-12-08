package net.japanesehunter.webgpu.interop

import kotlin.js.Promise

external interface GPUDevice : GPUObjectBase {
  val adapterInfo: GPUAdapterInfo

  /**
   * Supported resource and pipeline limits for this device.
   *
   * Values mirror WebGPU's `GPUSupportedLimits` and describe the validation caps that apply
   * when creating buffers, textures, pipelines, or dispatching work.
   */
  val limits: GPUSupportedLimits
  val queue: GPUQueue

  fun createBuffer(descriptor: GPUBufferDescriptor): GPUBuffer

  fun createCommandEncoder(descriptor: GPUCommandEncoderDescriptor = definedExternally): GPUCommandEncoder

  fun createSampler(descriptor: GPUSamplerDescriptor = definedExternally): GPUSampler

  fun createRenderPipelineAsync(descriptor: GPURenderPipelineDescriptor): Promise<GPURenderPipeline>

  fun createTexture(descriptor: GPUTextureDescriptor): GPUTexture

  fun createRenderBundleEncoder(descriptor: GPURenderBundleEncoderDescriptor): GPURenderBundleEncoder

  fun createBindGroup(descriptor: GPUBindGroupDescriptor): GPUBindGroup

  fun createShaderModule(descriptor: GPUShaderModuleDescriptor): GPUShaderModule

  fun destroy()
}
