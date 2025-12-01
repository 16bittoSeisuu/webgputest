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
 * This struct may be mutable. If so, then `is MutableDistance3d == true`.
 * [dx], [dy], and [dz] are all expected to be finite values
 * (`Double.isFinite() == true`).
 *
 * @author Int16
 */
sealed interface Distance3d {
  /**
   * The delta along the x-axis. Always expected to be a finite value.
   */
  val dx: Double

  /**
   * The delta along the y-axis. Always expected to be a finite value.
   */
  val dy: Double

  /**
   * The delta along the z-axis. Always expected to be a finite value.
   */
  val dz: Double

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

  companion object
}

/**
 * Represents an immutable distance in 3D space.
 * Because it is immutable, all operations produce a new instance.
 * To preserve immutability, users cannot implement this interface.
 * [dx], [dy], and [dz] are all expected to be finite values
 * ([Double.isFinite()] == true).
 */
sealed interface ImmutableDistance3d : Distance3d

/**
 * Represents a mutable distance in 3D space.
 * Changes in value can be monitored via [StateFlow] and [Observable.observe].
 * It is expected that [dx], [dy], and [dz] are all finite values
 * (Double.isFinite() == true).
 */
interface MutableDistance3d :
  Distance3d,
  Observable {
  override var dx: Double
  override var dy: Double
  override var dz: Double

  /**
   * A [StateFlow] that emits the current delta along the x-axis.
   * Always expected to emit finite values.
   */
  val dxFlow: StateFlow<Double>

  /**
   * A [StateFlow] that emits the current delta along the y-axis.
   * Always expected to emit finite values.
   */
  val dyFlow: StateFlow<Double>

  /**
   * A [StateFlow] that emits the current delta along the z-axis.
   * Always expected to emit finite values.
   */
  val dzFlow: StateFlow<Double>

  override fun observe(): ObserveTicket

  companion object
}

// endregion

// region constants

/**
 * The zero distance (0, 0, 0) in three-dimensional space.
 */
val Distance3d.Companion.zero: ImmutableDistance3d get() = DISTANCE3D_ZERO

// endregion

// region factory functions

/**
 * Creates a [Distance3d] by specifying each component in three-dimensional space.
 * You can treat it as a [MutableDistance3d] only at the very beginning using
 * a [mutator], but after that, it is frozen as an [ImmutableDistance3d].
 * Even if you use `as MutableDistance3d` after freezing, the value cannot be
 * changed and will result in an error.
 *
 * @param dx The delta along the x-axis. Expected to be finite.
 * @param dy The delta along the y-axis. Expected to be finite.
 * @param dz The delta along the z-axis. Expected to be finite.
 * @param mutator A scope for [MutableDistance3d] for initialization. If null, nothing is done.
 * @return The frozen, immutable [ImmutableDistance3d].
 */
@Suppress("FunctionName")
fun Distance3d(
  dx: Double = 0.0,
  dy: Double = 0.0,
  dz: Double = 0.0,
  mutator: (MutableDistance3d.() -> Unit)? = null,
): ImmutableDistance3d {
  require(dx.isFinite() && dy.isFinite() && dz.isFinite()) {
    "Distance3d components must be finite values: ($dx, $dy, $dz)"
  }
  if (dx == 0.0 && dy == 0.0 && dz == 0.0 && mutator == null) {
    return Distance3d.zero
  }
  val impl = ImmutableDistance3dImpl(dx, dy, dz)
  if (mutator != null) {
    val mutableWrapper = Distance3dMutableWrapper(impl)
    mutator(mutableWrapper)
  }
  return impl
}

/**
 * Creates an [ImmutableDistance3d] by copying an existing one.
 * If the original instance is an [ImmutableDistance3d] and [mutator] is null,
 * the same instance is returned without creating anything new.
 *
 * @param copyFrom The instance to copy from. This will be reused if possible.
 * @param mutator A [MutableDistance3d] scope to adjust the values immediately after copying.
 * @return The frozen, immutable [ImmutableDistance3d].
 */
