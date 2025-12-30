package net.japanesehunter.webgpu.interop

/**
 * Exposes the concrete resource and pipeline limits enforced by the current GPU device.
 *
 * Values mirror WebGPU's `GPUSupportedLimits` object and can be used to size buffers,
 * textures, and shader workloads safely before issuing commands.
 */
external interface GPUSupportedLimits {
  /** Maximum length of a 1D texture in texels. */
  val maxTextureDimension1D: Int

  /** Maximum width or height of a 2D texture in texels. */
  val maxTextureDimension2D: Int

  /** Maximum width, height, or depth of a 3D texture in texels. */
  val maxTextureDimension3D: Int

  /** Maximum number of array layers across all texture types. */
  val maxTextureArrayLayers: Int

  /** Maximum number of bind groups in a single pipeline layout. */
  val maxBindGroups: Int

  /** Maximum combined count of bind groups and vertex buffers in a layout. */
  val maxBindGroupsPlusVertexBuffers: Int

  /** Maximum total bindings in a single bind group. */
  val maxBindingsPerBindGroup: Int

  /** Maximum dynamic uniform buffers allowed in a pipeline layout. */
  val maxDynamicUniformBuffersPerPipelineLayout: Int

  /** Maximum dynamic storage buffers allowed in a pipeline layout. */
  val maxDynamicStorageBuffersPerPipelineLayout: Int

  /** Maximum sampled texture bindings available to a shader stage. */
  val maxSampledTexturesPerShaderStage: Int

  /** Maximum sampler bindings available to a shader stage. */
  val maxSamplersPerShaderStage: Int

  /** Maximum storage buffer bindings available to a shader stage. */
  val maxStorageBuffersPerShaderStage: Int

  /** Maximum storage texture bindings available to a shader stage. */
  val maxStorageTexturesPerShaderStage: Int

  /** Maximum uniform buffer bindings available to a shader stage. */
  val maxUniformBuffersPerShaderStage: Int

  /** Maximum byte size for a single uniform buffer binding. */
  val maxUniformBufferBindingSize: Int

  /** Maximum byte size for a single storage buffer binding. */
  val maxStorageBufferBindingSize: Int

  /** Required alignment for dynamic uniform buffer offsets in bytes. */
  val minUniformBufferOffsetAlignment: Int

  /** Required alignment for dynamic storage buffer offsets in bytes. */
  val minStorageBufferOffsetAlignment: Int

  /** Maximum number of vertex buffers that can be bound simultaneously. */
  val maxVertexBuffers: Int

  /** Maximum total size in bytes of a single GPU buffer. */
  val maxBufferSize: Int

  /** Maximum number of vertex attributes across all vertex buffers. */
  val maxVertexAttributes: Int

  /** Maximum stride in bytes for a single vertex buffer layout. */
  val maxVertexBufferArrayStride: Int

  /** Maximum number of user-defined varyings between shader stages. */
  val maxInterStageShaderVariables: Int

  /** Maximum number of color attachments in a render pipeline. */
  val maxColorAttachments: Int

  /** Maximum bytes written per sample across all color attachments. */
  val maxColorAttachmentBytesPerSample: Int

  /** Maximum shared memory available to a compute workgroup in bytes. */
  val maxComputeWorkgroupStorageSize: Int

  /** Maximum total invocations in a single compute workgroup. */
  val maxComputeInvocationsPerWorkgroup: Int

  /** Maximum X dimension for a compute workgroup. */
  val maxComputeWorkgroupSizeX: Int

  /** Maximum Y dimension for a compute workgroup. */
  val maxComputeWorkgroupSizeY: Int

  /** Maximum Z dimension for a compute workgroup. */
  val maxComputeWorkgroupSizeZ: Int

  /** Maximum number of compute workgroups dispatchable per dimension. */
  val maxComputeWorkgroupsPerDimension: Int

  /** Generic access to implementation-defined limits by name. */
  operator fun get(
    limitName: String,
  ): Int?
}
