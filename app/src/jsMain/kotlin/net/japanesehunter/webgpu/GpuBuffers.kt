package net.japanesehunter.webgpu

import arrow.fx.coroutines.Resource
import arrow.fx.coroutines.resource
import net.japanesehunter.webgpu.interop.GPUBufferUsage
import net.japanesehunter.webgpu.interop.GPUIndexFormat
import net.japanesehunter.webgpu.interop.GPUVertexFormat

// region vertex

interface VertexGpuBuffer : GpuBuffer {
  val format: GPUVertexFormat

  companion object
}

context(alloc: BufferAllocator)
fun VertexGpuBuffer.Companion.pos3D(data: FloatArray): Resource<VertexGpuBuffer> {
  val res = alloc.static(data, GPUBufferUsage.Vertex, "Vertex Position Buffer")
  return resource {
    val buf = res.bind()
    object : VertexGpuBuffer, GpuBuffer by buf {
      override val format: GPUVertexFormat = GPUVertexFormat.Float32x3
    }
  }
}

context(alloc: BufferAllocator)
fun VertexGpuBuffer.Companion.rgbaColor(data: FloatArray): Resource<VertexGpuBuffer> {
  val res = alloc.static(data, GPUBufferUsage.Vertex, "Vertex Color Buffer")
  return resource {
    val buf = res.bind()
    object : VertexGpuBuffer, GpuBuffer by buf {
      override val format: GPUVertexFormat = GPUVertexFormat.Float32x4
    }
  }
}

context(alloc: BufferAllocator)
fun VertexGpuBuffer.Companion.uv(data: FloatArray): Resource<VertexGpuBuffer> {
  val res = alloc.static(data, GPUBufferUsage.Vertex, "Vertex UV Buffer")
  return resource {
    val buf = res.bind()
    object : VertexGpuBuffer, GpuBuffer by buf {
      override val format: GPUVertexFormat = GPUVertexFormat.Float32x2
    }
  }
}

// endregion

// region index

interface IndexGpuBuffer : GpuBuffer {
  val indexCount: Int
  val indexFormat: GPUIndexFormat

  companion object
}

context(alloc: BufferAllocator)
fun IndexGpuBuffer.Companion.u16(data: ShortArray): Resource<IndexGpuBuffer> {
  val res = alloc.static(data, GPUBufferUsage.Index, "Index Buffer")
  return resource {
    val buf = res.bind()
    object : IndexGpuBuffer, GpuBuffer by buf {
      override val indexCount: Int = data.size
      override val indexFormat: GPUIndexFormat = GPUIndexFormat.Uint16
    }
  }
}

context(alloc: BufferAllocator)
fun IndexGpuBuffer.Companion.u16(vararg data: Int): Resource<IndexGpuBuffer> {
  val shortData = ShortArray(data.size) { i -> data[i].toShort() }
  return IndexGpuBuffer.u16(shortData)
}

context(alloc: BufferAllocator)
fun IndexGpuBuffer.Companion.u32(data: IntArray): Resource<IndexGpuBuffer> {
  val res = alloc.static(data, GPUBufferUsage.Index, "Index Buffer")
  return resource {
    val buf = res.bind()
    object : IndexGpuBuffer, GpuBuffer by buf {
      override val indexCount: Int = data.size
      override val indexFormat: GPUIndexFormat = GPUIndexFormat.Uint32
    }
  }
}

// endregion
