package net.japanesehunter.webgpu.interop

value class GPUCullMode private constructor(val value: String) {
  override fun toString(): String =
    value

  companion object {
    val None = GPUCullMode("none")
    val Front = GPUCullMode("front")
    val Back = GPUCullMode("back")
  }
}
