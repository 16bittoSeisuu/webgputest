@file:OptIn(ExperimentalAtomicApi::class)
@file:Suppress("NOTHING_TO_INLINE")

package net.japanesehunter.math

import kotlinx.atomicfu.locks.ReentrantLock
import kotlinx.atomicfu.locks.withLock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.math.abs
import kotlin.math.sign

// region interfaces

/**
 * Represents a read-only point in 3D space.
 * This struct may be mutable. If so, then `is MutablePoint3d == true`.
 * [x], [y], and [z] are all expected to be finite values
 * (`Double.isFinite() == true`).
 *
 * @author Int16
 */
sealed interface Point3d {
  /**
   * The x-coordinate of the point. Always expected to be a finite value.
   */
  val x: Double

  /**
   * The y-coordinate of the point. Always expected to be a finite value.
   */
  val y: Double

  /**
   * The z-coordinate of the point. Always expected to be a finite value.
   */
  val z: Double

  /**
   * Component operator for destructuring declarations.
   */
  operator fun component1() = x

  /**
   * Component operator for destructuring declarations.
   */
  operator fun component2() = y

  /**
   * Component operator for destructuring declarations.
   */
  operator fun component3() = z

  override fun toString(): String

  override fun equals(other: Any?): Boolean

  override fun hashCode(): Int

  companion object
}

/**
 * Represents an immutable point in 3D space.
 * Because it is immutable, all operations produce a new instance.
 * To preserve immutability, users cannot implement this interface.
 * [x], [y], and [z] are all expected to be finite values
 * ([Double.isFinite()] == true).
 */
sealed interface ImmutablePoint3d : Point3d

/**
 * Represents a mutable point in 3D space.
 * Changes in value can be monitored via [StateFlow] and [Observable.observe].
 * It is expected that [x], [y], and [z] are all finite values
 * (Double.isFinite() == true).
 */
interface MutablePoint3d :
  Point3d,
  Observable {
  override var x: Double
  override var y: Double
  override var z: Double

  /**
   * A [StateFlow] that emits the current x-coordinate of the point.
   * Always expected to emit finite values.
   */
  val xFlow: StateFlow<Double>

  /**
   * A [StateFlow] that emits the current y-coordinate of the point.
   * Always expected to emit finite values.
   */
  val yFlow: StateFlow<Double>

  /**
   * A [StateFlow] that emits the current z-coordinate of the point.
   * Always expected to emit finite values.
   */
  val zFlow: StateFlow<Double>

  override fun observe(): ObserveTicket

  companion object
}

/**
 * Represents the eight octants of three-dimensional space by the
 * combinations of signs for the x, y, and z axes.
 * Each constant has [xSign], [ySign], and [zSign],
 * which are 1 if positive and -1 if negative.
 */
enum class Octant(
  /**
   * Takes the value 1 for the positive direction and -1 for the
   * negative direction relative to the yz plane.
   */
  val xSign: Int,
  /**
   * Takes the value 1 for the positive direction and -1 for the
   * negative direction relative to the xz plane.
   */
  val ySign: Int,
  /**
   * Takes the value 1 for the positive direction and -1 for the
   * negative direction relative to the xy plane.
   */
  val zSign: Int,
) {
  /**
   * The octant where x, y, and z are all positive.
   */
  PLUS_PLUS_PLUS(1, 1, 1),

  /**
   * The octant where x is negative, and y and z are positive.
   */
  MINUS_PLUS_PLUS(-1, 1, 1),

  /**
   * The octant where y is negative, and x and z are positive.
   */
  PLUS_MINUS_PLUS(1, -1, 1),

  /**
   * The octant where x and y are negative, and z is positive.
   */
  MINUS_MINUS_PLUS(-1, -1, 1),

  /**
   * The octant where z is negative, and x and y are positive.
   */
  PLUS_PLUS_MINUS(1, 1, -1),

  /**
   * The octant where x and z are negative, y is positive.
   */
  MINUS_PLUS_MINUS(-1, 1, -1),

  /**
   * The octant where y and z are negative, x is positive.
   */
  PLUS_MINUS_MINUS(1, -1, -1),

  /**
   * The octant where x, y, and z are all negative.
   */
  MINUS_MINUS_MINUS(-1, -1, -1),
  ;

  companion object {
    /**
     * Returns the [Octant] corresponding to the given signs for
     * the x, y, and z axes.
     * If any of the signs are zero, returns `null`.
     *
     * @param xSign The sign of the x-axis (positive: >0, negative: <0).
     * @param ySign The sign of the y-axis (positive: >0, negative: <0).
     * @param zSign The sign of the z-axis (positive: >0, negative: <0).
     * @return The corresponding [Octant], or `null` if any sign is zero.
     */
    fun fromSigns(
      xSign: Int,
      ySign: Int,
      zSign: Int,
    ): Octant? =
      when {
        xSign > 0 && ySign > 0 && zSign > 0 -> PLUS_PLUS_PLUS
        xSign < 0 && ySign > 0 && zSign > 0 -> MINUS_PLUS_PLUS
        xSign > 0 && ySign < 0 && zSign > 0 -> PLUS_MINUS_PLUS
        xSign < 0 && ySign < 0 && zSign > 0 -> MINUS_MINUS_PLUS
        xSign > 0 && ySign > 0 && zSign < 0 -> PLUS_PLUS_MINUS
        xSign < 0 && ySign > 0 && zSign < 0 -> MINUS_PLUS_MINUS
        xSign > 0 && ySign < 0 && zSign < 0 -> PLUS_MINUS_MINUS
        xSign < 0 && ySign < 0 && zSign < 0 -> MINUS_MINUS_MINUS
        else -> null
      }
  }
}

