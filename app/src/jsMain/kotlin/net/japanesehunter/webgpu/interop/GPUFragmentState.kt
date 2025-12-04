package net.japanesehunter.webgpu.interop

fun GPUFragmentState(
  module: GPUShaderModule,
  targets: Array<GPUColorTargetState>,
  entryPoint: String? = null,
  constants: Map<String, Any>? = null,
): GPUFragmentState =
  js("{}").unsafeCast<GPUFragmentState>().apply {
    this.module = module
    this.targets = targets
    if (entryPoint != null) this.entryPoint = entryPoint
    if (constants != null) this.constants = constants
  }

external interface GPUFragmentState : GPUProgrammableStage {
  override var module: GPUShaderModule
  var targets: Array<GPUColorTargetState>
  override var entryPoint: String?
  override var constants: Map<String, Any>?
}
