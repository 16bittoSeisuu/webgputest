package net.japanesehunter.webgpu.interop

value class GPUSamplerBorderColor private constructor(
  val value: String,
) {
  override fun toString(): String = value

  companion object {
    val TransparentBlack = GPUSamplerBorderColor("transparent-black")
    val OpaqueBlack = GPUSamplerBorderColor("opaque-black")
    val OpaqueWhite = GPUSamplerBorderColor("opaque-white")
  }
}
