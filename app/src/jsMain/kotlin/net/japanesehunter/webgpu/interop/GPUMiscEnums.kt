package net.japanesehunter.webgpu.interop

value class GPUQueryType private constructor(
  val value: String,
) {
  override fun toString(): String = value

  companion object {
    val Occlusion = GPUQueryType("occlusion")
    val Timestamp = GPUQueryType("timestamp")
  }
}

value class GPUBufferMapState private constructor(
  val value: String,
) {
  override fun toString(): String = value

  companion object {
    val Unmapped = GPUBufferMapState("unmapped")
    val Pending = GPUBufferMapState("pending")
    val Mapped = GPUBufferMapState("mapped")
  }
}
