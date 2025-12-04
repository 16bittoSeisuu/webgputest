package net.japanesehunter.webgpu.interop

value class GPUBufferUsage private constructor(
  val value: Int,
) {
  operator fun plus(other: GPUBufferUsage): GPUBufferUsage = GPUBufferUsage(value or other.value)

  fun contains(other: GPUBufferUsage): Boolean = value and other.value == other.value

  companion object {
    val None = GPUBufferUsage(0)
    val MapRead = GPUBufferUsage(0x0001)
    val MapWrite = GPUBufferUsage(0x0002)
    val CopySrc = GPUBufferUsage(0x0004)
    val CopyDst = GPUBufferUsage(0x0008)
    val Index = GPUBufferUsage(0x0010)
    val Vertex = GPUBufferUsage(0x0020)
    val Uniform = GPUBufferUsage(0x0040)
    val Storage = GPUBufferUsage(0x0080)
    val Indirect = GPUBufferUsage(0x0100)
    val QueryResolve = GPUBufferUsage(0x0200)
  }
}
