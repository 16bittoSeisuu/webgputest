package net.japanesehunter.webgpu.interop

external interface GPUSampler :
  GPUObjectBase,
  GPUBindingResource {
  override var label: String
}
