package net.japanesehunter.webgpu.interop

external interface GPUPipelineLayout : GPUObjectBase {
  companion object
}

val GPUPipelineLayout.Companion.auto: GPUPipelineLayout
  get() = gpuPipelineLayoutAuto

private val gpuPipelineLayoutAuto: GPUPipelineLayout =
  "auto".unsafeCast<GPUPipelineLayout>()
