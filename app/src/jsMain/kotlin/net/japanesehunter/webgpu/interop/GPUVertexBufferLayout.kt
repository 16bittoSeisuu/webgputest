@file:Suppress("INLINE_CLASS_IN_EXTERNAL_DECLARATION_WARNING")

package net.japanesehunter.webgpu.interop

external class GPUVertexBufferLayout(
  val arrayStride: Int,
  val stepMode: GPUVertexStepMode = definedExternally, // "vertex" | "instance"
  val attributes: Array<GPUVertexAttribute>,
)
