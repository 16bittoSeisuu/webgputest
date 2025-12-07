@file:OptIn(ExperimentalAtomicApi::class)
@file:Suppress("NOTHING_TO_INLINE")

package net.japanesehunter.math

import kotlinx.atomicfu.locks.ReentrantLock
import kotlinx.atomicfu.locks.withLock
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.math.tan

// region interfaces

/**
 * Represents a 4x4 column-major matrix using 16 [Double] components.
 * Interprets column vectors (v' = M * v) and matches WebGPU/WebGL layout.
 *
 * @author Int16
 */
sealed interface Matrix4x4 {
  /**
   * Returns the element at [row], [col] (0-based).
   */
  operator fun get(
    row: Int,
    col: Int,
  ): Double

  /**
   * Returns the element at [index] in column-major order.
   */
  operator fun get(index: Int): Double

  /**
   * Returns a copy of the underlying elements in column-major order.
   */
  fun toDoubleArray(): DoubleArray

  override fun toString(): String

  override fun equals(other: Any?): Boolean

  override fun hashCode(): Int

  companion object
}

/**
 * Immutable 4x4 matrix. External implementations are forbidden to allow cached singletons.
 */
sealed interface ImmutableMatrix4x4 : Matrix4x4

/**
 * Mutable 4x4 matrix. The default implementation guards access with a lock.
 */
interface MutableMatrix4x4 : Matrix4x4 {
  /**
   * Updates the element at [row], [col] (0-based).
   */
  operator fun set(
    row: Int,
    col: Int,
    value: Double,
  )

  /**
   * Updates the element at [index] in column-major order.
   */
  operator fun set(
    index: Int,
    value: Double,
  )

  /**
   * Runs [action] while holding the internal lock when available so compound operations stay consistent.
   */
  fun mutate(action: MutableMatrix4x4.() -> Unit) = action(this)

  companion object
}

// endregion

// region constants

/**
 * The identity matrix.
 */
val Matrix4x4.Companion.identity: ImmutableMatrix4x4 get() = MATRIX4X4_IDENTITY

/**
 * The all-zero matrix.
 */
val Matrix4x4.Companion.zero: ImmutableMatrix4x4 get() = MATRIX4X4_ZERO

// endregion

// region factory functions

/**
 * Creates a [Matrix4x4] from row-major arguments. Internally stored as column-major.
 * Optionally accepts [mutator] to build a mutable instance before freezing.
 */
@Suppress("FunctionName")
fun Matrix4x4(
  m00: Double = 1.0,
  m01: Double = 0.0,
  m02: Double = 0.0,
  m03: Double = 0.0,
  m10: Double = 0.0,
  m11: Double = 1.0,
  m12: Double = 0.0,
  m13: Double = 0.0,
  m20: Double = 0.0,
  m21: Double = 0.0,
  m22: Double = 1.0,
  m23: Double = 0.0,
  m30: Double = 0.0,
  m31: Double = 0.0,
  m32: Double = 0.0,
  m33: Double = 1.0,
  mutator: (MutableMatrix4x4.() -> Unit)? = null,
): ImmutableMatrix4x4 {
  if (mutator == null) {
    val elements =
      doubleArrayOf(
        m00,
        m10,
        m20,
        m30,
        m01,
        m11,
        m21,
        m31,
        m02,
        m12,
        m22,
        m32,
        m03,
        m13,
        m23,
        m33,
      )
    return createMatrixImmutable(elements)
  }
  val elements =
    doubleArrayOf(
      m00,
      m10,
      m20,
      m30,
      m01,
      m11,
      m21,
      m31,
      m02,
      m12,
      m22,
      m32,
      m03,
      m13,
      m23,
      m33,
    )
  val impl = ImmutableMatrix4x4Impl(elements)
  val mutableWrapper = Matrix4x4MutableWrapper(impl)
  mutator(mutableWrapper)
  return impl
}

/**
 * Creates an [ImmutableMatrix4x4] by copying an existing one.
 * If the source is already immutable and [mutator] is null, the same instance is returned.
 */
inline fun Matrix4x4.Companion.copyOf(
  matrix: Matrix4x4,
  noinline mutator: (MutableMatrix4x4.() -> Unit)? = null,
): ImmutableMatrix4x4 =
  if (matrix is ImmutableMatrix4x4 && mutator == null) {
    matrix
  } else {
    val e = matrix.toDoubleArray()
    Matrix4x4(
      m00 = e[0],
      m01 = e[4],
      m02 = e[8],
      m03 = e[12],
      m10 = e[1],
      m11 = e[5],
      m12 = e[9],
      m13 = e[13],
      m20 = e[2],
      m21 = e[6],
      m22 = e[10],
      m23 = e[14],
      m30 = e[3],
      m31 = e[7],
      m32 = e[11],
      m33 = e[15],
      mutator = mutator,
    )
  }

