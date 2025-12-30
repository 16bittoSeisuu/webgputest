package net.japanesehunter.webgpu.interop

value class GPUTextureViewDimension private constructor(val value: String) {
  override fun toString(): String =
    value

  companion object {
    val D1 = GPUTextureViewDimension("1d")
    val D2 = GPUTextureViewDimension("2d")
    val D2Array = GPUTextureViewDimension("2d-array")
    val Cube = GPUTextureViewDimension("cube")
    val CubeArray = GPUTextureViewDimension("cube-array")
    val D3 = GPUTextureViewDimension("3d")
  }
}
