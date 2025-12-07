@file:Suppress("ktlint:standard:enum-entry-name-case")

package net.japanesehunter

import net.japanesehunter.webgpu.interop.GPUVertexFormat

enum class GpuVertexFormat(
  val raw: GPUVertexFormat,
  val sizeInBytes: Int,
) {
  Uint8x2(GPUVertexFormat.Uint8x2, sizeInBytes = 2),
  Uint8x4(GPUVertexFormat.Uint8x4, sizeInBytes = 4),
  Sint8x2(GPUVertexFormat.Sint8x2, sizeInBytes = 2),
  Sint8x4(GPUVertexFormat.Sint8x4, sizeInBytes = 4),
  Unorm8x2(GPUVertexFormat.Unorm8x2, sizeInBytes = 2),
  Unorm8x4(GPUVertexFormat.Unorm8x4, sizeInBytes = 4),
  Snorm8x2(GPUVertexFormat.Snorm8x2, sizeInBytes = 2),
  Snorm8x4(GPUVertexFormat.Snorm8x4, sizeInBytes = 4),
  Uint16x2(GPUVertexFormat.Uint16x2, sizeInBytes = 4),
  Uint16x4(GPUVertexFormat.Uint16x4, sizeInBytes = 8),
  Sint16x2(GPUVertexFormat.Sint16x2, sizeInBytes = 4),
  Sint16x4(GPUVertexFormat.Sint16x4, sizeInBytes = 8),
  Unorm16x2(GPUVertexFormat.Unorm16x2, sizeInBytes = 4),
  Unorm16x4(GPUVertexFormat.Unorm16x4, sizeInBytes = 8),
  Snorm16x2(GPUVertexFormat.Snorm16x2, sizeInBytes = 4),
  Snorm16x4(GPUVertexFormat.Snorm16x4, sizeInBytes = 8),
  Float16x2(GPUVertexFormat.Float16x2, sizeInBytes = 4),
  Float16x4(GPUVertexFormat.Float16x4, sizeInBytes = 8),
  Float32(GPUVertexFormat.Float32, sizeInBytes = 4),
  Float32x2(GPUVertexFormat.Float32x2, sizeInBytes = 8),
  Float32x3(GPUVertexFormat.Float32x3, sizeInBytes = 12),
  Float32x4(GPUVertexFormat.Float32x4, sizeInBytes = 16),
  Uint32(GPUVertexFormat.Uint32, sizeInBytes = 4),
  Uint32x2(GPUVertexFormat.Uint32x2, sizeInBytes = 8),
  Uint32x3(GPUVertexFormat.Uint32x3, sizeInBytes = 12),
  Uint32x4(GPUVertexFormat.Uint32x4, sizeInBytes = 16),
  Sint32(GPUVertexFormat.Sint32, sizeInBytes = 4),
  Sint32x2(GPUVertexFormat.Sint32x2, sizeInBytes = 8),
  Sint32x3(GPUVertexFormat.Sint32x3, sizeInBytes = 12),
  Sint32x4(GPUVertexFormat.Sint32x4, sizeInBytes = 16),
  Unorm10_10_10_2(GPUVertexFormat.Unorm10_10_10_2, sizeInBytes = 4),
}