/**
 * Creates a [MutableMatrix4x4] from row-major arguments.
 */
fun MutableMatrix4x4(
  m00: Double = 1.0,
  m01: Double = 0.0,
  m02: Double = 0.0,
  m03: Double = 0.0,
  m10: Double = 0.0,
  m11: Double = 1.0,
  m12: Double = 0.0,
  m13: Double = 0.0,
  m20: Double = 0.0,
  m21: Double = 0.0,
  m22: Double = 1.0,
  m23: Double = 0.0,
  m30: Double = 0.0,
  m31: Double = 0.0,
  m32: Double = 0.0,
  m33: Double = 1.0,
): MutableMatrix4x4 =
  MutableMatrix4x4Impl(
    doubleArrayOf(
      m00,
      m10,
      m20,
      m30,
      m01,
      m11,
      m21,
      m31,
      m02,
      m12,
      m22,
      m32,
      m03,
      m13,
      m23,
      m33,
    ),
  )

/**
 * Creates a [MutableMatrix4x4] by copying an existing one.
 */
fun MutableMatrix4x4.Companion.copyOf(matrix: Matrix4x4): MutableMatrix4x4 = MutableMatrix4x4Impl(matrix.toDoubleArray())

// endregion

// region operations

/**
 * Returns a new immutable matrix representing `this * other`.
 */
inline operator fun Matrix4x4.times(other: Matrix4x4): ImmutableMatrix4x4 = multipliedBy(other)

/**
 * Returns the matrix product `this * other` as an [ImmutableMatrix4x4].
 */
inline infix fun Matrix4x4.multipliedBy(other: Matrix4x4): ImmutableMatrix4x4 {
  val a = toDoubleArray()
  val b = other.toDoubleArray()
  val out = DoubleArray(16)
  var col = 0
  while (col < 4) {
    val b0 = b[col * 4 + 0]
    val b1 = b[col * 4 + 1]
    val b2 = b[col * 4 + 2]
    val b3 = b[col * 4 + 3]
    var row = 0
    while (row < 4) {
      val a0 = a[0 * 4 + row]
      val a1 = a[1 * 4 + row]
      val a2 = a[2 * 4 + row]
      val a3 = a[3 * 4 + row]
      out[col * 4 + row] = a0 * b0 + a1 * b1 + a2 * b2 + a3 * b3
      row++
    }
    col++
  }
  return Matrix4x4.fromColumnMajor(out)
}

/**
 * Returns the matrix elements as [FloatArray] in column-major order.
 */
fun Matrix4x4.toFloatArray(): FloatArray {
  val doubles = toDoubleArray()
  val floats = FloatArray(16) { i -> doubles[i].toFloat() }
  return floats
}

/**
 * Sets this matrix to a TRS built from [transform] (`scale -> rotation -> translation`).
 */
fun MutableMatrix4x4.setTransform(
  transform: Transform,
  unit: LengthUnit = LengthUnit.METER,
) = mutateElements { writeTransform(it, transform, unit) }

/**
 * Builds a TRS matrix from [transform], applying `scale -> rotation -> translation`. Column-major, column vectors.
 */
fun Transform.toMatrix4x4(unit: LengthUnit = LengthUnit.METER): ImmutableMatrix4x4 =
  Matrix4x4 {
    setTransform(this@toMatrix4x4, unit)
  }

/**
 * Resets this matrix to the identity matrix.
 */
fun MutableMatrix4x4.setIdentity() =
  mutateElements {
    it.fill(0.0)
    it[0] = 1.0
    it[5] = 1.0
    it[10] = 1.0
    it[15] = 1.0
  }

/**
 * Copies [matrix] into this instance.
 */
fun MutableMatrix4x4.setFrom(matrix: Matrix4x4) = mutateElements { matrix.toDoubleArray().copyInto(it) }

/**
 * Sets this matrix to the product `a * b`.
 */
