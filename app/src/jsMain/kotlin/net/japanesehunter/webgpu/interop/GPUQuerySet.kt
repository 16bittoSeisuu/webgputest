package net.japanesehunter.webgpu.interop

external interface GPUQuerySet : GPUObjectBase {
  override var label: String
  val type: String
  val count: Int
}
