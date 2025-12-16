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
 * This struct may be mutable. If so, then `is MutableLength3 == true`.
 * [dx], [dy], and [dz] are all represented as [Length] values.
 *
 * @author Int16
 */
sealed interface Length3 {
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
sealed interface ImmutableLength3 : Length3

/**
 * Represents a mutable distance in 3D space.
 * Changes in value can be monitored via [StateFlow] and [Observable.observe].
 * [dx], [dy], and [dz] are all represented as [Length] values.
 */
interface MutableLength3 :
  Length3,
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

  /**
   * Runs [action] while holding the internal lock when available so compound operations stay consistent.
   */
  fun mutate(action: MutableLength3.() -> Unit) = action(this)

  override fun observe(): ObserveTicket

  companion object
}

// endregion

// region constants

/**
 * The zero length (0, 0, 0) in three-dimensional space.
 */
val Length3.Companion.zero: ImmutableLength3 get() = LENGTH3_ZERO

/**
 * A one-meter displacement upward (0, 1 m, 0).
 */
val Length3.Companion.up: ImmutableLength3 get() = LENGTH3_UP

/**
 * A one-meter displacement toward the negative z-axis (0, 0, -1m).
 */
val Length3.Companion.north: ImmutableLength3 get() = LENGTH3_NORTH

/**
 * A one-meter displacement toward the positive x-axis (1m, 0, 0).
 */
val Length3.Companion.east: ImmutableLength3 get() = LENGTH3_EAST

/**
 * A one-meter displacement toward the positive z-axis (0, 0, 1m).
 */
val Length3.Companion.south: ImmutableLength3 get() = LENGTH3_SOUTH

/**
 * A one-meter displacement toward the negative x-axis (-1m, 0, 0).
 */
val Length3.Companion.west: ImmutableLength3 get() = LENGTH3_WEST

/**
 * A one-meter displacement downward (0, -1m, 0).
 */
val Length3.Companion.down: ImmutableLength3 get() = LENGTH3_DOWN

// endregion

// region factory functions

/**
 * Creates a [Length3] by specifying each component in three-dimensional space.
 * You can treat it as a [MutableLength3] only at the very beginning using
 * a [mutator], but after that, it is frozen as an [ImmutableLength3].
 * Even if you use `as MutableLength3` after freezing, the value cannot be
 * changed and will result in an error.
 *
 * @param dx The delta along the x-axis.
 * @param dy The delta along the y-axis.
 * @param dz The delta along the z-axis.
 * @param mutator A scope for [MutableLength3] for initialization. If null, nothing is done.
 * @return The frozen, immutable [ImmutableLength3].
 */
@Suppress("FunctionName")
fun Length3(
  dx: Length = Length.ZERO,
  dy: Length = Length.ZERO,
  dz: Length = Length.ZERO,
  mutator: (MutableLength3.() -> Unit)? = null,
): ImmutableLength3 {
  if (mutator == null) {
    when {
      dx.isZero && dy.isZero && dz.isZero -> return Length3.zero
      dx == ONE_METER && dy.isZero && dz.isZero -> return Length3.east
      dx == -ONE_METER && dy.isZero && dz.isZero -> return Length3.west
      dx.isZero && dy == ONE_METER && dz.isZero -> return Length3.up
      dx.isZero && dy == -ONE_METER && dz.isZero -> return Length3.down
      dx.isZero && dy.isZero && dz == -ONE_METER -> return Length3.north
      dx.isZero && dy.isZero && dz == ONE_METER -> return Length3.south
    }
  }
  val impl = ImmutableLength3Impl(dx, dy, dz)
  if (mutator != null) {
    val mutableWrapper = Length3MutableWrapper(impl)
    mutator(mutableWrapper)
  }
  return impl
}

/**
 * Creates an [ImmutableLength3] by copying an existing one.
 * If the original instance is an [ImmutableLength3] and [mutator] is null,
 * the same instance is returned without creating anything new.
 *
 * @param copyFrom The instance to copy from. This will be reused if possible.
 * @param mutator A [MutableLength3] scope to adjust the values immediately after copying.
 * @return The frozen, immutable [ImmutableLength3].
 */
inline fun Length3.Companion.copyOf(
  copyFrom: Length3,
  noinline mutator: (MutableLength3.() -> Unit)? = null,
): ImmutableLength3 =
  if (copyFrom is ImmutableLength3 && mutator == null) {
    copyFrom
  } else {
    Length3(
      dx = copyFrom.dx,
      dy = copyFrom.dy,
      dz = copyFrom.dz,
      mutator = mutator,
    )
  }

