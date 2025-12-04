package net.japanesehunter.webgpu.interop

value class GPUMapMode private constructor(
  val value: Int,
) {
  operator fun plus(other: GPUMapMode): GPUMapMode = GPUMapMode(value or other.value)

  fun contains(other: GPUMapMode): Boolean = value and other.value == other.value

  companion object {
    val None = GPUMapMode(0)
    val Read = GPUMapMode(0x0001)
    val Write = GPUMapMode(0x0002)
  }
}