inline fun Distance3d.Companion.copyOf(
  copyFrom: Distance3d,
  noinline mutator: (MutableDistance3d.() -> Unit)? = null,
): ImmutableDistance3d =
  if (copyFrom is ImmutableDistance3d && mutator == null) {
    copyFrom
  } else {
    Distance3d(
      dx = copyFrom.dx,
      dy = copyFrom.dy,
      dz = copyFrom.dz,
      mutator = mutator,
    )
  }

/**
 * Creates a [MutableDistance3d] by specifying each component in three-dimensional space.
 *
 * @param dx The delta along the x-axis. Expected to be finite.
 * @param dy The delta along the y-axis. Expected to be finite.
 * @param dz The delta along the z-axis. Expected to be finite.
 * @return The created [MutableDistance3d].
 */
fun MutableDistance3d(
  dx: Double = 0.0,
  dy: Double = 0.0,
  dz: Double = 0.0,
): MutableDistance3d {
  require(dx.isFinite() && dy.isFinite() && dz.isFinite()) {
    "MutableDistance3d components must be finite values: ($dx, $dy, $dz)"
  }
  return MutableDistance3dImpl(dx, dy, dz)
}

/**
 * Creates a [MutableDistance3d] by copying an existing [Distance3d].
 *
 * @param copyFrom The instance to copy from.
 * @return The created [MutableDistance3d].
 */
fun MutableDistance3d.Companion.copyOf(copyFrom: Distance3d): MutableDistance3d = MutableDistance3d(copyFrom.dx, copyFrom.dy, copyFrom.dz)

// endregion

// region arithmetic

/**
 * Returns `true` if this distance is (0, 0, 0),
 * within a small epsilon (1e-5), and `false` otherwise.
 *
 * @return Whether this distance is approximately zero.
 */
inline val Distance3d.isZero: Boolean
  get() {
    val eps = 1e-5
    return abs(dx) < eps && abs(dy) < eps && abs(dz) < eps
  }

/**
 * Returns the Euclidean magnitude of this distance.
 * Uses a numerically stable algorithm to avoid overflow/underflow.
 *
 * @return The magnitude of this distance.
 */
inline val Distance3d.magnitude: Double
  get() {
    val max = maxOf(abs(dx), abs(dy), abs(dz))
    if (max == 0.0) return 0.0
    val minThreshold = 1e-154
    val maxThreshold = 1e154
    return if (minThreshold < max && max < maxThreshold) {
      sqrt(dx * dx + dy * dy + dz * dz)
    } else {
      hypot(hypot(dx, dy), dz)
    }
  }

/**
 * Returns the dot product of this distance with [other].
 *
 * @param other The distance to take the dot product with.
 * @return The scalar dot product.
 */
inline infix fun Distance3d.dot(other: Distance3d): Double = dx * other.dx + dy * other.dy + dz * other.dz

/**
 * Returns the cross product of this distance with [other].
 *
 * @param other The distance to take the cross product with.
 * @return A new [Distance3d] representing the cross product.
 */
inline infix fun Distance3d.cross(other: Distance3d): Distance3d =
  Distance3d(
    dx = dy * other.dz - dz * other.dy,
    dy = dz * other.dx - dx * other.dz,
    dz = dx * other.dy - dy * other.dx,
  )

/**
 * Negates all components of this mutable distance.
 * After this operation, dx, dy, and dz become -dx, -dy, and -dz respectively.
 * This operation mutates the original distance.
 */
inline fun MutableDistance3d.negate() = map("Negation") { _, value -> -value }

/**
 * Returns a new [ImmutableDistance3d] with all components negated.
 * The original distance remains unchanged.
 *
 * @return A new distance with negated components.
 */
inline operator fun Distance3d.unaryMinus(): ImmutableDistance3d =
  Distance3d.copyOf(this) {
    negate()
  }

/**
 * Adds the corresponding components of [other] to this mutable distance.
 * Mutates this distance.
 *
 * @param other The distance to add.
 */
