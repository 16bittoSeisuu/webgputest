package net.japanesehunter.worldcreate

import arrow.fx.coroutines.ResourceScope
import kotlinx.coroutines.yield
import kotlinx.io.Buffer
import kotlinx.io.readByteString
import kotlinx.io.writeFloatLe
import kotlinx.io.writeIntLe
import net.japanesehunter.GpuVertexFormat
import net.japanesehunter.math.Length
import net.japanesehunter.math.LengthUnit
import net.japanesehunter.math.MutableLength3
import net.japanesehunter.math.meters
import net.japanesehunter.math.plus
import net.japanesehunter.webgpu.BufferAllocator
import net.japanesehunter.webgpu.GpuBuffer
import net.japanesehunter.webgpu.IndexGpuBuffer
import net.japanesehunter.webgpu.VertexGpuBuffer
import net.japanesehunter.webgpu.interop.GPUBufferUsage
import net.japanesehunter.webgpu.interop.GPUIndexFormat

context(alloc: BufferAllocator, resource: ResourceScope)
suspend fun List<List<List<BlockState>>>.toMeshGpuBuffer(): Pair<
  VertexGpuBuffer,
  IndexGpuBuffer,
> {
  val world = this
  val quads =
    buildList {
      var x = 0
      var y = 0
      var z = 0
      val requiredOpaqueNeighbors = mutableSetOf<BlockFace>()
      val actualOpaqueNeighbors = mutableSetOf<BlockFace>()

      fun setNeighborOpaqueFaces(required: Set<BlockFace>) {
        actualOpaqueNeighbors.clear()
        for (face in required) {
          val nx = x + face.normal.ux.toInt()
          val ny = y + face.normal.uy.toInt()
          val nz = z + face.normal.uz.toInt()
          val neighbor = world.getOrNull(nx)?.getOrNull(ny)?.getOrNull(nz)
          if (neighbor?.isOpaque(face.opposite()) == true) {
            actualOpaqueNeighbors.add(face)
          }
        }
      }

      val tmpLen = MutableLength3()
      val opaqueFaceSink =
        QuadSink.OpaqueFaceSink {
          requiredOpaqueNeighbors.add(it)
        }
      val sink =
        QuadSink { quad, cullReq ->
          requiredOpaqueNeighbors.clear()
          opaqueFaceSink.cullReq()
          setNeighborOpaqueFaces(requiredOpaqueNeighbors)
          val shouldCull =
            actualOpaqueNeighbors.containsAll(requiredOpaqueNeighbors)

          if (!shouldCull) {
            val offset =
              tmpLen.apply {
                dx = x.meters
                dy = y.meters
                dz = z.meters
              }
            val v0 = quad.min + quad.v + offset
            val v1 = quad.max + offset
            val v2 = quad.min + offset
            val v3 = quad.min + quad.u + offset
            add(
              listOf(
                v0 to (0f to 1f),
                v1 to (1f to 1f),
                v2 to (0f to 0f),
                v3 to (1f to 0f),
              ),
            )
          }
        }
      for (plane in world) {
        for (line in plane) {
          for (block in line) {
            with(block) {
              sink.emitQuads()
            }
            yield()
            z++
          }
          z = 0
          y++
        }
        y = 0
        x++
      }
    }
  val bytes = Buffer()

  val vertices = quads.flatten().toSet()
  vertices.forEach { (vertex, uv) ->
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
    yield()
  }
  val vertexData = bytes.readByteString()
  quads.forEach { (v0, v1, v2, v3) ->
    val v0Index = vertices.indexOf(v0)
    val v1Index = vertices.indexOf(v1)
    val v2Index = vertices.indexOf(v2)
    val v3Index = vertices.indexOf(v3)
    bytes.writeIntLe(v0Index)
    bytes.writeIntLe(v1Index)
    bytes.writeIntLe(v2Index)
    bytes.writeIntLe(v1Index)
    bytes.writeIntLe(v3Index)
    bytes.writeIntLe(v2Index)
    yield()
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
