package net.japanesehunter.webgpu.interop

value class GPUPolygonMode private constructor(val value: String) {
  override fun toString(): String =
    value

  companion object {
    val Fill = GPUPolygonMode("fill")
    val Line = GPUPolygonMode("line")
  }
}
