package net.japanesehunter.webgpu.interop

value class GPUPrimitiveTopology private constructor(
  val value: String,
) {
  override fun toString(): String = value

  companion object {
    val PointList = GPUPrimitiveTopology("point-list")
    val LineList = GPUPrimitiveTopology("line-list")
    val LineStrip = GPUPrimitiveTopology("line-strip")
    val TriangleList = GPUPrimitiveTopology("triangle-list")
    val TriangleStrip = GPUPrimitiveTopology("triangle-strip")
  }
}
