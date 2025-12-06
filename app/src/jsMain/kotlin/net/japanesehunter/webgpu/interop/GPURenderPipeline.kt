package net.japanesehunter.webgpu.interop

external interface GPURenderPipeline : GPUObjectBase {
  override var label: String

  fun getBindGroupLayout(index: Int): GPUBindGroupLayout
}
