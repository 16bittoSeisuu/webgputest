package net.japanesehunter.worldcreate

import arrow.fx.coroutines.ResourceScope
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
suspend fun List<GreedyQuad>.toGpuBuffer(): Pair<VertexGpuBuffer, IndexGpuBuffer> {
  val vertices =
    map {
      listOf(
        it.shape.min + it.shape.v to (0f to 1f),
        it.shape.max to (1f to 1f),
        it.shape.min to (0f to 0f),
        it.shape.min + it.shape.u to (1f to 0f),
      )
    }
  val verticesDistinct =
    vertices.flatten().toSet()
  val vertexArray =
    ByteArray(
      verticesDistinct.size *
        (
          3 * Int.SIZE_BYTES +
            3 * Float.SIZE_BYTES +
            2 * Float.SIZE_BYTES
        ),
    )
  val indexArray =
    IntArray(vertices.size * 6)
  var pos = 0

  fun ByteArray.writeFloatLe(value: Float) {
    val bits = value.toRawBits()
    this[pos++] = (bits and 0xFF).toByte()
    this[pos++] = ((bits ushr 8) and 0xFF).toByte()
    this[pos++] = ((bits ushr 16) and 0xFF).toByte()
    this[pos++] = ((bits ushr 24) and 0xFF).toByte()
  }

  fun ByteArray.writeIntLe(value: Int) {
    this[pos++] = (value and 0xFF).toByte()
    this[pos++] = ((value ushr 8) and 0xFF).toByte()
    this[pos++] = ((value ushr 16) and 0xFF).toByte()
    this[pos++] = ((value ushr 24) and 0xFF).toByte()
  }

  verticesDistinct.forEach { (vertex, uv) ->
    fun Length.inWholeMeters(): Int = inWholeMeters.toInt()

    fun Length.subMeterToFloat(): Float = (toDouble(LengthUnit.METER) - inWholeMeters).toFloat()

    vertexArray.writeIntLe(vertex.x.inWholeMeters())
    vertexArray.writeIntLe(vertex.y.inWholeMeters())
    vertexArray.writeIntLe(vertex.z.inWholeMeters())
    vertexArray.writeFloatLe(vertex.x.subMeterToFloat())
    vertexArray.writeFloatLe(vertex.y.subMeterToFloat())
    vertexArray.writeFloatLe(vertex.z.subMeterToFloat())
    vertexArray.writeFloatLe(uv.first)
    vertexArray.writeFloatLe(uv.second)
  }
  pos = 0
  vertices.forEach { (v0, v1, v2, v3) ->
    val v0Index = verticesDistinct.indexOf(v0)
    val v1Index = verticesDistinct.indexOf(v1)
    val v2Index = verticesDistinct.indexOf(v2)
    val v3Index = verticesDistinct.indexOf(v3)
    indexArray[pos++] = v0Index
    indexArray[pos++] = v1Index
    indexArray[pos++] = v2Index
    indexArray[pos++] = v1Index
    indexArray[pos++] = v3Index
    indexArray[pos++] = v2Index
  }
  check(pos == indexArray.size)

  val vBuf =
    with(resource) {
      alloc
        .static(
          data = vertexArray,
          usage = GPUBufferUsage.Vertex,
          label = "Triangle Vertex Buffer",
        ).bind()
    }
  val iBuf =
    with(resource) {
      alloc
        .static(
          data = indexArray,
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
      override val indexCount: Int = indexArray.size
      override val indexFormat: GPUIndexFormat = GPUIndexFormat.Uint32
    }
}