fun MutableMatrix4x4.setProduct(
  a: Matrix4x4,
  b: Matrix4x4,
) {
  val aArr = a.toDoubleArray()
  val bArr = b.toDoubleArray()
  mutateElements { out ->
    for (col in 0 until 4) {
      val b0 = bArr[col * 4 + 0]
      val b1 = bArr[col * 4 + 1]
      val b2 = bArr[col * 4 + 2]
      val b3 = bArr[col * 4 + 3]
      for (row in 0 until 4) {
        val a0 = aArr[0 * 4 + row]
        val a1 = aArr[1 * 4 + row]
        val a2 = aArr[2 * 4 + row]
        val a3 = aArr[3 * 4 + row]
        out[col * 4 + row] = a0 * b0 + a1 * b1 + a2 * b2 + a3 * b3
      }
    }
  }
}

/**
 * Sets this matrix to a right-handed view-projection (`projection * view`) for [camera] with a `[0, 1]` depth range.
 *
 * @param camera The camera supplying transform, FOV, aspect ratio, and near/far planes.
 * @param unit The length unit used to express translations; [NearFar] distances are converted using the same unit.
 * @throws IllegalArgumentException If any scale component on [Camera.transform] is zero.
 */
fun MutableMatrix4x4.setViewProjRH(
  camera: Camera,
  unit: LengthUnit = LengthUnit.METER,
) = mutateElements { target ->
  val view = DoubleArray(16)
  val proj = DoubleArray(16)
  writeViewMatrix(view, camera.transform, unit)
  writePerspectiveMatrix(proj, camera.fov, camera.aspect, camera.nearFar, unit)
  multiplyInto(target, proj, view)
}

/**
 * Builds a right-handed view-projection matrix (`projection * view`) for [camera] with a `[0, 1]` depth range.
 *
 * @param camera The camera supplying transform, FOV, aspect ratio, and near/far planes.
 * @param unit The length unit used to express translations; [NearFar] distances are converted using the same unit.
 * @throws IllegalArgumentException If any scale component on [Camera.transform] is zero.
 */
fun Matrix4x4.Companion.viewProjRH(
  camera: Camera,
  unit: LengthUnit = LengthUnit.METER,
): ImmutableMatrix4x4 =
  Matrix4x4 {
    setViewProjRH(camera, unit)
  }

// endregion

// region implementations

private val MATRIX4X4_IDENTITY: ImmutableMatrix4x4Impl =
  ImmutableMatrix4x4Impl(
    doubleArrayOf(
      1.0,
      0.0,
      0.0,
      0.0,
      0.0,
      1.0,
      0.0,
      0.0,
      0.0,
      0.0,
      1.0,
      0.0,
      0.0,
      0.0,
      0.0,
      1.0,
    ),
  )
private val MATRIX4X4_ZERO: ImmutableMatrix4x4Impl = ImmutableMatrix4x4Impl(DoubleArray(16))

private data class ImmutableMatrix4x4Impl(
  val elements: DoubleArray,
) : ImmutableMatrix4x4 {
  init {
    validateElements(elements)
  }

  override fun get(
    row: Int,
    col: Int,
  ): Double {
    require(row in 0..3 && col in 0..3) { "Indices out of bounds: row=$row col=$col" }
    return elements[col * 4 + row]
  }

  override fun get(index: Int): Double {
    require(index in 0..15) { "Index out of bounds: $index" }
    return elements[index]
  }

  override fun toDoubleArray(): DoubleArray = elements.copyOf()

  override fun toString(): String =
    buildString {
      append("Matrix4x4(")
      var i = 0
      while (i < 16) {
        append(elements[i])
        if (i != 15) append(", ")
        i++
      }
      append(")")
    }

  override fun equals(other: Any?): Boolean =
    when {
      this === other -> true
      other !is Matrix4x4 -> false
      else -> elementsEqual(this, other)
    }

  override fun hashCode(): Int = elementsHash(elements)
}

private value class Matrix4x4MutableWrapper(
  val impl: ImmutableMatrix4x4Impl,
) : MutableMatrix4x4 {
  override fun get(
    row: Int,
    col: Int,
  ): Double = impl[row, col]

  override fun get(index: Int): Double = impl[index]

  override fun toDoubleArray(): DoubleArray = impl.toDoubleArray()

  override fun set(
    row: Int,
    col: Int,
    value: Double,
  ) {
    val idx = columnMajorIndex(row, col)
    impl.elements[idx] = ensureFiniteMatrixElement(value, "[$row,$col]")
  }

  override fun set(
    index: Int,
    value: Double,
  ) {
    require(index in 0..15) { "Index out of bounds: $index" }
    impl.elements[index] = ensureFiniteMatrixElement(value, "[$index]")
  }
}

