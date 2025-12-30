package net.japanesehunter.worldcreate

import arrow.fx.coroutines.Resource
import arrow.fx.coroutines.resource
import net.japanesehunter.math.AngleUnit
import net.japanesehunter.math.Camera
import net.japanesehunter.math.LengthUnit
import net.japanesehunter.math.MovableCamera
import net.japanesehunter.math.meters
import net.japanesehunter.math.normalized
import net.japanesehunter.webgpu.BufferAllocator
import net.japanesehunter.webgpu.CameraGpuBuffer
import net.japanesehunter.webgpu.MutableGpuBuffer
import net.japanesehunter.webgpu.interop.GPUBufferUsage
import kotlin.math.tan

private const val CAMERA_UNIFORM_SIZE_BYTES = 144
private const val ROTATION_STRIDE_FLOATS = 4

context(alloc: BufferAllocator)
fun MovableCamera.toGpuBuffer(
  unit: LengthUnit = LengthUnit.METER,
): Resource<CameraGpuBuffer> {
  val res =
    alloc.mutable(
      data = toCameraUniformBytes(unit),
      usage = GPUBufferUsage.Uniform,
      label = "Camera Uniform Buffer",
    )
  return resource {
    val buf = res.bind()
    object : CameraGpuBuffer, MutableGpuBuffer by buf {
      override fun update() {
        write(toCameraUniformBytes(unit))
      }
    }
  }
}

fun Camera.projectionMatrix(
  unit: LengthUnit = LengthUnit.METER,
): FloatArray {
  val out = FloatArray(16) { 0f }
  val f =
    1.0 /
      tan(
        fov.angle
          .toDouble(AngleUnit.RADIAN) /
          2.0,
      )
  val near =
    nearFar.near
      .toDouble(unit)
  val far =
    nearFar.far
      .toDouble(unit)
  val invRange = 1.0 / (near - far)

  out[0] = (f / aspect).toFloat()
  out[5] = f.toFloat()
  out[11] = -1f
  out[10] = (far * invRange).toFloat()
  out[14] = (near * far * invRange).toFloat()
  return out
}

fun Camera.viewRotationMatrix(): FloatArray {
  val scale = transform.scale
  val invSx = 1.0 / scale.sx
  val invSy = 1.0 / scale.sy
  val invSz = 1.0 / scale.sz
  require(invSx.isFinite() && invSy.isFinite() && invSz.isFinite()) {
    "Cannot invert a camera transform with zero scale."
  }

  val rotation =
    transform.rotation
      .normalized()
  val rx = rotation.x
  val ry = rotation.y
  val rz = rotation.z
  val rw = rotation.w

  val xx = rx * rx
  val yy = ry * ry
  val zz = rz * rz
  val xy = rx * ry
  val xz = rx * rz
  val yz = ry * rz
  val wx = rw * rx
  val wy = rw * ry
  val wz = rw * rz

  val r00 = 1.0 - 2.0 * (yy + zz)
  val r01 = 2.0 * (xy - wz)
  val r02 = 2.0 * (xz + wy)
  val r10 = 2.0 * (xy + wz)
  val r11 = 1.0 - 2.0 * (xx + zz)
  val r12 = 2.0 * (yz - wx)
  val r20 = 2.0 * (xz - wy)
  val r21 = 2.0 * (yz + wx)
  val r22 = 1.0 - 2.0 * (xx + yy)

  val out = FloatArray(ROTATION_STRIDE_FLOATS * 3)
  var offset = 0

  fun writeColumn(
    x: Double,
    y: Double,
    z: Double,
  ) {
    out[offset + 0] = (invSx * x).toFloat()
    out[offset + 1] = (invSy * y).toFloat()
    out[offset + 2] = (invSz * z).toFloat()
    out[offset + 3] = 0f // std140 padding for vec3 column
    offset += ROTATION_STRIDE_FLOATS
  }
  writeColumn(r00, r01, r02)
  writeColumn(r10, r11, r12)
  writeColumn(r20, r21, r22)
  return out
}

private fun MovableCamera.toCameraUniformBytes(
  unit: LengthUnit,
): ByteArray {
  val buffer = ByteArray(CAMERA_UNIFORM_SIZE_BYTES)
  var pos = 0

  fun writeIntLe(
    value: Int,
  ) {
    buffer[pos + 0] = (value ushr 0 and 0xFF).toByte()
    buffer[pos + 1] = (value ushr 8 and 0xFF).toByte()
    buffer[pos + 2] = (value ushr 16 and 0xFF).toByte()
    buffer[pos + 3] = (value ushr 24 and 0xFF).toByte()
    pos += Int.SIZE_BYTES
  }

  fun writeFloatLe(
    value: Float,
  ) {
    writeIntLe(value.toRawBits())
  }

  projectionMatrix(unit).forEach(::writeFloatLe)
  viewRotationMatrix().forEach(::writeFloatLe)

  val translation = transform.translation
  val blockX = translation.dx.inWholeMeters
  val blockY = translation.dy.inWholeMeters
  val blockZ = translation.dz.inWholeMeters
  require(blockX in Int.MIN_VALUE..Int.MAX_VALUE) {
    "Camera block_pos.x out of Int range: $blockX"
  }
  require(blockY in Int.MIN_VALUE..Int.MAX_VALUE) {
    "Camera block_pos.y out of Int range: $blockY"
  }
  require(blockZ in Int.MIN_VALUE..Int.MAX_VALUE) {
    "Camera block_pos.z out of Int range: $blockZ"
  }

  writeIntLe(blockX.toInt())
  writeIntLe(blockY.toInt())
  writeIntLe(blockZ.toInt())
  writeIntLe(0)

  val localX =
    (translation.dx - blockX.meters)
      .toDouble(unit)
      .toFloat()
  val localY =
    (translation.dy - blockY.meters)
      .toDouble(unit)
      .toFloat()
  val localZ =
    (translation.dz - blockZ.meters)
      .toDouble(unit)
      .toFloat()

  writeFloatLe(localX)
  writeFloatLe(localY)
  writeFloatLe(localZ)
  writeIntLe(0)

  check(pos == CAMERA_UNIFORM_SIZE_BYTES) {
    "Camera uniform write mismatch: expected $CAMERA_UNIFORM_SIZE_BYTES bytes, wrote $pos"
  }
  return buffer
}
