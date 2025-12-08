package net.japanesehunter.webgpu.interop

value class GPUFilterMode private constructor(
  val value: String,
) {
  override fun toString(): String = value

  companion object {
    val Nearest = GPUFilterMode("nearest")
    val Linear = GPUFilterMode("linear")
  }
}