private class MutableMatrix4x4Impl(
  elements: DoubleArray,
) : MutableMatrix4x4 {
  private val lock = ReentrantLock()
  val data: DoubleArray = validateElementsCopy(elements)

  override fun get(
    row: Int,
    col: Int,
  ): Double {
    require(row in 0..3 && col in 0..3) { "Indices out of bounds: row=$row col=$col" }
    return lock.withLock { data[col * 4 + row] }
  }

  override fun get(index: Int): Double {
    require(index in 0..15) { "Index out of bounds: $index" }
    return lock.withLock { data[index] }
  }

  override fun toDoubleArray(): DoubleArray = lock.withLock { data.copyOf() }

  override fun set(
    row: Int,
    col: Int,
    value: Double,
  ) {
    require(row in 0..3 && col in 0..3) { "Indices out of bounds: row=$row col=$col" }
    val idx = columnMajorIndex(row, col)
    val finite = ensureFiniteMatrixElement(value, "[$row,$col]")
    lock.withLock {
      data[idx] = finite
    }
  }

  override fun set(
    index: Int,
    value: Double,
  ) {
    require(index in 0..15) { "Index out of bounds: $index" }
    val finite = ensureFiniteMatrixElement(value, "[$index]")
    lock.withLock {
      data[index] = finite
    }
  }

  override fun mutate(action: MutableMatrix4x4.() -> Unit) {
    lock.withLock { action(this) }
  }

  override fun equals(other: Any?): Boolean =
    when {
      this === other -> true
      other !is Matrix4x4 -> false
      else -> elementsEqual(this, other)
    }

  override fun hashCode(): Int = lock.withLock { elementsHash(data) }

  override fun toString(): String = toDoubleArray().contentToString()
}

private inline fun MutableMatrix4x4.mutateElements(crossinline action: (DoubleArray) -> Unit) =
  mutate {
    when (this) {
      is MutableMatrix4x4Impl -> {
        action(data)
      }

      is Matrix4x4MutableWrapper -> {
        action(impl.elements)
      }

      else -> {
        val temp = toDoubleArray()
        action(temp)
        var i = 0
        while (i < 16) {
          this[i] = temp[i]
          i++
        }
      }
    }
  }

private fun columnMajorIndex(
  row: Int,
  col: Int,
): Int = col * 4 + row

private fun elementsEqual(
  a: Matrix4x4,
  b: Matrix4x4,
): Boolean {
  val ea = a.toDoubleArray()
  val eb = b.toDoubleArray()
  var i = 0
  while (i < 16) {
    if (ea[i] != eb[i]) return false
    i++
  }
  return true
}

private fun elementsHash(elements: DoubleArray): Int {
  var result = 17
  var i = 0
  while (i < 16) {
    result = 31 * result + elements[i].hashCode()
    i++
  }
  return result
}

@PublishedApi
internal fun ensureFiniteMatrixElement(
  value: Double,
  label: String,
): Double {
  require(value.isFinite()) { "Matrix4x4 element $label must be finite: $value" }
  return value
}

private fun validateElements(elements: DoubleArray) {
  require(elements.size == 16) { "Matrix4x4 requires 16 elements, was ${elements.size}" }
  var i = 0
  while (i < 16) {
    ensureFiniteMatrixElement(elements[i], "[$i]")
    i++
  }
}

private fun validateElementsCopy(elements: DoubleArray): DoubleArray {
  validateElements(elements)
  return elements.copyOf()
}

private fun createMatrixImmutable(elements: DoubleArray): ImmutableMatrix4x4Impl {
  validateElements(elements)
  return when {
    elements.contentEquals(MATRIX4X4_IDENTITY.elements) -> MATRIX4X4_IDENTITY
    elements.contentEquals(MATRIX4X4_ZERO.elements) -> MATRIX4X4_ZERO
    else -> ImmutableMatrix4x4Impl(elements.copyOf())
  }
}

@PublishedApi
internal fun Matrix4x4.Companion.fromColumnMajor(elements: DoubleArray): ImmutableMatrix4x4 = createMatrixImmutable(elements)

