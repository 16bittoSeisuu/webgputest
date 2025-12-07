@file:OptIn(ExperimentalAtomicApi::class)

package net.japanesehunter.webgpu

import arrow.fx.coroutines.Resource
import arrow.fx.coroutines.resource
import net.japanesehunter.math.Camera
import net.japanesehunter.math.LengthUnit
import net.japanesehunter.math.MutableMatrix4x4
import net.japanesehunter.math.setIdentity
import net.japanesehunter.math.setViewProjRH
import net.japanesehunter.math.toFloatArray
import net.japanesehunter.webgpu.interop.GPUBufferBinding
import net.japanesehunter.webgpu.interop.GPUBufferUsage
import net.japanesehunter.webgpu.interop.GPUIndexFormat
import net.japanesehunter.webgpu.interop.GPUVertexFormat
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.update

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

// region uniform

interface UniformGpuBuffer : GpuBuffer {
  companion object
}

interface CameraGpuBuffer :
  UniformGpuBuffer,
  MutableGpuBuffer {
  fun update()
}

private val tmpMatrix = AtomicReference(MutableMatrix4x4())

context(alloc: BufferAllocator)
fun UniformGpuBuffer.Companion.camera(
  data: Camera,
  unit: LengthUnit = LengthUnit.METER,
): Resource<CameraGpuBuffer> {
  val res =
    alloc.mutable(
      data = data.toFloatArray(unit),
      usage = GPUBufferUsage.Uniform,
      label = "Camera View Proj Uniform Buffer",
    )
  return resource {
    val buf = res.bind()
    object : CameraGpuBuffer, MutableGpuBuffer by buf {
      override fun update() {
        val array = data.toFloatArray(unit)
        write(array)
      }
    }
  }
}

private fun Camera.toFloatArray(unit: LengthUnit): FloatArray {
  tmpMatrix.update {
    it.apply {
      setIdentity()
      setViewProjRH(
        camera = this@toFloatArray,
        unit = unit,
      )
      return toFloatArray()
    }
  }
}

// endregion

// region binding

fun UniformGpuBuffer.asBinding(): GPUBufferBinding =
  GPUBufferBinding(
    buffer = raw,
    offset = offset,
    size = size,
  )

// endregion
