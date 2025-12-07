package net.japanesehunter.worldcreate

import arrow.fx.coroutines.Resource
import arrow.fx.coroutines.resource
import net.japanesehunter.GpuVertexFormat
import net.japanesehunter.math.Direction3
import net.japanesehunter.math.Length
import net.japanesehunter.math.Length3
import net.japanesehunter.math.LengthUnit
import net.japanesehunter.math.Point3
import net.japanesehunter.math.cross
import net.japanesehunter.math.meters
import net.japanesehunter.math.minus
import net.japanesehunter.webgpu.BufferAllocator
import net.japanesehunter.webgpu.GpuBuffer
import net.japanesehunter.webgpu.InstanceGpuBuffer
import net.japanesehunter.webgpu.StorageGpuBuffer
import net.japanesehunter.webgpu.interop.GPUBufferUsage

data class Quad(
  val pos: Point3,
  val normal: Direction3,
  val tangent: Direction3,
  val negateBitangent: Boolean,
  val sizeU: Length,
  val sizeV: Length,
  val materialId: Int,
) {
  init {
    require(pos.x < World.MAX_SIZE) {
      "pos.x must be less than ${World.MAX_SIZE}, but got: $pos"
    }
    require(pos.y < World.MAX_SIZE) {
      "pos.y must be less than ${World.MAX_SIZE}, but got: $pos"
    }
    require(pos.z < World.MAX_SIZE) {
      "pos.z must be less than ${World.MAX_SIZE}, but got: $pos"
    }
    try {
      normal cross tangent
    } catch (_: Throwable) {
      error(
        "normal and tangent must not be parallel, but got: " +
          "normal=$normal, tangent=$tangent",
      )
    }
    require(sizeU in Length.ZERO..<World.MAX_SIZE) {
      "sizeU must be in [0, ${World.MAX_SIZE}), but got: $sizeU"
    }
    require(sizeV in Length.ZERO..<World.MAX_SIZE) {
      "sizeV must be in [0, ${World.MAX_SIZE}), but got: $sizeV"
    }
  }
}

context(alloc: BufferAllocator)
fun List<Quad>.toGpuBuffer(): Resource<StorageGpuBuffer> {
  val stride = 64

  fun ByteArray.write(
    offset: Int,
    quad: Quad,
  ) {
    var pos = 0

    fun writeIntLe(value: Int) {
      this[offset + pos + 0] = (value ushr 0 and 0xFF).toByte()
      this[offset + pos + 1] = (value ushr 8 and 0xFF).toByte()
      this[offset + pos + 2] = (value ushr 16 and 0xFF).toByte()
      this[offset + pos + 3] = (value ushr 24 and 0xFF).toByte()
      // Kotlin being too dumb
      @Suppress("AssignedValueIsNeverRead")
      pos += 4
    }

    fun writeFloatLe(value: Float) {
      val intBits = value.toRawBits()
      writeIntLe(intBits)
    }

    val blockPosX = quad.pos.x.inWholeMeters
    val blockPosY = quad.pos.y.inWholeMeters
    val blockPosZ = quad.pos.z.inWholeMeters
    val localPos = quad.pos - Length3(blockPosX.meters, blockPosY.meters, blockPosZ.meters)
    val localPosX = localPos.x.toDouble(LengthUnit.METER).toFloat()
    val localPosY = localPos.y.toDouble(LengthUnit.METER).toFloat()
    val localPosZ = localPos.z.toDouble(LengthUnit.METER).toFloat()
    val normalX = quad.normal.ux.toFloat()
    val normalY = quad.normal.uy.toFloat()
    val normalZ = quad.normal.uz.toFloat()
    val tangentX = quad.tangent.ux.toFloat()
    val tangentY = quad.tangent.uy.toFloat()
    val tangentZ = quad.tangent.uz.toFloat()
    val tangentW = if (quad.negateBitangent) -1f else 1f
    val sizeUX = quad.sizeU.toDouble(LengthUnit.METER).toFloat()
    val sizeVY = quad.sizeV.toDouble(LengthUnit.METER).toFloat()
    // world_pos.x
    writeIntLe(blockPosX.toInt())
    // world_pos.y
    writeIntLe(blockPosY.toInt())
    // world_pos.z
    writeIntLe(blockPosZ.toInt())
    // quad_size.u
    writeFloatLe(sizeUX)
    // local_pos.x
    writeFloatLe(localPosX)
    // local_pos.y
    writeFloatLe(localPosY)
    // local_pos.z
    writeFloatLe(localPosZ)
    // quad_size.v
    writeFloatLe(sizeVY)
    // normal.x
    writeFloatLe(normalX)
    // normal.y
    writeFloatLe(normalY)
    // normal.z
    writeFloatLe(normalZ)
    // bitangent_sign
    writeFloatLe(tangentW)
    // tangent.x
    writeFloatLe(tangentX)
    // tangent.y
    writeFloatLe(tangentY)
    // tangent.z
    writeFloatLe(tangentZ)
    // mat_id
    writeIntLe(quad.materialId)
  }

  val array = ByteArray(size * stride)
  this.forEachIndexed { index, quad ->
    array.write(index * stride, quad)
  }
  val res =
    alloc.static(
      data = array,
      usage = GPUBufferUsage.Storage,
      label = "Quad Instance Buffer",
    )
  return resource {
    val buf = res.bind()
    object : StorageGpuBuffer, GpuBuffer by buf {}
  }
}

context(alloc: BufferAllocator)
fun List<Quad>.toIndicesGpuBuffer(): Resource<InstanceGpuBuffer> {
  val indices = IntArray(size) { it }
  val res =
    alloc.static(
      data = indices,
      usage = GPUBufferUsage.Vertex,
      label = "Quad Indices Buffer",
    )
  return resource {
    val buf = res.bind()
    object : InstanceGpuBuffer, GpuBuffer by buf {
      override val formats = listOf(GpuVertexFormat.Uint32)
    }
  }
}