private fun writeViewMatrix(
  target: DoubleArray,
  transform: Transform,
  unit: LengthUnit,
) {
  require(target.size == 16) { "View matrix target must have 16 elements." }
  val invSx = 1.0 / transform.scale.sx
  val invSy = 1.0 / transform.scale.sy
  val invSz = 1.0 / transform.scale.sz
  require(invSx.isFinite() && invSy.isFinite() && invSz.isFinite()) { "Cannot invert a camera transform with zero scale." }

  val normalizedRotation = transform.rotation.normalized()
  val rx = normalizedRotation.x
  val ry = normalizedRotation.y
  val rz = normalizedRotation.z
  val rw = normalizedRotation.w

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
  val r01 = 2.0 * (xy + wz)
  val r02 = 2.0 * (xz - wy)
  val r10 = 2.0 * (xy - wz)
  val r11 = 1.0 - 2.0 * (xx + zz)
  val r12 = 2.0 * (yz + wx)
  val r20 = 2.0 * (xz + wy)
  val r21 = 2.0 * (yz - wx)
  val r22 = 1.0 - 2.0 * (xx + yy)

  target[0] = invSx * r00
  target[1] = invSy * r01
  target[2] = invSz * r02
  target[3] = 0.0

  target[4] = invSx * r10
  target[5] = invSy * r11
  target[6] = invSz * r12
  target[7] = 0.0

  target[8] = invSx * r20
  target[9] = invSy * r21
  target[10] = invSz * r22
  target[11] = 0.0

  val tx = transform.translation.dx.toDouble(unit)
  val ty = transform.translation.dy.toDouble(unit)
  val tz = transform.translation.dz.toDouble(unit)

  val viewTx = -(r00 * tx + r10 * ty + r20 * tz) * invSx
  val viewTy = -(r01 * tx + r11 * ty + r21 * tz) * invSy
  val viewTz = -(r02 * tx + r12 * ty + r22 * tz) * invSz

  target[12] = viewTx
  target[13] = viewTy
  target[14] = viewTz
  target[15] = 1.0
}

private fun writePerspectiveMatrix(
  target: DoubleArray,
  fov: Fov,
  aspect: Double,
  nearFar: NearFar,
  unit: LengthUnit,
) {
  require(target.size == 16) { "Projection matrix target must have 16 elements." }
  target.fill(0.0)

  val f = 1.0 / tan(fov.angle.toDouble(AngleUnit.RADIAN) / 2.0)
  val near = nearFar.near.toDouble(unit)
  val far = nearFar.far.toDouble(unit)

  target[0] = f / aspect
  target[5] = f
  target[11] = -1.0

  val invRange = 1.0 / (near - far)
  target[10] = far * invRange
  target[14] = near * far * invRange
}

private fun multiplyInto(
  target: DoubleArray,
  a: DoubleArray,
  b: DoubleArray,
) {
  require(target.size == 16 && a.size == 16 && b.size == 16) { "All matrices must have 16 elements." }
  for (col in 0 until 4) {
    val b0 = b[col * 4 + 0]
    val b1 = b[col * 4 + 1]
    val b2 = b[col * 4 + 2]
    val b3 = b[col * 4 + 3]
    for (row in 0 until 4) {
      val a0 = a[0 * 4 + row]
      val a1 = a[1 * 4 + row]
      val a2 = a[2 * 4 + row]
      val a3 = a[3 * 4 + row]
      target[col * 4 + row] = a0 * b0 + a1 * b1 + a2 * b2 + a3 * b3
    }
  }
}

private fun writeTransform(
  target: DoubleArray,
  transform: Transform,
  unit: LengthUnit,
) {
  val (sx, sy, sz) = transform.scale
  val normalizedRotation = transform.rotation.normalized()
  val rx = normalizedRotation.x
  val ry = normalizedRotation.y
  val rz = normalizedRotation.z
  val rw = normalizedRotation.w
  val tx = transform.translation.dx.toDouble(unit)
  val ty = transform.translation.dy.toDouble(unit)
  val tz = transform.translation.dz.toDouble(unit)

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
  val r01 = 2.0 * (xy + wz)
  val r02 = 2.0 * (xz - wy)
  val r10 = 2.0 * (xy - wz)
  val r11 = 1.0 - 2.0 * (xx + zz)
  val r12 = 2.0 * (yz + wx)
  val r20 = 2.0 * (xz + wy)
  val r21 = 2.0 * (yz - wx)
  val r22 = 1.0 - 2.0 * (xx + yy)

  target[0] = r00 * sx
  target[1] = r10 * sx
  target[2] = r20 * sx
  target[3] = 0.0

  target[4] = r01 * sy
  target[5] = r11 * sy
  target[6] = r21 * sy
  target[7] = 0.0

  target[8] = r02 * sz
  target[9] = r12 * sz
  target[10] = r22 * sz
  target[11] = 0.0

  target[12] = tx
  target[13] = ty
  target[14] = tz
  target[15] = 1.0
}

// endregion
