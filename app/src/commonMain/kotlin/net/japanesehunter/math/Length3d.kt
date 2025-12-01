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
import kotlin.math.hypot
import kotlin.math.sqrt

// region interfaces

/**
 * Represents a distance or displacement in 3D space.
 * This struct may be mutable. If so, then `is MutableLength3d == true`.
 * [dx], [dy], and [dz] are all represented as [Length] values.
 *
 * @author Int16
 */
sealed interface Length3d {
  /**
   * The delta along the x-axis as a [Length].
   */
  val dx: Length

  /**
   * The delta along the y-axis as a [Length].
   */
  val dy: Length

  /**
   * The delta along the z-axis as a [Length].
   */
  val dz: Length

  /**
   * Component operator for destructuring declarations.
   */
  operator fun component1() = dx

  /**
   * Component operator for destructuring declarations.
   */
  operator fun component2() = dy

  /**
   * Component operator for destructuring declarations.
   */
  operator fun component3() = dz

  override fun toString(): String

  override fun equals(other: Any?): Boolean

  override fun hashCode(): Int

  companion object
}

/**
 * Represents an immutable distance in 3D space.
 * Because it is immutable, all operations produce a new instance.
 * To preserve immutability, users cannot implement this interface.
 * [dx], [dy], and [dz] are all represented as [Length] values.
 */
sealed interface ImmutableLength3d : Length3d

/**
 * Represents a mutable distance in 3D space.
 * Changes in value can be monitored via [StateFlow] and [Observable.observe].
 * [dx], [dy], and [dz] are all represented as [Length] values.
 */
interface MutableLength3d :
  Length3d,
  Observable {
  override var dx: Length
  override var dy: Length
  override var dz: Length

  /**
   * A [StateFlow] that emits the current delta along the x-axis.
   * Always emits [Length] values.
   */
  val dxFlow: StateFlow<Length>

  /**
   * A [StateFlow] that emits the current delta along the y-axis.
   * Always emits [Length] values.
   */
  val dyFlow: StateFlow<Length>

  /**
   * A [StateFlow] that emits the current delta along the z-axis.
   * Always emits [Length] values.
   */
  val dzFlow: StateFlow<Length>

  override fun observe(): ObserveTicket

  companion object
}

// endregion

// region constants

/**
 * The zero distance (0, 0, 0) in three-dimensional space.
 */
val Length3d.Companion.zero: ImmutableLength3d get() = LENGTH3D_ZERO

// endregion

// region factory functions

/**
 * Creates a [Length3d] by specifying each component in three-dimensional space.
 * You can treat it as a [MutableLength3d] only at the very beginning using
 * a [mutator], but after that, it is frozen as an [ImmutableLength3d].
 * Even if you use `as MutableLength3d` after freezing, the value cannot be
 * changed and will result in an error.
 *
 * @param dx The delta along the x-axis.
 * @param dy The delta along the y-axis.
 * @param dz The delta along the z-axis.
 * @param mutator A scope for [MutableLength3d] for initialization. If null, nothing is done.
 * @return The frozen, immutable [ImmutableLength3d].
 */
@Suppress("FunctionName")
fun Length3d(
  dx: Length = Length.ZERO,
  dy: Length = Length.ZERO,
  dz: Length = Length.ZERO,
  mutator: (MutableLength3d.() -> Unit)? = null,
): ImmutableLength3d {
  if (dx.isZero && dy.isZero && dz.isZero && mutator == null) {
    return Length3d.zero
  }
  val impl = ImmutableLength3dImpl(dx, dy, dz)
  if (mutator != null) {
    val mutableWrapper = Length3dMutableWrapper(impl)
    mutator(mutableWrapper)
  }
  return impl
}

/**
 * Creates an [ImmutableLength3d] by copying an existing one.
 * If the original instance is an [ImmutableLength3d] and [mutator] is null,
 * the same instance is returned without creating anything new.
 *
 * @param copyFrom The instance to copy from. This will be reused if possible.
 * @param mutator A [MutableLength3d] scope to adjust the values immediately after copying.
 * @return The frozen, immutable [ImmutableLength3d].
 */
