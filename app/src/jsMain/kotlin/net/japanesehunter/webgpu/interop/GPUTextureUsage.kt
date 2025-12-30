package net.japanesehunter.webgpu.interop

value class GPUTextureUsage private constructor(val value: Int) {
  operator fun plus(
    other: GPUTextureUsage,
  ): GPUTextureUsage =
    GPUTextureUsage(value or other.value)

  fun contains(
    other: GPUTextureUsage,
  ): Boolean =
    value and other.value == other.value

  companion object {
    val None = GPUTextureUsage(0)
    val CopySrc = GPUTextureUsage(0x01)
    val CopyDst = GPUTextureUsage(0x02)
    val TextureBinding = GPUTextureUsage(0x04)
    val StorageBinding = GPUTextureUsage(0x08)
    val RenderAttachment = GPUTextureUsage(0x10)
  }
}
