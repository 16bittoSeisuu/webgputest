package net.japanesehunter.webgpu.interop

value class GPUMipmapFilterMode private constructor(val value: String) {
  override fun toString(): String =
    value

  companion object {
    val Nearest = GPUMipmapFilterMode("nearest")
    val Linear = GPUMipmapFilterMode("linear")
  }
}