inline fun Length3d.Companion.copyOf(
  copyFrom: Length3d,
  noinline mutator: (MutableLength3d.() -> Unit)? = null,
): ImmutableLength3d =
  if (copyFrom is ImmutableLength3d && mutator == null) {
    copyFrom
  } else {
    Length3d(
      dx = copyFrom.dx,
      dy = copyFrom.dy,
      dz = copyFrom.dz,
      mutator = mutator,
    )
  }

/**
 * Creates a [MutableLength3d] by specifying each component in three-dimensional space.
 *
 * @param dx The delta along the x-axis.
 * @param dy The delta along the y-axis.
 * @param dz The delta along the z-axis.
 * @return The created [MutableLength3d].
 */
fun MutableLength3d(
  dx: Length = Length.ZERO,
  dy: Length = Length.ZERO,
  dz: Length = Length.ZERO,
): MutableLength3d {
  return MutableLength3dImpl(dx, dy, dz)
}

/**
 * Creates a [MutableLength3d] by copying an existing [Length3d].
 *
 * @param copyFrom The instance to copy from.
 * @return The created [MutableLength3d].
 */
fun MutableLength3d.Companion.copyOf(copyFrom: Length3d): MutableLength3d = MutableLength3d(copyFrom.dx, copyFrom.dy, copyFrom.dz)

// endregion

// region arithmetic

/**
 * Returns `true` if this distance is exactly (0, 0, 0).
 */
inline val Length3d.isZero: Boolean
  get() = dx.isZero && dy.isZero && dz.isZero

/**
 * Returns the Euclidean magnitude of this distance as a [Length].
 * Uses a numerically stable algorithm to avoid overflow/underflow by working
 * in nanometers.
 *
 * @return The magnitude of this distance.
 */
inline val Length3d.magnitude: Length
  get() {
    val dxNm = dx.toDouble(LengthUnit.NANOMETER)
    val dyNm = dy.toDouble(LengthUnit.NANOMETER)
    val dzNm = dz.toDouble(LengthUnit.NANOMETER)
    val max = maxOf(abs(dxNm), abs(dyNm), abs(dzNm))
    if (max == 0.0) return Length.ZERO
    val minThreshold = 1e-154
    val maxThreshold = 1e154
    val magnitudeNm =
      if (minThreshold < max && max < maxThreshold) {
        sqrt(dxNm * dxNm + dyNm * dyNm + dzNm * dzNm)
      } else {
        hypot(hypot(dxNm, dyNm), dzNm)
      }
    return Length.from(magnitudeNm, LengthUnit.NANOMETER)
  }

/**
 * Normalizes this mutable vector to unit length (1 [unit]).
 *
 * @param unit The unit to measure the magnitude in (default: meters).
 * @throws IllegalArgumentException if the vector has zero length.
 */
inline fun MutableLength3d.normalize(unit: LengthUnit = LengthUnit.METER) {
  val mag = magnitude.toDouble(unit)
  require(mag != 0.0) { "Cannot normalize a zero-length vector." }
  val inv = 1.0 / mag
  map("Normalization") { _, value -> value * inv }
}

/**
 * Returns a normalized copy of this vector with length 1 [unit].
 *
 * @param unit The unit to measure the magnitude in (default: meters).
 * @throws IllegalArgumentException if the vector has zero length.
 */
inline fun Length3d.normalized(unit: LengthUnit = LengthUnit.METER): ImmutableLength3d =
  Length3d.copyOf(this) {
    normalize(unit)
  }

/**
 * Returns the dot product of this distance with [other] as an [Area] (nanometer²).
 *
 * @param other The distance to take the dot product with.
 * @return The dot product in nm².
 */
inline infix fun Length3d.dot(other: Length3d): Area {
  val dx = this.dx.toDouble(LengthUnit.NANOMETER)
  val dy = this.dy.toDouble(LengthUnit.NANOMETER)
  val dz = this.dz.toDouble(LengthUnit.NANOMETER)
  val dot = dx * other.dx.toDouble(LengthUnit.NANOMETER) + dy * other.dy.toDouble(LengthUnit.NANOMETER) + dz * other.dz.toDouble(LengthUnit.NANOMETER)
  return Area.from(dot, AreaUnit.SQUARE_NANOMETER)
}

