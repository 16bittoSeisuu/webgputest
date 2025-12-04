@file:Suppress("INLINE_CLASS_IN_EXTERNAL_DECLARATION_WARNING")

package net.japanesehunter.webgpu.interop

external class GPUPrimitiveState(
  val topology: GPUPrimitiveTopology = definedExternally, // "point-list" | "line-list" | ...
  val stripIndexFormat: GPUIndexFormat = definedExternally, // "uint16" | "uint32"
  val frontFace: GPUFrontFace = definedExternally, // "ccw" | "cw"
  val cullMode: GPUCullMode = definedExternally, // "none" | "front" | "back"
  val unclippedDepth: Boolean = definedExternally,
  val polygonMode: GPUPolygonMode = definedExternally, // "fill" | "line"
  val conservative: Boolean = definedExternally,
)
