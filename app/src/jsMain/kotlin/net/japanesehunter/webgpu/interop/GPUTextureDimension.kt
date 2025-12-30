package net.japanesehunter.webgpu.interop

value class GPUTextureDimension private constructor(val value: String) {
  override fun toString(): String =
    value

  companion object {
    val D1 = GPUTextureDimension("1d")
    val D2 = GPUTextureDimension("2d")
    val D3 = GPUTextureDimension("3d")
  }
}