/**
 * Returns the cross product of this distance with [other] as an [Area3d] in nanometer².
 *
 * @param other The distance to take the cross product with.
 * @return A new [Area3d] representing the cross product in nm².
 */
inline infix fun Length3d.cross(other: Length3d): Area3d = cross(other, LengthUnit.NANOMETER)

/**
 * Returns the cross product of this distance with [other] as an [Area3d].
 *
 * @param other The distance to take the cross product with.
 * @param unit The unit the result is expressed in (squared).
 * @return A new [Area3d] representing the cross product in [unit]².
 */
fun Length3d.cross(
  other: Length3d,
  unit: LengthUnit,
): Area3d {
  val dx = this.dx.toDouble(unit)
  val dy = this.dy.toDouble(unit)
  val dz = this.dz.toDouble(unit)
  val areaUnit = AreaUnit.from(unit)
  return Area3d(
    ax = Area.from(dy * other.dz.toDouble(unit) - dz * other.dy.toDouble(unit), areaUnit),
    ay = Area.from(dz * other.dx.toDouble(unit) - dx * other.dz.toDouble(unit), areaUnit),
    az = Area.from(dx * other.dy.toDouble(unit) - dy * other.dx.toDouble(unit), areaUnit),
  )
}

/**
 * Returns the left-handed cross product of this distance with [other] as an [Area3d] in nanometer².
 * Equivalent to `-(this cross other)` when using a right-handed system.
 *
 * @param other The distance to take the left-handed cross product with.
 * @return A new [Area3d] representing the left-handed cross product in nm².
 */
inline infix fun Length3d.crossLH(other: Length3d): Area3d = crossLH(other, LengthUnit.NANOMETER)

/**
 * Returns the left-handed cross product of this distance with [other] as an [Area3d].
 * Equivalent to the negated right-handed cross when interpreted in a right-handed system.
 *
 * @param other The distance to take the left-handed cross product with.
 * @param unit The unit the result is expressed in (squared).
 * @return A new [Area3d] representing the left-handed cross product in [unit]².
 */
fun Length3d.crossLH(
  other: Length3d,
  unit: LengthUnit,
): Area3d {
  val dx = this.dx.toDouble(unit)
  val dy = this.dy.toDouble(unit)
  val dz = this.dz.toDouble(unit)
  val areaUnit = AreaUnit.from(unit)
  return Area3d(
    ax = Area.from(dz * other.dy.toDouble(unit) - dy * other.dz.toDouble(unit), areaUnit),
    ay = Area.from(dx * other.dz.toDouble(unit) - dz * other.dx.toDouble(unit), areaUnit),
    az = Area.from(dy * other.dx.toDouble(unit) - dx * other.dy.toDouble(unit), areaUnit),
  )
}

/**
 * Negates all components of this mutable distance.
 * After this operation, dx, dy, and dz become -dx, -dy, and -dz respectively.
 * This operation mutates the original distance.
 */
inline fun MutableLength3d.negate() = map("Negation") { _, value -> -value }

/**
 * Returns a new [ImmutableLength3d] with all components negated.
 * The original distance remains unchanged.
 *
 * @return A new distance with negated components.
 */
inline operator fun Length3d.unaryMinus(): ImmutableLength3d =
  Length3d.copyOf(this) {
    negate()
  }

/**
 * Adds the corresponding components of [other] to this mutable distance.
 * Mutates this distance.
 *
 * @param other The distance to add.
 */
inline operator fun MutableLength3d.plusAssign(other: Length3d) =
  map("Addition of $other") { index, value ->
    when (index) {
      0 -> value + other.dx
      1 -> value + other.dy
      else -> value + other.dz
    }
  }

/**
 * Returns a new [ImmutableLength3d] representing the sum of this distance and [other].
 *
 * @param other The distance to add.
 * @return A new immutable distance containing the component-wise sum.
 */
inline operator fun Length3d.plus(other: Length3d): ImmutableLength3d =
  Length3d.copyOf(this) {
    this += other
  }

