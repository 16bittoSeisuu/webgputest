package net.japanesehunter.webgpu.interop

fun GPUVertexState(
  module: GPUShaderModule,
  buffers: Array<GPUVertexBufferLayout>? = null,
  entryPoint: String? = null,
  constants: Map<String, Any>? = null,
): GPUVertexState =
  {}.unsafeCast<GPUVertexState>().apply {
    this.module = module
    if (buffers != null) this.buffers = buffers
    if (entryPoint != null) this.entryPoint = entryPoint
    if (constants != null) this.constants = constants
  }

external interface GPUVertexState : GPUProgrammableStage {
  override var module: GPUShaderModule
  var buffers: Array<GPUVertexBufferLayout>?
  override var entryPoint: String?
  override var constants: Map<String, Any>?
}