inline operator fun MutableDistance3d.plusAssign(other: Distance3d) =
  map("Addition of $other") { index, value ->
    when (index) {
      0 -> value + other.dx
      1 -> value + other.dy
      else -> value + other.dz
    }
  }

/**
 * Returns a new [ImmutableDistance3d] representing the sum of this distance and [other].
 *
 * @param other The distance to add.
 * @return A new immutable distance containing the component-wise sum.
 */
inline operator fun Distance3d.plus(other: Distance3d): ImmutableDistance3d =
  Distance3d.copyOf(this) {
    this += other
  }

/**
 * Subtracts the corresponding components of [other] from this mutable distance.
 * Mutates this distance.
 *
 * @param other The distance to subtract.
 */
inline operator fun MutableDistance3d.minusAssign(other: Distance3d) =
  map("Subtraction of $other") { index, value ->
    when (index) {
      0 -> value - other.dx
      1 -> value - other.dy
      else -> value - other.dz
    }
  }

/**
 * Returns a new [ImmutableDistance3d] representing the difference of this distance and [other].
 *
 * @param other The distance to subtract.
 * @return A new immutable distance containing the component-wise difference.
 */
inline operator fun Distance3d.minus(other: Distance3d): ImmutableDistance3d =
  Distance3d.copyOf(this) {
    this -= other
  }

/**
 * Multiplies all components of this mutable distance by the given scalar.
 * This operation mutates the original distance.
 *
 * @param scalar The scalar to multiply by.
 */
inline operator fun MutableDistance3d.timesAssign(scalar: Int) = map("Multiplication by $scalar") { _, value -> value * scalar }

/**
 * Returns a new [ImmutableDistance3d] with all components multiplied by the given scalar.
 * The original distance remains unchanged.
 *
 * @param scalar The scalar to multiply by.
 * @return A new distance with multiplied components.
 */
inline operator fun Distance3d.times(scalar: Int): ImmutableDistance3d =
  Distance3d.copyOf(this) {
    this *= scalar
  }

/**
 * Multiplies all components of this mutable distance by the given scalar.
 * This operation mutates the original distance.
 *
 * @param scalar The scalar to multiply by.
 */
inline operator fun MutableDistance3d.timesAssign(scalar: Double) = map("Multiplication by $scalar") { _, value -> value * scalar }

/**
 * Returns a new [ImmutableDistance3d] with all components multiplied by the given scalar.
 * The original distance remains unchanged.
 *
 * @param scalar The scalar to multiply by.
 * @return A new distance with multiplied components.
 */
inline operator fun Distance3d.times(scalar: Double): ImmutableDistance3d =
  Distance3d.copyOf(this) {
    this *= scalar
  }

/**
 * Divides all components of this mutable distance by the given scalar.
 * This operation mutates the original distance.
 *
 * @param scalar The scalar to divide by.
 * @throws IllegalArgumentException if [scalar] is zero.
 */
inline operator fun MutableDistance3d.divAssign(scalar: Int) {
  require(scalar != 0) { "Cannot divide a Distance3d by 0" }
  map("Division by $scalar") { _, value -> value / scalar }
}

/**
 * Returns a new [ImmutableDistance3d] with all components divided by the given scalar.
 * The original distance remains unchanged.
 *
 * @param scalar The scalar to divide by.
 * @return A new distance with divided components.
 * @throws IllegalArgumentException if [scalar] is zero.
 */
inline operator fun Distance3d.div(scalar: Int): ImmutableDistance3d =
  Distance3d.copyOf(this) {
    this /= scalar
  }

/**
 * Divides all components of this mutable distance by the given scalar.
 * This operation mutates the original distance.
 *
 * @param scalar The scalar to divide by.
 * @throws IllegalArgumentException if [scalar] is zero.
 */
inline operator fun MutableDistance3d.divAssign(scalar: Double) {
  require(scalar != 0.0) { "Cannot divide a Distance3d by 0.0" }
  map("Division by $scalar") { _, value -> value / scalar }
}

