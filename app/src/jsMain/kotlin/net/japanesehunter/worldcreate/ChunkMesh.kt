package net.japanesehunter.worldcreate

import arrow.fx.coroutines.ResourceScope
import kotlinx.io.Buffer
import kotlinx.io.readByteString
import kotlinx.io.writeFloatLe
import kotlinx.io.writeIntLe
import net.japanesehunter.GpuVertexFormat
import net.japanesehunter.math.LengthUnit
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
  val nanosPerMeter = LengthUnit.METER.nanometersPerUnit.toInt()
  val nanosToMeters = 1e-9f

  fun appendVertex(
    blockX: Int,
    blockY: Int,
    blockZ: Int,
    localXNanometers: Int,
    localYNanometers: Int,
    localZNanometers: Int,
    uv: Pair<Float, Float>,
  ): Int {
    var wholeX = blockX
    var wholeY = blockY
    var wholeZ = blockZ

    var subXNanometers = localXNanometers
    var subYNanometers = localYNanometers
    var subZNanometers = localZNanometers

    if (subXNanometers >= nanosPerMeter) {
      val carry = subXNanometers / nanosPerMeter
      wholeX += carry
      subXNanometers -= carry * nanosPerMeter
    } else if (subXNanometers < 0) {
      val carry = (-subXNanometers + (nanosPerMeter - 1)) / nanosPerMeter
      wholeX -= carry
      subXNanometers += carry * nanosPerMeter
    }

    if (subYNanometers >= nanosPerMeter) {
      val carry = subYNanometers / nanosPerMeter
      wholeY += carry
      subYNanometers -= carry * nanosPerMeter
    } else if (subYNanometers < 0) {
      val carry = (-subYNanometers + (nanosPerMeter - 1)) / nanosPerMeter
      wholeY -= carry
      subYNanometers += carry * nanosPerMeter
    }

    if (subZNanometers >= nanosPerMeter) {
      val carry = subZNanometers / nanosPerMeter
      wholeZ += carry
      subZNanometers -= carry * nanosPerMeter
    } else if (subZNanometers < 0) {
      val carry = (-subZNanometers + (nanosPerMeter - 1)) / nanosPerMeter
      wholeZ -= carry
      subZNanometers += carry * nanosPerMeter
    }

    val subX = subXNanometers.toFloat() * nanosToMeters
    val subY = subYNanometers.toFloat() * nanosToMeters
    val subZ = subZNanometers.toFloat() * nanosToMeters

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
        val min = quad.min
        val minX = min.x.inWholeNanometers.toInt()
        val minY = min.y.inWholeNanometers.toInt()
        val minZ = min.z.inWholeNanometers.toInt()

        val u = quad.u
        val uX = u.dx.inWholeNanometers.toInt()
        val uY = u.dy.inWholeNanometers.toInt()
        val uZ = u.dz.inWholeNanometers.toInt()

        val v = quad.v
        val vX = v.dx.inWholeNanometers.toInt()
        val vY = v.dy.inWholeNanometers.toInt()
        val vZ = v.dz.inWholeNanometers.toInt()

        val x0 = minX + vX
        val y0 = minY + vY
        val z0 = minZ + vZ

        val x1 = minX + uX + vX
        val y1 = minY + uY + vY
        val z1 = minZ + uZ + vZ

        val x2 = minX
        val y2 = minY
        val z2 = minZ

        val x3 = minX + uX
        val y3 = minY + uY
        val z3 = minZ + uZ

        val i0 = appendVertex(x, y, z, x0, y0, z0, 0f to 1f)
        val i1 = appendVertex(x, y, z, x1, y1, z1, 1f to 1f)
        val i2 = appendVertex(x, y, z, x2, y2, z2, 0f to 0f)
        val i3 = appendVertex(x, y, z, x3, y3, z3, 1f to 0f)

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
