@file:Suppress("INLINE_CLASS_IN_EXTERNAL_DECLARATION_WARNING")

package net.japanesehunter.webgpu.interop

fun GPUVertexAttribute(
  format: GPUVertexFormat,
  offset: Long,
  shaderLocation: Int,
): GPUVertexAttribute =
  js("{}").unsafeCast<GPUVertexAttribute>().apply {
    this.format = format
    this.offset = offset
    this.shaderLocation = shaderLocation
  }

external interface GPUVertexAttribute {
  var format: GPUVertexFormat
  var offset: Long
  var shaderLocation: Int
}