/**
 * Subtracts the corresponding components of [other] from this mutable distance.
 * Mutates this distance.
 *
 * @param other The distance to subtract.
 */
inline operator fun MutableLength3d.minusAssign(other: Length3d) =
  map("Subtraction of $other") { index, value ->
    when (index) {
      0 -> value - other.dx
      1 -> value - other.dy
      else -> value - other.dz
    }
  }

/**
 * Returns a new [ImmutableLength3d] representing the difference of this distance and [other].
 *
 * @param other The distance to subtract.
 * @return A new immutable distance containing the component-wise difference.
 */
inline operator fun Length3d.minus(other: Length3d): ImmutableLength3d =
  Length3d.copyOf(this) {
    this -= other
  }

/**
 * Multiplies all components of this mutable distance by the given scalar.
 * This operation mutates the original distance.
 *
 * @param scalar The scalar to multiply by.
 */
inline operator fun MutableLength3d.timesAssign(scalar: Int) =
  map("Multiplication by $scalar") { _, value ->
    value * scalar.toLong()
  }

/**
 * Returns a new [ImmutableLength3d] with all components multiplied by the given scalar.
 * The original distance remains unchanged.
 *
 * @param scalar The scalar to multiply by.
 * @return A new distance with multiplied components.
 */
inline operator fun Length3d.times(scalar: Int): ImmutableLength3d =
  Length3d.copyOf(this) {
    this *= scalar
  }

/**
 * Multiplies all components of this mutable distance by the given scalar.
 * This operation mutates the original distance.
 *
 * @param scalar The scalar to multiply by.
 */
inline operator fun MutableLength3d.timesAssign(scalar: Double) = map("Multiplication by $scalar") { _, value -> value * scalar }

/**
 * Returns a new [ImmutableLength3d] with all components multiplied by the given scalar.
 * The original distance remains unchanged.
 *
 * @param scalar The scalar to multiply by.
 * @return A new distance with multiplied components.
 */
inline operator fun Length3d.times(scalar: Double): ImmutableLength3d =
  Length3d.copyOf(this) {
    this *= scalar
  }

/**
 * Divides all components of this mutable distance by the given scalar.
 * This operation mutates the original distance.
 *
 * @param scalar The scalar to divide by.
 * @throws IllegalArgumentException if [scalar] is zero.
 */
inline operator fun MutableLength3d.divAssign(scalar: Int) {
  require(scalar != 0) { "Cannot divide a Length3d by 0" }
  map("Division by $scalar") { _, value -> value / scalar.toLong() }
}

/**
 * Returns a new [ImmutableLength3d] with all components divided by the given scalar.
 * The original distance remains unchanged.
 *
 * @param scalar The scalar to divide by.
 * @return A new distance with divided components.
 * @throws IllegalArgumentException if [scalar] is zero.
 */
inline operator fun Length3d.div(scalar: Int): ImmutableLength3d =
  Length3d.copyOf(this) {
    this /= scalar
  }

/**
 * Divides all components of this mutable distance by the given scalar.
 * This operation mutates the original distance.
 *
 * @param scalar The scalar to divide by.
 * @throws IllegalArgumentException if [scalar] is zero.
 */
inline operator fun MutableLength3d.divAssign(scalar: Double) {
  require(scalar != 0.0) { "Cannot divide a Length3d by 0.0" }
  map("Division by $scalar") { _, value -> value / scalar }
}

/**
 * Returns a new [ImmutableLength3d] with all components divided by the given scalar.
 * The original distance remains unchanged.
 *
 * @param scalar The scalar to divide by.
 * @return A new distance with divided components.
 * @throws IllegalArgumentException if [scalar] is zero.
 */
inline operator fun Length3d.div(scalar: Double): ImmutableLength3d =
  Length3d.copyOf(this) {
    this /= scalar
  }

/**
 * Maps each component of this mutable distance using the given [action],
 * and updates the components with the results.
 * The [action] receives the index (0 for dx, 1 for dy, 2 for dz)
 * and the current value of the component, and should return
 * the new value for that component.
 *
 * @param actionName An optional name for the action, used in error messages.
 * @param action The mapping function to apply to each component.
 * 0: dx, 1: dy, 2: dz.
 */
