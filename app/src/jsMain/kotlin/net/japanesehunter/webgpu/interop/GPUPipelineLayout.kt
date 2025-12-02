package net.japanesehunter.webgpu.interop

external interface GPUPipelineLayout : GPUObjectBase

val autoPipelineLayout: GPUPipelineLayout
  get() = "auto".unsafeCast<GPUPipelineLayout>()