// endregion

// region constants

/**
 * The origin point (0, 0, 0) in three-dimensional space.
 */
val Point3d.Companion.zero: ImmutablePoint3d get() = POINT3D_ZERO

// endregion

// region factory functions

/**
 * Creates a [Point3d] by specifying each coordinate in three-dimensional space.
 * You can treat it as a [MutablePoint3d] only at the very beginning using
 * a [mutator], but after that, it is frozen as an [ImmutablePoint3d].
 * Even if you use `as MutablePoint3d` after freezing, the value cannot be
 * changed and will result in an error.
 *
 * @param x The x-coordinate in space. Expected to be finite.
 * @param y The y-coordinate in space. Expected to be finite.
 * @param z The z-coordinate in space. Expected to be finite.
 * @param mutator A scope for [MutablePoint3d] for initialization. If null, nothing is done.
 * @return The frozen, immutable [ImmutablePoint3d].
 */
@Suppress("FunctionName")
fun Point3d(
  x: Double = 0.0,
  y: Double = 0.0,
  z: Double = 0.0,
  mutator: (MutablePoint3d.() -> Unit)? = null,
): ImmutablePoint3d {
  require(x.isFinite() && y.isFinite() && z.isFinite()) {
    "Point3d coordinates must be finite values: ($x, $y, $z)"
  }
  if (x == 0.0 && y == 0.0 && z == 0.0 && mutator == null) {
    return Point3d.zero
  }
  val impl = ImmutablePoint3dImpl(x, y, z)
  if (mutator != null) {
    val mutableWrapper = Point3dMutableWrapper(impl)
    mutator(mutableWrapper)
  }
  return impl
}

/**
 * Creates an [ImmutablePoint3d] by copying an existing one.
 * If the original instance is an [ImmutablePoint3d] and [mutator] is null,
 * the same instance is returned without creating anything new.
 *
 * @param copyFrom The instance to copy from. This will be reused if possible.
 * @param mutator A [MutablePoint3d] scope to adjust the values immediately after copying.
 * @return The frozen, immutable [ImmutablePoint3d].
 */
inline fun Point3d.Companion.copyOf(
  copyFrom: Point3d,
  noinline mutator: (MutablePoint3d.() -> Unit)? = null,
): ImmutablePoint3d =
  if (copyFrom is ImmutablePoint3d && mutator == null) {
    copyFrom
  } else {
    Point3d(
      x = copyFrom.x,
      y = copyFrom.y,
      z = copyFrom.z,
      mutator = mutator,
    )
  }

/**
 * Creates a [MutablePoint3d] by specifying each coordinate in three-dimensional space.
 *
 * @param x The x-coordinate in space. Expected to be finite.
 * @param y The y-coordinate in space. Expected to be finite.
 * @param z The z-coordinate in space. Expected to be finite.
 * @return The created [MutablePoint3d].
 */
fun MutablePoint3d(
  x: Double = 0.0,
  y: Double = 0.0,
  z: Double = 0.0,
): MutablePoint3d {
  require(x.isFinite() && y.isFinite() && z.isFinite()) {
    "MutablePoint3d coordinates must be finite values: ($x, $y, $z)"
  }
  return MutablePoint3dImpl(x, y, z)
}

