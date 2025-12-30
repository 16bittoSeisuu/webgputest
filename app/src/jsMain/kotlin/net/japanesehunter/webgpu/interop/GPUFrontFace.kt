package net.japanesehunter.webgpu.interop

value class GPUFrontFace private constructor(val value: String) {
  override fun toString(): String =
    value

  companion object {
    val Ccw = GPUFrontFace("ccw")
    val Cw = GPUFrontFace("cw")
  }
}
