package net.japanesehunter.webgpu.interop

sealed external interface GPUProgrammableStage {
  var module: GPUShaderModule
  var entryPoint: String?
  var constants: Map<String, Any>?
}