@Suppress("UNUSED_PARAMETER")
inline fun MutableLength3d.map(
  actionName: String? = null,
  action: (index: Int, value: Length) -> Length,
) {
  val newDx = action(0, dx)
  val newDy = action(1, dy)
  val newDz = action(2, dz)
  dx = newDx
  dy = newDy
  dz = newDz
}

// endregion

// region implementations

private val LENGTH3D_ZERO: ImmutableLength3d = ImmutableLength3dImpl(Length.ZERO, Length.ZERO, Length.ZERO)

private data class ImmutableLength3dImpl(
  override var dx: Length,
  override var dy: Length,
  override var dz: Length,
) : ImmutableLength3d {
  override fun toString(): String = "Length3d(dx=$dx, dy=$dy, dz=$dz)"

  override fun equals(other: Any?): Boolean =
    when {
      this === other -> true
      other !is Length3d -> false
      else -> componentsEqual(this, other)
    }

  override fun hashCode(): Int = componentsHash(dx, dy, dz)
}

private value class Length3dMutableWrapper(
  private val impl: ImmutableLength3dImpl,
) : MutableLength3d {
  override var dx: Length
    get() = impl.dx
    set(value) {
      impl.dx = value
    }
  override var dy: Length
    get() = impl.dy
    set(value) {
      impl.dy = value
    }
  override var dz: Length
    get() = impl.dz
    set(value) {
      impl.dz = value
    }

  override fun toString(): String = "Length3d(dx=$dx, dy=$dy, dz=$dz)"

  override val dxFlow: StateFlow<Length>
    get() = throw UnsupportedOperationException()
  override val dyFlow: StateFlow<Length>
    get() = throw UnsupportedOperationException()
  override val dzFlow: StateFlow<Length>
    get() = throw UnsupportedOperationException()

  override fun observe(): ObserveTicket = throw UnsupportedOperationException()
}

private class MutableLength3dImpl(
  dx: Length,
  dy: Length,
  dz: Length,
) : MutableLength3d {
  private var generation: Int = 0
  private val lock = ReentrantLock()
  private val _dxFlow: MutableStateFlow<Length> = MutableStateFlow(dx)
  private val _dyFlow: MutableStateFlow<Length> = MutableStateFlow(dy)
  private val _dzFlow: MutableStateFlow<Length> = MutableStateFlow(dz)

  override var dx: Length
    get() = lock.withLock { _dxFlow.value }
    set(value) {
      lock.withLock {
        generation++
        _dxFlow.value = value
      }
    }
  override var dy: Length
    get() = lock.withLock { _dyFlow.value }
    set(value) {
      lock.withLock {
        generation++
        _dyFlow.value = value
      }
    }
  override var dz: Length
    get() = lock.withLock { _dzFlow.value }
    set(value) {
      lock.withLock {
        generation++
        _dzFlow.value = value
      }
    }

  override val dxFlow: StateFlow<Length> get() = _dxFlow.asStateFlow()
  override val dyFlow: StateFlow<Length> get() = _dyFlow.asStateFlow()
  override val dzFlow: StateFlow<Length> get() = _dzFlow.asStateFlow()

  override fun toString(): String = "Length3d(dx=$dx, dy=$dy, dz=$dz)"

  override fun equals(other: Any?): Boolean =
    when {
      this === other -> true
      other !is Length3d -> false
      else -> componentsEqual(this, other)
    }

  override fun hashCode(): Int = componentsHash(dx, dy, dz)

  override fun observe(): ObserveTicket = Ticket(this)

  private class Ticket(
    original: MutableLength3dImpl,
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
  a: Length3d,
  b: Length3d,
): Boolean =
  a.dx == b.dx &&
    a.dy == b.dy &&
    a.dz == b.dz

private fun componentsHash(
  dx: Length,
  dy: Length,
  dz: Length,
): Int {
  var result = 17
  result = 31 * result + dx.inWholeNanometers.hashCode()
  result = 31 * result + dy.inWholeNanometers.hashCode()
  result = 31 * result + dz.inWholeNanometers.hashCode()
  return result
}

// endregion
