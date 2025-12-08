package net.japanesehunter.webgpu.interop

value class GPUAddressMode private constructor(
  val value: String,
) {
  override fun toString(): String = value

  companion object {
    val ClampToEdge = GPUAddressMode("clamp-to-edge")
    val Repeat = GPUAddressMode("repeat")
    val MirrorRepeat = GPUAddressMode("mirror-repeat")
  }
}
