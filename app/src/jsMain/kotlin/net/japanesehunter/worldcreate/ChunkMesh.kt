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
  val faces = BlockFace.entries
  val faceOffsetsX = IntArray(faces.size) { i -> faces[i].normal.ux.toInt() }
  val faceOffsetsY = IntArray(faces.size) { i -> faces[i].normal.uy.toInt() }
  val faceOffsetsZ = IntArray(faces.size) { i -> faces[i].normal.uz.toInt() }
  val faceOpposites = Array(faces.size) { i -> faces[i].opposite() }
  val faceBits = IntArray(faces.size) { i -> 1 shl i }

  val vertexBytes = Buffer()
  val indexBytes = Buffer()
  var vertexCount = 0

  fun appendVertex(
    vertex: Point3,
    uv: Pair<Float, Float>,
  ): Int {
    val mx = vertex.x.toDouble(LengthUnit.METER)
    val my = vertex.y.toDouble(LengthUnit.METER)
    val mz = vertex.z.toDouble(LengthUnit.METER)

    val wholeX = mx.toInt()
    val wholeY = my.toInt()
    val wholeZ = mz.toInt()

    val subX = (mx - wholeX).toFloat()
    val subY = (my - wholeY).toFloat()
    val subZ = (mz - wholeZ).toFloat()

    vertexBytes.writeIntLe(wholeX)
    vertexBytes.writeIntLe(wholeY)
    vertexBytes.writeIntLe(wholeZ)
    vertexBytes.writeFloatLe(subX)
    vertexBytes.writeFloatLe(subY)
    vertexBytes.writeFloatLe(subZ)
    vertexBytes.writeFloatLe(uv.first)
    vertexBytes.writeFloatLe(uv.second)

    return vertexCount++
  }

  var x = 0
  var y = 0
  var z = 0
  var requiredOpaqueMask = 0

  fun computeNeighborOpaqueMask(requiredMask: Int): Int {
    if (requiredMask == 0) return 0
    var mask = 0
    val worldSizeX = world.size
    val x0 = x
    val y0 = y
    val z0 = z
    for (i in faces.indices) {
      val bit = faceBits[i]
      if (requiredMask and bit == 0) continue
      val nx = x0 + faceOffsetsX[i]
      val ny = y0 + faceOffsetsY[i]
      val nz = z0 + faceOffsetsZ[i]
      if (nx < 0 || nx >= worldSizeX) continue
      val plane = world[nx]
      if (ny < 0 || ny >= plane.size) continue
      val line = plane[ny]
      if (nz < 0 || nz >= line.size) continue
      val neighbor = line[nz]
      if (neighbor.isOpaque(faceOpposites[i])) {
        mask = mask or bit
      }
    }
    return mask
  }

  val tmpLen = MutableLength3()
  val opaqueFaceSink =
    QuadSink.OpaqueFaceSink {
      requiredOpaqueMask = requiredOpaqueMask or faceBits[it.ordinal]
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

        val i0 = appendVertex(v0, 0f to 1f)
        val i1 = appendVertex(v1, 1f to 1f)
        val i2 = appendVertex(v2, 0f to 0f)
        val i3 = appendVertex(v3, 1f to 0f)

        indexBytes.writeIntLe(i0)
        indexBytes.writeIntLe(i1)
        indexBytes.writeIntLe(i2)
        indexBytes.writeIntLe(i1)
        indexBytes.writeIntLe(i3)
        indexBytes.writeIntLe(i2)
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

  val vertexData = vertexBytes.readByteString()
  val indexData = indexBytes.readByteString()

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