/**
 * Creates a [MutablePoint3d] by copying an existing [Point3d].
 *
 * @param copyFrom The instance to copy from.
 * @return The created [MutablePoint3d].
 */
fun MutablePoint3d.Companion.copyOf(copyFrom: Point3d): MutablePoint3d = MutablePoint3d(copyFrom.x, copyFrom.y, copyFrom.z)

// endregion

// region arithmetic

/**
 * Returns the corresponding octant from the signs of x/y/z.
 * Returns null if any of x, y, or z are 0 or `NaN`.
 *
 * @return the unique corresponding octant if this [Point3d] has one;
 *         otherwise null.
 */
inline val Point3d.octant: Octant?
  get() {
    val xSign = x.sign.toInt()
    val ySign = y.sign.toInt()
    val zSign = z.sign.toInt()
    return Octant.fromSigns(xSign, ySign, zSign)
  }

/**
 * Returns `true` if this point is at the origin (0, 0, 0),
 * within a small epsilon (1e-5), and `false` otherwise.
 *
 * @return Whether this point is approximately the zero point.
 */
inline val Point3d.isZero: Boolean
  get() {
    val eps = 1e-5
    return abs(x) < eps && abs(y) < eps && abs(z) < eps
  }

/**
 * Negates all coordinates of this mutable point.
 * After this operation, x, y, and z become -x, -y, and -z respectively.
 * This operation mutates the original point.
 */
inline fun MutablePoint3d.negate() = map("Negation") { _, value -> -value }

/**
 * Returns a new [ImmutablePoint3d] with all coordinates negated.
 * The original point remains unchanged.
 *
 * @return A new point with negated coordinates.
 */
inline operator fun Point3d.unaryMinus(): ImmutablePoint3d =
  Point3d.copyOf(this) {
    negate()
  }

/**
 * Translates this mutable point by the given [distance].
 *
 * @param distance The displacement to apply to this point.
 */
inline operator fun MutablePoint3d.plusAssign(distance: Distance3d) =
  map("Addition of $distance") { index, value ->
    when (index) {
      0 -> value + distance.dx
      1 -> value + distance.dy
      else -> value + distance.dz
    }
  }

/**
 * Returns a new [ImmutablePoint3d] translated by the given [distance].
 *
 * @param distance The displacement to add.
 * @return A new point after applying the displacement.
 */
inline operator fun Point3d.plus(distance: Distance3d): ImmutablePoint3d =
  Point3d.copyOf(this) {
    this += distance
  }

/**
 * Translates this mutable point by the negative of the given [distance].
 *
 * @param distance The displacement to subtract.
 */
inline operator fun MutablePoint3d.minusAssign(distance: Distance3d) =
  map("Subtraction of $distance") { index, value ->
    when (index) {
      0 -> value - distance.dx
      1 -> value - distance.dy
      else -> value - distance.dz
    }
  }

/**
 * Returns a new [ImmutablePoint3d] translated by the negative of the given [distance].
 *
 * @param distance The displacement to subtract.
 * @return A new point after applying the negative displacement.
 */
inline operator fun Point3d.minus(distance: Distance3d): ImmutablePoint3d =
  Point3d.copyOf(this) {
    this -= distance
  }

/**
 * Returns the component-wise displacement from [other] to this point as a [Distance3d].
 *
 * @param other The origin point of the displacement.
 * @return The [Distance3d] representing this - [other].
 */
inline operator fun Point3d.minus(other: Point3d): Distance3d =
  Distance3d(
    dx = x - other.x,
    dy = y - other.y,
    dz = z - other.z,
  )

/**
 * Multiplies all coordinates of this mutable point by the given scalar.
 * This operation mutates the original point.
 *
 * @param scalar The scalar to multiply by.
 */
inline operator fun MutablePoint3d.timesAssign(scalar: Int) = map("Multiplication by $scalar") { _, value -> value * scalar }

/**
 * Returns a new [ImmutablePoint3d] with all coordinates multiplied by the given scalar.
 * The original point remains unchanged.
 *
 * @param scalar The scalar to multiply by.
 * @return A new point with multiplied coordinates.
 */
inline operator fun Point3d.times(scalar: Int): ImmutablePoint3d =
  Point3d.copyOf(this) {
    this *= scalar
  }

