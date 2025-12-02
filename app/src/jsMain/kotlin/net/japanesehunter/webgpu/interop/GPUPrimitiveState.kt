package net.japanesehunter.webgpu.interop

external class GPUPrimitiveState(
  val topology: String = definedExternally, // "point-list" | "line-list" | ...
  val stripIndexFormat: String = definedExternally, // "uint16" | "uint32"
  val frontFace: String = definedExternally, // "ccw" | "cw"
  val cullMode: String = definedExternally, // "none" | "front" | "back"
  val unclippedDepth: Boolean = definedExternally,
  val polygonMode: String = definedExternally, // "fill" | "line"
  val conservative: Boolean = definedExternally,
)
