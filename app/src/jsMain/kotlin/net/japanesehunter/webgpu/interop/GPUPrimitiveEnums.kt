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

value class GPUIndexFormat private constructor(
  val value: String,
) {
  override fun toString(): String = value

  companion object {
    val Uint16 = GPUIndexFormat("uint16")
    val Uint32 = GPUIndexFormat("uint32")
  }
}

value class GPUFrontFace private constructor(
  val value: String,
) {
  override fun toString(): String = value

  companion object {
    val Ccw = GPUFrontFace("ccw")
    val Cw = GPUFrontFace("cw")
  }
}

value class GPUCullMode private constructor(
  val value: String,
) {
  override fun toString(): String = value

  companion object {
    val None = GPUCullMode("none")
    val Front = GPUCullMode("front")
    val Back = GPUCullMode("back")
  }
}

value class GPUPolygonMode private constructor(
  val value: String,
) {
  override fun toString(): String = value

  companion object {
    val Fill = GPUPolygonMode("fill")
    val Line = GPUPolygonMode("line")
  }
}

value class GPUVertexStepMode private constructor(
  val value: String,
) {
  override fun toString(): String = value

  companion object {
    val Vertex = GPUVertexStepMode("vertex")
    val Instance = GPUVertexStepMode("instance")
  }
}