/**
 * Multiplies all coordinates of this mutable point by the given scalar.
 * This operation mutates the original point.
 *
 * @param scalar The scalar to multiply by.
 */
inline operator fun MutablePoint3d.timesAssign(scalar: Double) = map("Multiplication by $scalar") { _, value -> value * scalar }

/**
 * Returns a new [ImmutablePoint3d] with all coordinates multiplied by the given scalar.
 * The original point remains unchanged.
 *
 * @param scalar The scalar to multiply by.
 * @return A new point with multiplied coordinates.
 */
inline operator fun Point3d.times(scalar: Double): ImmutablePoint3d =
  Point3d.copyOf(this) {
    this *= scalar
  }

/**
 * Divides all coordinates of this mutable point by the given scalar.
 * This operation mutates the original point.
 *
 * @param scalar The scalar to divide by.
 * @throws IllegalArgumentException if [scalar] is zero.
 */
inline operator fun MutablePoint3d.divAssign(scalar: Int) {
  require(scalar != 0) { "Cannot divide a Point3d by 0" }
  map("Division by $scalar") { _, value -> value / scalar }
}

/**
 * Returns a new [ImmutablePoint3d] with all coordinates divided by the given scalar.
 * The original point remains unchanged.
 *
 * @param scalar The scalar to divide by.
 * @return A new point with divided coordinates.
 * @throws IllegalArgumentException if [scalar] is zero.
 */
inline operator fun Point3d.div(scalar: Int): ImmutablePoint3d =
  Point3d.copyOf(this) {
    this /= scalar
  }

/**
 * Divides all coordinates of this mutable point by the given scalar.
 * This operation mutates the original point.
 *
 * @param scalar The scalar to divide by.
 * @throws IllegalArgumentException if [scalar] is zero.
 */
inline operator fun MutablePoint3d.divAssign(scalar: Double) {
  require(scalar != 0.0) { "Cannot divide a Point3d by 0.0" }
  map("Division by $scalar") { _, value -> value / scalar }
}

/**
 * Returns a new [ImmutablePoint3d] with all coordinates divided by the given scalar.
 * The original point remains unchanged.
 *
 * @param scalar The scalar to divide by.
 * @return A new point with divided coordinates.
 * @throws IllegalArgumentException if [scalar] is zero.
 */
inline operator fun Point3d.div(scalar: Double): ImmutablePoint3d =
  Point3d.copyOf(this) {
    this /= scalar
  }

/**
 * Returns the distance between this point and another point.
 * Uses a numerically stable algorithm to avoid overflow/underflow.
 *
 * @param other The other point to measure the distance to.
 * @return The distance between this point and [other].
 */
inline infix fun Point3d.distanceTo(other: Point3d): Double = (this - other).magnitude

/**
 * Returns the distance from this point to the origin (0, 0, 0).
 *
 * @return The distance from this point to the origin.
 */
inline val Point3d.distanceFromZero: Double get() = this distanceTo Point3d.zero

/**
 * Maps each coordinate of this mutable point using the given [action],
 * and updates the coordinates with the results.
 * The [action] receives the index (0 for x, 1 for y, 2 for z)
 * and the current value of the coordinate, and should return
 * the new value for that coordinate.
 * After applying the [action], it is verified that all new values
 * are finite; if not, an [ArithmeticException] is thrown.
 *
 * @param actionName An optional name for the action, used in error messages.
 * @param action The mapping function to apply to each coordinate.
 * 0: x-coordinate, 1: y-coordinate, 2: z-coordinate.
 * @throws ArithmeticException if any resulting coordinate is not finite.
 */
inline fun MutablePoint3d.map(
  actionName: String? = null,
  action: (index: Int, value: Double) -> Double,
) {
  val newX = action(0, x)
  val newY = action(1, y)
  val newZ = action(2, z)
  require(newX.isFinite() && newY.isFinite() && newZ.isFinite()) {
    val message =
      if (actionName == null) {
        "Mutation resulted in non-finite values: ($x, $y, $z)"
      } else {
        "$actionName resulted in non-finite values: ($x, $y, $z)"
      }
    throw ArithmeticException(message)
  }
  x = newX
  y = newY
  z = newZ
}

// endregion

// region implementations

private val POINT3D_ZERO: ImmutablePoint3d = ImmutablePoint3dImpl(0.0, 0.0, 0.0)

