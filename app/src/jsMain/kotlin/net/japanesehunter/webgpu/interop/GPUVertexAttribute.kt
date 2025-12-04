@file:Suppress("INLINE_CLASS_IN_EXTERNAL_DECLARATION_WARNING")

package net.japanesehunter.webgpu.interop

fun GPUVertexAttribute(
  format: GPUVertexFormat,
  offset: Int,
  shaderLocation: Int,
): GPUVertexAttribute =
  {}.unsafeCast<GPUVertexAttribute>().apply {
    this.format = format
    this.offset = offset
    this.shaderLocation = shaderLocation
  }

external interface GPUVertexAttribute {
  var format: GPUVertexFormat
  var offset: Int
  var shaderLocation: Int
}
