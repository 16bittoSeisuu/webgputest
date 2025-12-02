package net.japanesehunter.webgpu.interop

external class GPUVertexBufferLayout(
  val arrayStride: Int,
  val stepMode: String = definedExternally, // "vertex" | "instance"
  val attributes: Array<GPUVertexAttribute>,
)
