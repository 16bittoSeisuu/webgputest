package net.japanesehunter.webgpu.interop

value class GPUVertexFormat private constructor(
  val value: String,
) {
  override fun toString(): String = value

  companion object {
    val Uint8x2 = GPUVertexFormat("uint8x2")
    val Uint8x4 = GPUVertexFormat("uint8x4")
    val Sint8x2 = GPUVertexFormat("sint8x2")
    val Sint8x4 = GPUVertexFormat("sint8x4")
    val Unorm8x2 = GPUVertexFormat("unorm8x2")
    val Unorm8x4 = GPUVertexFormat("unorm8x4")
    val Snorm8x2 = GPUVertexFormat("snorm8x2")
    val Snorm8x4 = GPUVertexFormat("snorm8x4")
    val Uint16x2 = GPUVertexFormat("uint16x2")
    val Uint16x4 = GPUVertexFormat("uint16x4")
    val Sint16x2 = GPUVertexFormat("sint16x2")
    val Sint16x4 = GPUVertexFormat("sint16x4")
    val Unorm16x2 = GPUVertexFormat("unorm16x2")
    val Unorm16x4 = GPUVertexFormat("unorm16x4")
    val Snorm16x2 = GPUVertexFormat("snorm16x2")
    val Snorm16x4 = GPUVertexFormat("snorm16x4")
    val Float16x2 = GPUVertexFormat("float16x2")
    val Float16x4 = GPUVertexFormat("float16x4")
    val Float32 = GPUVertexFormat("float32")
    val Float32x2 = GPUVertexFormat("float32x2")
    val Float32x3 = GPUVertexFormat("float32x3")
    val Float32x4 = GPUVertexFormat("float32x4")
    val Uint32 = GPUVertexFormat("uint32")
    val Uint32x2 = GPUVertexFormat("uint32x2")
    val Uint32x3 = GPUVertexFormat("uint32x3")
    val Uint32x4 = GPUVertexFormat("uint32x4")
    val Sint32 = GPUVertexFormat("sint32")
    val Sint32x2 = GPUVertexFormat("sint32x2")
    val Sint32x3 = GPUVertexFormat("sint32x3")
    val Sint32x4 = GPUVertexFormat("sint32x4")
    val Unorm10_10_10_2 = GPUVertexFormat("unorm10-10-10-2")
  }
}