/**
 * Returns a new [ImmutableDistance3d] with all components divided by the given scalar.
 * The original distance remains unchanged.
 *
 * @param scalar The scalar to divide by.
 * @return A new distance with divided components.
 * @throws IllegalArgumentException if [scalar] is zero.
 */
inline operator fun Distance3d.div(scalar: Double): ImmutableDistance3d =
  Distance3d.copyOf(this) {
    this /= scalar
  }

/**
 * Maps each component of this mutable distance using the given [action],
 * and updates the components with the results.
 * The [action] receives the index (0 for dx, 1 for dy, 2 for dz)
 * and the current value of the component, and should return
 * the new value for that component.
 * After applying the [action], it is verified that all new values
 * are finite; if not, an [IllegalArgumentException] is thrown.
 *
 * @param actionName An optional name for the action, used in error messages.
 * @param action The mapping function to apply to each component.
 * 0: dx, 1: dy, 2: dz.
 * @throws IllegalArgumentException if any resulting component is not finite.
 */
inline fun MutableDistance3d.map(
  actionName: String? = null,
  action: (index: Int, value: Double) -> Double,
) {
  val newDx = action(0, dx)
  val newDy = action(1, dy)
  val newDz = action(2, dz)
  require(newDx.isFinite() && newDy.isFinite() && newDz.isFinite()) {
    if (actionName == null) {
      "Mutation resulted in non-finite values: ($dx, $dy, $dz)"
    } else {
      "$actionName resulted in non-finite values: ($dx, $dy, $dz)"
    }
  }
  dx = newDx
  dy = newDy
  dz = newDz
}

// endregion

// region implementations

private val DISTANCE3D_ZERO: ImmutableDistance3d = ImmutableDistance3dImpl(0.0, 0.0, 0.0)

private data class ImmutableDistance3dImpl(
  override var dx: Double,
  override var dy: Double,
  override var dz: Double,
) : ImmutableDistance3d

private value class Distance3dMutableWrapper(
  private val impl: ImmutableDistance3dImpl,
) : MutableDistance3d {
  override var dx: Double
    get() = impl.dx
    set(value) {
      impl.dx = value
    }
  override var dy: Double
    get() = impl.dy
    set(value) {
      impl.dy = value
    }
  override var dz: Double
    get() = impl.dz
    set(value) {
      impl.dz = value
    }
  override val dxFlow: StateFlow<Double>
    get() = throw UnsupportedOperationException()
  override val dyFlow: StateFlow<Double>
    get() = throw UnsupportedOperationException()
  override val dzFlow: StateFlow<Double>
    get() = throw UnsupportedOperationException()

  override fun observe(): ObserveTicket = throw UnsupportedOperationException()
}

private class MutableDistance3dImpl(
  dx: Double,
  dy: Double,
  dz: Double,
) : MutableDistance3d {
  private var generation: Int = 0
  private val lock = ReentrantLock()
  private val _dxFlow: MutableStateFlow<Double> = MutableStateFlow(dx)
  private val _dyFlow: MutableStateFlow<Double> = MutableStateFlow(dy)
  private val _dzFlow: MutableStateFlow<Double> = MutableStateFlow(dz)

  override var dx: Double
    get() = lock.withLock { _dxFlow.value }
    set(value) {
      lock.withLock {
        generation++
        _dxFlow.value = value
      }
    }
  override var dy: Double
    get() = lock.withLock { _dyFlow.value }
    set(value) {
      lock.withLock {
        generation++
        _dyFlow.value = value
      }
    }
  override var dz: Double
    get() = lock.withLock { _dzFlow.value }
    set(value) {
      lock.withLock {
        generation++
        _dzFlow.value = value
      }
    }

  override val dxFlow: StateFlow<Double> get() = _dxFlow.asStateFlow()
  override val dyFlow: StateFlow<Double> get() = _dyFlow.asStateFlow()
  override val dzFlow: StateFlow<Double> get() = _dzFlow.asStateFlow()

  override fun observe(): ObserveTicket = Ticket(this)

  private class Ticket(
    original: MutableDistance3dImpl,
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

// endregion
