@file:Suppress("INLINE_CLASS_IN_EXTERNAL_DECLARATION_WARNING")

package net.japanesehunter.webgpu.interop

external class GPUVertexAttribute(
  val format: GPUVertexFormat,
  val offset: Int,
  val shaderLocation: Int,
)
