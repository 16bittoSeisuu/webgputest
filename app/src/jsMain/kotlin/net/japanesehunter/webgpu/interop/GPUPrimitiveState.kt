@file:Suppress("INLINE_CLASS_IN_EXTERNAL_DECLARATION_WARNING")

package net.japanesehunter.webgpu.interop

fun GPUPrimitiveState(
  topology: GPUPrimitiveTopology? = null,
  stripIndexFormat: GPUIndexFormat? = null,
  frontFace: GPUFrontFace? = null,
  cullMode: GPUCullMode? = null,
  unclippedDepth: Boolean? = null,
  polygonMode: GPUPolygonMode? = null,
  conservative: Boolean? = null,
): GPUPrimitiveState =
  js("{}").unsafeCast<GPUPrimitiveState>().apply {
    if (topology != null) this.topology = topology
    if (stripIndexFormat != null) this.stripIndexFormat = stripIndexFormat
    if (frontFace != null) this.frontFace = frontFace
    if (cullMode != null) this.cullMode = cullMode
    if (unclippedDepth != null) this.unclippedDepth = unclippedDepth
    if (polygonMode != null) this.polygonMode = polygonMode
    if (conservative != null) this.conservative = conservative
  }

external interface GPUPrimitiveState {
  var topology: GPUPrimitiveTopology
  var stripIndexFormat: GPUIndexFormat
  var frontFace: GPUFrontFace
  var cullMode: GPUCullMode
  var unclippedDepth: Boolean
  var polygonMode: GPUPolygonMode
  var conservative: Boolean
}