private data class ImmutablePoint3dImpl(
  override var x: Double,
  override var y: Double,
  override var z: Double,
) : ImmutablePoint3d {
  override fun toString(): String = "Point3d(x=$x, y=$y, z=$z)"

  override fun equals(other: Any?): Boolean =
    when {
      this === other -> true
      other !is Point3d -> false
      else -> componentsEqual(this, other)
    }

  override fun hashCode(): Int = componentsHash(x, y, z)
}

private value class Point3dMutableWrapper(
  private val impl: ImmutablePoint3dImpl,
) : MutablePoint3d {
  override var x: Double
    get() = impl.x
    set(value) {
      impl.x = value
    }
  override var y: Double
    get() = impl.y
    set(value) {
      impl.y = value
    }
  override var z: Double
    get() = impl.z
    set(value) {
      impl.z = value
    }

  override fun toString(): String = "Point3d(x=$x, y=$y, z=$z)"

  override val xFlow: StateFlow<Double>
    get() = throw UnsupportedOperationException()
  override val yFlow: StateFlow<Double>
    get() = throw UnsupportedOperationException()
  override val zFlow: StateFlow<Double>
    get() = throw UnsupportedOperationException()

  override fun observe(): ObserveTicket = throw UnsupportedOperationException()
}

private class MutablePoint3dImpl(
  x: Double,
  y: Double,
  z: Double,
) : MutablePoint3d {
  private var generation: Int = 0
  private val lock = ReentrantLock()
  private val _xFlow: MutableStateFlow<Double> = MutableStateFlow(x)
  private val _yFlow: MutableStateFlow<Double> = MutableStateFlow(y)
  private val _zFlow: MutableStateFlow<Double> = MutableStateFlow(z)

  override var x: Double
    get() = lock.withLock { _xFlow.value }
    set(value) {
      lock.withLock {
        generation++
        _xFlow.value = value
      }
    }
  override var y: Double
    get() = lock.withLock { _yFlow.value }
    set(value) {
      lock.withLock {
        generation++
        _yFlow.value = value
      }
    }
  override var z: Double
    get() = lock.withLock { _zFlow.value }
    set(value) {
      lock.withLock {
        generation++
        _zFlow.value = value
      }
    }

  override val xFlow: StateFlow<Double> get() = _xFlow.asStateFlow()
  override val yFlow: StateFlow<Double> get() = _yFlow.asStateFlow()
  override val zFlow: StateFlow<Double> get() = _zFlow.asStateFlow()

  override fun toString(): String = "Point3d(x=$x, y=$y, z=$z)"

  override fun equals(other: Any?): Boolean =
    when {
      this === other -> true
      other !is Point3d -> false
      else -> componentsEqual(this, other)
    }

  override fun hashCode(): Int = componentsHash(x, y, z)

  override fun observe(): ObserveTicket = Ticket(this)

  private class Ticket(
    original: MutablePoint3dImpl,
  ) : ObserveTicket {
    private val weakOriginal by WeakProperty(original)
    private val knownGeneration: Int = original.lock.withLock { original.generation }

    override val isDirty: Boolean
      get() =
        weakOriginal?.let {
          it.lock.withLock { it.generation != knownGeneration }
        } ?: false

    override val isActive: Boolean
      get() = weakOriginal != null

    override fun reset() {
      weakOriginal?.let {
        it.lock.withLock {
          it.generation = knownGeneration
        }
      }
    }

    override fun fetchAndReset(): Boolean =
      weakOriginal?.let {
        it.lock.withLock {
          val dirty = it.generation != knownGeneration
          it.generation = knownGeneration
          dirty
        }
      } ?: false
  }
}

private fun componentsEqual(
  a: Point3d,
  b: Point3d,
): Boolean =
  doublesEqual(a.x, b.x) &&
    doublesEqual(a.y, b.y) &&
    doublesEqual(a.z, b.z)

private fun componentsHash(
  x: Double,
  y: Double,
  z: Double,
): Int {
  var result = 17
  result = 31 * result + normalizedHash(x)
  result = 31 * result + normalizedHash(y)
  result = 31 * result + normalizedHash(z)
  return result
}

private fun doublesEqual(
  a: Double,
  b: Double,
): Boolean = a == b || (a.isNaN() && b.isNaN())

private fun normalizedHash(value: Double): Int {
  val normalized =
    when {
      value == 0.0 -> 0.0
      value.isNaN() -> Double.NaN
      else -> value
    }
  val bits = normalized.toRawBits()
  return (bits xor (bits ushr 32)).toInt()
}

// endregion
