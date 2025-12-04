@file:Suppress("INLINE_CLASS_IN_EXTERNAL_DECLARATION_WARNING")

package net.japanesehunter.webgpu.interop

external interface GPUQuerySet : GPUObjectBase {
  override var label: String
  val type: GPUQueryType
  val count: Int
}