/**
 * Creates a [MutableLength3] by specifying each component in three-dimensional space.
 *
 * @param dx The delta along the x-axis.
 * @param dy The delta along the y-axis.
 * @param dz The delta along the z-axis.
 * @return The created [MutableLength3].
 */
fun MutableLength3(
  dx: Length = Length.ZERO,
  dy: Length = Length.ZERO,
  dz: Length = Length.ZERO,
): MutableLength3 = MutableLength3Impl(dx, dy, dz)

/**
 * Creates a [MutableLength3] by copying an existing [Length3].
 *
 * @param copyFrom The instance to copy from.
 * @return The created [MutableLength3].
 */
fun MutableLength3.Companion.copyOf(copyFrom: Length3): MutableLength3 = MutableLength3(copyFrom.dx, copyFrom.dy, copyFrom.dz)

/**
 * Returns a new [ImmutableLength3] by scaling the given [Direction3] by the specified [Length].
 *
 * @param other The length to scale by.
 * @return A new [ImmutableLength3] representing the scaled direction.
 */
operator fun Direction3.times(other: Length): ImmutableLength3 =
  Length3(
    dx = this.ux * other,
    dy = this.uy * other,
    dz = this.uz * other,
  )

// endregion

// region arithmetic

/**
 * Returns `true` if this distance is exactly (0, 0, 0).
 */
inline val Length3.isZero: Boolean
  get() = dx.isZero && dy.isZero && dz.isZero

/**
 * Returns the Euclidean magnitude of this distance as a [Length].
 * Uses a numerically stable algorithm to avoid overflow/underflow by working
 * in nanometers.
 *
 * @return The magnitude of this distance.
 */
inline val Length3.magnitude: Length
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
inline fun MutableLength3.normalize(unit: LengthUnit = LengthUnit.METER) {
  mutate {
    val mag = magnitude.toDouble(unit)
    require(mag != 0.0) { "Cannot normalize a zero-length vector." }
    val inv = 1.0 / mag
    map({ "Normalization" }) { _, value -> value * inv }
  }
}

/**
 * Returns a normalized copy of this vector with length 1 [unit].
 *
 * @param unit The unit to measure the magnitude in (default: meters).
 * @throws IllegalArgumentException if the vector has zero length.
 */
inline fun Length3.normalized(unit: LengthUnit = LengthUnit.METER): ImmutableLength3 =
  Length3.copyOf(this) {
    normalize(unit)
  }

/**
 * Returns the dot product of this distance with [other] as an [Area] (nanometer²).
 *
 * @param other The distance to take the dot product with.
 * @return The dot product in nm².
 */
inline infix fun Length3.dot(other: Length3): Area {
  val dx = this.dx.toDouble(LengthUnit.NANOMETER)
  val dy = this.dy.toDouble(LengthUnit.NANOMETER)
  val dz = this.dz.toDouble(LengthUnit.NANOMETER)
  val dot =
    dx * other.dx.toDouble(LengthUnit.NANOMETER) + dy * other.dy.toDouble(LengthUnit.NANOMETER) +
      dz * other.dz.toDouble(LengthUnit.NANOMETER)
  return Area.from(dot, AreaUnit.SQUARE_NANOMETER)
}

/**
 * Returns the cross product of this distance with [other] as an [Area3] in nanometer².
 *
 * @param other The distance to take the cross product with.
 * @return A new [Area3] representing the cross product in nm².
 */
inline infix fun Length3.cross(other: Length3): ImmutableArea3 =
  cross(
    other,
    LengthUnit.NANOMETER,
  )

/**
 * Returns the cross product of this distance with [other] as an [Area3].
 *
 * @param other The distance to take the cross product with.
 * @param unit The unit the result is expressed in (squared).
 * @return A new [Area3] representing the cross product in [unit]².
 */
fun Length3.cross(
  other: Length3,
  unit: LengthUnit,
): ImmutableArea3 {
  val dx = this.dx.toDouble(unit)
  val dy = this.dy.toDouble(unit)
  val dz = this.dz.toDouble(unit)
  val areaUnit = AreaUnit.from(unit)
  return Area3(
    ax = Area.from(dy * other.dz.toDouble(unit) - dz * other.dy.toDouble(unit), areaUnit),
    ay = Area.from(dz * other.dx.toDouble(unit) - dx * other.dz.toDouble(unit), areaUnit),
    az = Area.from(dx * other.dy.toDouble(unit) - dy * other.dx.toDouble(unit), areaUnit),
  )
}

