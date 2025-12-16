package net.japanesehunter.worldcreate

import arrow.fx.coroutines.ResourceScope
import kotlinx.io.Buffer
import kotlinx.io.readByteString
import kotlinx.io.writeFloatLe
import kotlinx.io.writeIntLe
import net.japanesehunter.GpuVertexFormat
import net.japanesehunter.math.Length
import net.japanesehunter.math.LengthUnit
import net.japanesehunter.math.plus
import net.japanesehunter.webgpu.BufferAllocator
import net.japanesehunter.webgpu.GpuBuffer
import net.japanesehunter.webgpu.IndexGpuBuffer
import net.japanesehunter.webgpu.VertexGpuBuffer
import net.japanesehunter.webgpu.interop.GPUBufferUsage
import net.japanesehunter.webgpu.interop.GPUIndexFormat

context(alloc: BufferAllocator, resource: ResourceScope)
suspend fun List<MaterialQuad>.toGpuBuffer(): Pair<VertexGpuBuffer, IndexGpuBuffer> {
  val vertices =
    map {
      listOf(
        it.min + it.v to (0f to 1f),
        it.max to (1f to 1f),
        it.min to (0f to 0f),
        it.min + it.u to (1f to 0f),
      )
    }
  val verticesDistinct =
    vertices.flatten().toSet()
  val bytes = Buffer()

  verticesDistinct.forEach { (vertex, uv) ->
    fun Length.inWholeMeters(): Int = inWholeMeters.toInt()

    fun Length.subMeterToFloat(): Float = (toDouble(LengthUnit.METER) - inWholeMeters).toFloat()

    bytes.writeIntLe(vertex.x.inWholeMeters())
    bytes.writeIntLe(vertex.y.inWholeMeters())
    bytes.writeIntLe(vertex.z.inWholeMeters())
    bytes.writeFloatLe(vertex.x.subMeterToFloat())
    bytes.writeFloatLe(vertex.y.subMeterToFloat())
    bytes.writeFloatLe(vertex.z.subMeterToFloat())
    bytes.writeFloatLe(uv.first)
    bytes.writeFloatLe(uv.second)
  }
  val vertexData = bytes.readByteString()
  vertices.forEach { (v0, v1, v2, v3) ->
    val v0Index = verticesDistinct.indexOf(v0)
    val v1Index = verticesDistinct.indexOf(v1)
    val v2Index = verticesDistinct.indexOf(v2)
    val v3Index = verticesDistinct.indexOf(v3)
    bytes.writeIntLe(v0Index)
    bytes.writeIntLe(v1Index)
    bytes.writeIntLe(v2Index)
    bytes.writeIntLe(v1Index)
    bytes.writeIntLe(v3Index)
    bytes.writeIntLe(v2Index)
  }
  val indexData = bytes.readByteString()

  val vBuf =
    with(resource) {
      alloc
        .static(
          data = vertexData,
          usage = GPUBufferUsage.Vertex,
          label = "Triangle Vertex Buffer",
        ).bind()
    }
  val iBuf =
    with(resource) {
      alloc
        .static(
          data = indexData,
          usage = GPUBufferUsage.Index,
          label = "Triangle Index Buffer",
        ).bind()
    }

  return object : VertexGpuBuffer, GpuBuffer by vBuf {
    override val formats =
      listOf(
        GpuVertexFormat.Sint32x3, // block pos
        GpuVertexFormat.Float32x3, // local pos
        GpuVertexFormat.Float32x2, // uv
      )
  } to
    object : IndexGpuBuffer, GpuBuffer by iBuf {
      override val indexCount: Int = indexData.size / Int.SIZE_BYTES
      override val indexFormat: GPUIndexFormat = GPUIndexFormat.Uint32
    }
}
