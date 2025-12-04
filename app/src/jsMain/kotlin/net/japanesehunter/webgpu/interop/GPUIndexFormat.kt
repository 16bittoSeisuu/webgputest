package net.japanesehunter.webgpu.interop

value class GPUIndexFormat private constructor(
  val value: String,
) {
  override fun toString(): String = value

  companion object {
    val Uint16 = GPUIndexFormat("uint16")
    val Uint32 = GPUIndexFormat("uint32")
  }
}