/**
 * Returns the left-handed cross product of this distance with [other] as an [Area3] in nanometer².
 * Equivalent to `-(this cross other)` when using a right-handed system.
 *
 * @param other The distance to take the left-handed cross product with.
 * @return A new [Area3] representing the left-handed cross product in nm².
 */
inline infix fun Length3.crossLH(other: Length3): Area3 = crossLH(other, LengthUnit.NANOMETER)

/**
 * Returns the left-handed cross product of this distance with [other] as an [Area3].
 * Equivalent to the negated right-handed cross when interpreted in a right-handed system.
 *
 * @param other The distance to take the left-handed cross product with.
 * @param unit The unit the result is expressed in (squared).
 * @return A new [Area3] representing the left-handed cross product in [unit]².
 */
fun Length3.crossLH(
  other: Length3,
  unit: LengthUnit,
): Area3 {
  val dx = this.dx.toDouble(unit)
  val dy = this.dy.toDouble(unit)
  val dz = this.dz.toDouble(unit)
  val areaUnit = AreaUnit.from(unit)
  return Area3(
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
inline fun MutableLength3.negate() = map({ "Negation" }) { _, value -> -value }

/**
 * Returns a new [ImmutableLength3] with all components negated.
 * The original distance remains unchanged.
 *
 * @return A new distance with negated components.
 */
inline operator fun Length3.unaryMinus(): ImmutableLength3 =
  Length3.copyOf(this) {
    negate()
  }

/**
 * Adds the corresponding components of [other] to this mutable distance.
 * Mutates this distance.
 *
 * @param other The distance to add.
 */
inline operator fun MutableLength3.plusAssign(other: Length3) =
  map({ "Addition of $other" }) { index, value ->
    when (index) {
      0 -> value + other.dx
      1 -> value + other.dy
      else -> value + other.dz
    }
  }

/**
 * Returns a new [ImmutableLength3] representing the sum of this distance and [other].
 *
 * @param other The distance to add.
 * @return A new immutable distance containing the component-wise sum.
 */
inline operator fun Length3.plus(other: Length3): ImmutableLength3 =
  Length3.copyOf(this) {
    this += other
  }

/**
 * Subtracts the corresponding components of [other] from this mutable distance.
 * Mutates this distance.
 *
 * @param other The distance to subtract.
 */
inline operator fun MutableLength3.minusAssign(other: Length3) =
  map({ "Subtraction of $other" }) { index, value ->
    when (index) {
      0 -> value - other.dx
      1 -> value - other.dy
      else -> value - other.dz
    }
  }

/**
 * Returns a new [ImmutableLength3] representing the difference of this distance and [other].
 *
 * @param other The distance to subtract.
 * @return A new immutable distance containing the component-wise difference.
 */
inline operator fun Length3.minus(other: Length3): ImmutableLength3 =
  Length3.copyOf(this) {
    this -= other
  }

/**
 * Multiplies all components of this mutable distance by the given scalar.
 * This operation mutates the original distance.
 *
 * @param scalar The scalar to multiply by.
 */
inline operator fun MutableLength3.timesAssign(scalar: Int) =
  map({ "Multiplication by $scalar" }) { _, value ->
    value * scalar.toLong()
  }

/**
 * Returns a new [ImmutableLength3] with all components multiplied by the given scalar.
 * The original distance remains unchanged.
 *
 * @param scalar The scalar to multiply by.
 * @return A new distance with multiplied components.
 */
inline operator fun Length3.times(scalar: Int): ImmutableLength3 =
  Length3.copyOf(this) {
    this *= scalar
  }

/**
 * Multiplies all components of this mutable distance by the given scalar.
 * This operation mutates the original distance.
 *
 * @param scalar The scalar to multiply by.
 */
inline operator fun MutableLength3.timesAssign(scalar: Double) = map({ "Multiplication by $scalar" }) { _, value -> value * scalar }

/**
 * Returns a new [ImmutableLength3] with all components multiplied by the given scalar.
 * The original distance remains unchanged.
 *
 * @param scalar The scalar to multiply by.
 * @return A new distance with multiplied components.
 */
inline operator fun Length3.times(scalar: Double): ImmutableLength3 =
  Length3.copyOf(this) {
    this *= scalar
  }

/**
 * Divides all components of this mutable distance by the given scalar.
 * This operation mutates the original distance.
 *
 * @param scalar The scalar to divide by.
 * @throws IllegalArgumentException if [scalar] is zero.
 */
inline operator fun MutableLength3.divAssign(scalar: Int) {
  require(scalar != 0) { "Cannot divide a Length3 by 0" }
  map({ "Division by $scalar" }) { _, value -> value / scalar.toLong() }
}

/**
 * Returns a new [ImmutableLength3] with all components divided by the given scalar.
 * The original distance remains unchanged.
 *
 * @param scalar The scalar to divide by.
 * @return A new distance with divided components.
 * @throws IllegalArgumentException if [scalar] is zero.
 */
inline operator fun Length3.div(scalar: Int): ImmutableLength3 =
  Length3.copyOf(this) {
    this /= scalar
  }

/**
 * Divides all components of this mutable distance by the given scalar.
 * This operation mutates the original distance.
 *
 * @param scalar The scalar to divide by.
 * @throws IllegalArgumentException if [scalar] is zero.
 */
inline operator fun MutableLength3.divAssign(scalar: Double) {
  require(scalar != 0.0) { "Cannot divide a Length3 by 0.0" }
  map({ "Division by $scalar" }) { _, value -> value / scalar }
}

/**
 * Returns a new [ImmutableLength3] with all components divided by the given scalar.
 * The original distance remains unchanged.
 *
 * @param scalar The scalar to divide by.
 * @return A new distance with divided components.
 * @throws IllegalArgumentException if [scalar] is zero.
 */
inline operator fun Length3.div(scalar: Double): ImmutableLength3 =
  Length3.copyOf(this) {
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
inline fun MutableLength3.map(
  noinline actionName: (() -> String)? = null,
  crossinline action: (index: Int, value: Length) -> Length,
) {
  mutate {
    val newDx = action(0, dx)
    val newDy = action(1, dy)
    val newDz = action(2, dz)
    dx = newDx
    dy = newDy
    dz = newDz
  }
}

// endregion

// region implementations

private val ONE_METER: Length = 1L.meters
private val LENGTH3_ZERO: ImmutableLength3 = ImmutableLength3Impl(Length.ZERO, Length.ZERO, Length.ZERO)
private val LENGTH3_UP: ImmutableLength3 = ImmutableLength3Impl(Length.ZERO, ONE_METER, Length.ZERO)
private val LENGTH3_NORTH: ImmutableLength3 = ImmutableLength3Impl(Length.ZERO, Length.ZERO, -ONE_METER)
private val LENGTH3_EAST: ImmutableLength3 = ImmutableLength3Impl(ONE_METER, Length.ZERO, Length.ZERO)
private val LENGTH3_SOUTH: ImmutableLength3 = ImmutableLength3Impl(Length.ZERO, Length.ZERO, ONE_METER)
private val LENGTH3_WEST: ImmutableLength3 = ImmutableLength3Impl(-ONE_METER, Length.ZERO, Length.ZERO)
private val LENGTH3_DOWN: ImmutableLength3 = ImmutableLength3Impl(Length.ZERO, -ONE_METER, Length.ZERO)

private data class ImmutableLength3Impl(
  override var dx: Length,
  override var dy: Length,
  override var dz: Length,
) : ImmutableLength3 {
  override fun toString(): String = "Length3(dx=$dx, dy=$dy, dz=$dz)"

  override fun equals(other: Any?): Boolean =
    when {
      this === other -> true
      other !is Length3 -> false
      else -> componentsEqual(this, other)
    }

  override fun hashCode(): Int = componentsHash(dx, dy, dz)
}

private value class Length3MutableWrapper(
  private val impl: ImmutableLength3Impl,
) : MutableLength3 {
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

  override fun toString(): String = "Length3(dx=$dx, dy=$dy, dz=$dz)"

  override val dxFlow: StateFlow<Length>
    get() = throw UnsupportedOperationException()
  override val dyFlow: StateFlow<Length>
    get() = throw UnsupportedOperationException()
  override val dzFlow: StateFlow<Length>
    get() = throw UnsupportedOperationException()

  override fun observe(): ObserveTicket = throw UnsupportedOperationException()
}

private class MutableLength3Impl(
  dx: Length,
  dy: Length,
  dz: Length,
) : MutableLength3 {
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

  override fun mutate(action: MutableLength3.() -> Unit) {
    lock.withLock { action(this) }
  }

  override fun toString(): String = "Length3(dx=$dx, dy=$dy, dz=$dz)"

  override fun equals(other: Any?): Boolean =
    when {
      this === other -> true
      other !is Length3 -> false
      else -> componentsEqual(this, other)
    }

  override fun hashCode(): Int = componentsHash(dx, dy, dz)

  override fun observe(): ObserveTicket = Ticket(this)

  private class Ticket(
    original: MutableLength3Impl,
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
  a: Length3,
  b: Length3,
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
