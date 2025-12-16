package net.japanesehunter.worldcreate

import arrow.fx.coroutines.ResourceScope
import kotlinx.io.Buffer
import kotlinx.io.readByteString
import kotlinx.io.writeFloatLe
import kotlinx.io.writeIntLe
import net.japanesehunter.GpuVertexFormat
import net.japanesehunter.math.LengthUnit
import net.japanesehunter.math.MutableLength3
import net.japanesehunter.math.Point3
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
  val vertexIndices = LinkedHashMap<Pair<Point3, Pair<Float, Float>>, Int>()
  val vertices = mutableListOf<Pair<Point3, Pair<Float, Float>>>()
  val indexList = mutableListOf<Int>()

  fun indexFor(
    vertex: Point3,
    uv: Pair<Float, Float>,
  ): Int {
    val key = vertex to uv
    val existing = vertexIndices[key]
    if (existing != null) return existing
    val newIndex = vertices.size
    vertexIndices[key] = newIndex
    vertices.add(key)
    return newIndex
  }

  var x = 0
  var y = 0
  var z = 0
  var requiredOpaqueMask = 0

  fun faceMask(face: BlockFace): Int = 1 shl face.ordinal

  fun computeNeighborOpaqueMask(requiredMask: Int): Int {
    var mask = 0
    for (face in BlockFace.entries) {
      val bit = faceMask(face)
      if (requiredMask and bit == 0) continue
      val nx = x + face.normal.ux.toInt()
      val ny = y + face.normal.uy.toInt()
      val nz = z + face.normal.uz.toInt()
      val neighbor = world.getOrNull(nx)?.getOrNull(ny)?.getOrNull(nz)
      if (neighbor?.isOpaque(face.opposite()) == true) {
        mask = mask or bit
      }
    }
    return mask
  }

  val tmpLen = MutableLength3()
  val opaqueFaceSink =
    QuadSink.OpaqueFaceSink {
      requiredOpaqueMask = requiredOpaqueMask or faceMask(it)
    }
  val sink =
    QuadSink { quad, cullReq ->
      requiredOpaqueMask = 0
      opaqueFaceSink.cullReq()
      val actualOpaqueMask = computeNeighborOpaqueMask(requiredOpaqueMask)
      val shouldCull = (actualOpaqueMask and requiredOpaqueMask) == requiredOpaqueMask

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
        val i0 = indexFor(v0, 0f to 1f)
        val i1 = indexFor(v1, 1f to 1f)
        val i2 = indexFor(v2, 0f to 0f)
        val i3 = indexFor(v3, 1f to 0f)
        indexList.add(i0)
        indexList.add(i1)
        indexList.add(i2)
        indexList.add(i1)
        indexList.add(i3)
        indexList.add(i2)
      }
    }
  for (plane in world) {
    for (line in plane) {
      for (block in line) {
        with(block) {
          sink.emitQuads()
        }
        z++
      }
      z = 0
      y++
    }
    y = 0
    x++
  }

  val bytes = Buffer()
  vertices.forEach { (vertex, uv) ->
    val mx = vertex.x.toDouble(LengthUnit.METER)
    val my = vertex.y.toDouble(LengthUnit.METER)
    val mz = vertex.z.toDouble(LengthUnit.METER)

    val wholeX = mx.toInt()
    val wholeY = my.toInt()
    val wholeZ = mz.toInt()

    val subX = (mx - wholeX).toFloat()
    val subY = (my - wholeY).toFloat()
    val subZ = (mz - wholeZ).toFloat()

    bytes.writeIntLe(wholeX)
    bytes.writeIntLe(wholeY)
    bytes.writeIntLe(wholeZ)
    bytes.writeFloatLe(subX)
    bytes.writeFloatLe(subY)
    bytes.writeFloatLe(subZ)
    bytes.writeFloatLe(uv.first)
    bytes.writeFloatLe(uv.second)
  }
  val vertexData = bytes.readByteString()
  indexList.forEach { index ->
    bytes.writeIntLe(index)
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
