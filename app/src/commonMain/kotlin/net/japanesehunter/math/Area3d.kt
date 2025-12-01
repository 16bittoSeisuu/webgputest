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
 * Represents an oriented area vector in 3D space.
 * Components [ax], [ay], and [az] are stored as [Area] values.
 *
 * @author Int16
 */
sealed interface Area3d {
  /**
   * The component along the x-axis stored as an [Area].
   */
  val ax: Area

  /**
   * The component along the y-axis stored as an [Area].
   */
  val ay: Area

  /**
   * The component along the z-axis stored as an [Area].
   */
  val az: Area

  /**
   * Component operator for destructuring declarations.
   */
  operator fun component1() = ax

  /**
   * Component operator for destructuring declarations.
   */
  operator fun component2() = ay

  /**
   * Component operator for destructuring declarations.
   */
  operator fun component3() = az

  override fun toString(): String

  override fun equals(other: Any?): Boolean

  override fun hashCode(): Int

  companion object
}

/**
 * Represents an immutable area in 3D space.
 * Because it is immutable, all operations produce a new instance.
 * To preserve immutability, users cannot implement this interface.
 */
sealed interface ImmutableArea3d : Area3d

/**
 * Represents a mutable area in 3D space.
 * Changes in value can be monitored via [StateFlow] and [Observable.observe].
 */
interface MutableArea3d :
  Area3d,
  Observable {
  override var ax: Area
  override var ay: Area
  override var az: Area

  /**
   * A [StateFlow] that emits the current x component.
   */
  val axFlow: StateFlow<Area>

  /**
   * A [StateFlow] that emits the current y component.
   */
  val ayFlow: StateFlow<Area>

  /**
   * A [StateFlow] that emits the current z component.
   */
  val azFlow: StateFlow<Area>

  override fun observe(): ObserveTicket

  companion object
}

// endregion

// region constants

/**
 * The zero area vector (0, 0, 0) in nanometer².
 */
val Area3d.Companion.zero: ImmutableArea3d get() = AREA3D_ZERO

// endregion

// region factory functions

/**
 * Creates an [Area3d] by specifying each component.
 * You can treat it as a [MutableArea3d] only at the very beginning using
 * a [mutator], but after that, it is frozen as an [ImmutableArea3d].
 * Even if you use `as MutableArea3d` after freezing, the value cannot be
 * changed and will result in an error.
 *
 * @param ax The component along the x-axis.
 * @param ay The component along the y-axis.
 * @param az The component along the z-axis.
 * @param mutator A scope for [MutableArea3d] for initialization. If null, nothing is done.
 * @return The frozen, immutable [ImmutableArea3d].
 */
@Suppress("FunctionName")
fun Area3d(
  ax: Area = Area.ZERO,
  ay: Area = Area.ZERO,
  az: Area = Area.ZERO,
  mutator: (MutableArea3d.() -> Unit)? = null,
): ImmutableArea3d {
  if (ax.isZero && ay.isZero && az.isZero && mutator == null) {
    return Area3d.zero
  }
  val impl = ImmutableArea3dImpl(ax, ay, az)
  if (mutator != null) {
    val mutableWrapper = Area3dMutableWrapper(impl)
    mutator(mutableWrapper)
  }
  return impl
}

/**
 * Creates an [ImmutableArea3d] by copying an existing one.
 * If the original instance is an [ImmutableArea3d] and [mutator] is null,
 * the same instance is returned without creating anything new.
 *
 * @param copyFrom The instance to copy from. This will be reused if possible.
 * @param mutator A [MutableArea3d] scope to adjust the values immediately after copying.
 * @return The frozen, immutable [ImmutableArea3d].
 */
inline fun Area3d.Companion.copyOf(
  copyFrom: Area3d,
  noinline mutator: (MutableArea3d.() -> Unit)? = null,
): ImmutableArea3d =
  if (copyFrom is ImmutableArea3d && mutator == null) {
    copyFrom
  } else {
    Area3d(
      ax = copyFrom.ax,
      ay = copyFrom.ay,
      az = copyFrom.az,
      mutator = mutator,
    )
  }

/**
 * Creates a [MutableArea3d] by specifying each component.
 *
 * @param ax The component along the x-axis.
 * @param ay The component along the y-axis.
 * @param az The component along the z-axis.
 * @return The created [MutableArea3d].
 */
fun MutableArea3d(
  ax: Area = Area.ZERO,
  ay: Area = Area.ZERO,
  az: Area = Area.ZERO,
): MutableArea3d {
  return MutableArea3dImpl(ax, ay, az)
}

/**
 * Creates a [MutableArea3d] by copying an existing [Area3d].
 *
 * @param copyFrom The instance to copy from.
 * @return The created [MutableArea3d].
 */
fun MutableArea3d.Companion.copyOf(copyFrom: Area3d): MutableArea3d =
  MutableArea3d(copyFrom.ax, copyFrom.ay, copyFrom.az)

// endregion

// region arithmetic

/**
 * Returns `true` if this area is exactly (0, 0, 0).
 */
inline val Area3d.isZero: Boolean
  get() = ax.isZero && ay.isZero && az.isZero

/**
 * Returns the magnitude of this area vector as an [Area].
 * Uses a numerically stable algorithm to avoid overflow/underflow by working
 * in square nanometers.
 */
inline val Area3d.magnitude: Area
  get() {
    val axNm2 = ax.toDouble(AreaUnit.SQUARE_NANOMETER)
    val ayNm2 = ay.toDouble(AreaUnit.SQUARE_NANOMETER)
    val azNm2 = az.toDouble(AreaUnit.SQUARE_NANOMETER)
    val max = maxOf(abs(axNm2), abs(ayNm2), abs(azNm2))
    if (max == 0.0) return Area.ZERO
    val minThreshold = 1e-154
    val maxThreshold = 1e154
    val magnitudeNm2 =
      if (minThreshold < max && max < maxThreshold) {
        sqrt(axNm2 * axNm2 + ayNm2 * ayNm2 + azNm2 * azNm2)
      } else {
        hypot(hypot(axNm2, ayNm2), azNm2)
      }
    return Area.from(magnitudeNm2, AreaUnit.SQUARE_NANOMETER)
  }

/**
 * Returns the dot product of this area with [other] in [unit]⁴.
 *
 * @param other The area to take the dot product with.
 * @return The scalar dot product in [unit]⁴.
 */
inline infix fun Area3d.dot(other: Area3d): Double = dot(other, AreaUnit.SQUARE_NANOMETER)

/**
 * Returns the dot product of this area with [other] in the provided [unit]⁴.
 *
 * @param other The area to take the dot product with.
 * @param unit The squared distance unit to express the result in (raised to the fourth power).
 * @return The scalar dot product in [unit]⁴.
 */
fun Area3d.dot(
  other: Area3d,
  unit: AreaUnit,
): Double {
  val ax = this.ax.toDouble(unit)
  val ay = this.ay.toDouble(unit)
  val az = this.az.toDouble(unit)
  return ax * other.ax.toDouble(unit) + ay * other.ay.toDouble(unit) + az * other.az.toDouble(unit)
}

/**
 * Negates all components of this mutable area.
 */
inline fun MutableArea3d.negate() = map("Negation") { _, value -> -value }

/**
 * Returns a new [ImmutableArea3d] with all components negated.
 */
inline operator fun Area3d.unaryMinus(): ImmutableArea3d =
  Area3d.copyOf(this) {
    negate()
  }

/**
 * Adds the corresponding components of [other] to this mutable area.
 */
inline operator fun MutableArea3d.plusAssign(other: Area3d) {
  map("Addition of $other") { index, value ->
    when (index) {
      0 -> value + other.ax
      1 -> value + other.ay
      else -> value + other.az
    }
  }
}

/**
 * Returns a new [ImmutableArea3d] representing the sum of this area and [other].
 */
inline operator fun Area3d.plus(other: Area3d): ImmutableArea3d =
  Area3d.copyOf(this) {
    this += other
  }

/**
 * Subtracts the corresponding components of [other] from this mutable area.
 */
inline operator fun MutableArea3d.minusAssign(other: Area3d) {
  map("Subtraction of $other") { index, value ->
    when (index) {
      0 -> value - other.ax
      1 -> value - other.ay
      else -> value - other.az
    }
  }
}

/**
 * Returns a new [ImmutableArea3d] representing the difference of this area and [other].
 */
inline operator fun Area3d.minus(other: Area3d): ImmutableArea3d =
  Area3d.copyOf(this) {
    this -= other
  }

/**
 * Multiplies all components of this mutable area by the given scalar.
 * This operation mutates the original area.
 */
inline operator fun MutableArea3d.timesAssign(scalar: Double) =
  map("Multiplication by $scalar") { _, value -> value * scalar }

/**
 * Returns a new [ImmutableArea3d] with all components multiplied by the given scalar.
 */
inline operator fun Area3d.times(scalar: Double): ImmutableArea3d =
  Area3d.copyOf(this) {
    this *= scalar
  }

/**
 * Divides all components of this mutable area by the given scalar.
 * This operation mutates the original area.
 *
 * @throws IllegalArgumentException if [scalar] is zero.
 */
inline operator fun MutableArea3d.divAssign(scalar: Double) {
  require(scalar != 0.0) { "Cannot divide an Area3d by 0.0" }
  map("Division by $scalar") { _, value -> value / scalar }
}

/**
 * Returns a new [ImmutableArea3d] with all components divided by the given scalar.
 *
 * @throws IllegalArgumentException if [scalar] is zero.
 */
inline operator fun Area3d.div(scalar: Double): ImmutableArea3d =
  Area3d.copyOf(this) {
    this /= scalar
  }

/**
 * Maps each component of this mutable area using the given [action],
 * and updates the components with the results.
 * The [action] receives the index (0 for ax, 1 for ay, 2 for az)
 * and the current value of the component, and should return
 * the new value for that component.
 *
 * @param actionName An optional name for the action, used in error messages.
 * @param action The mapping function to apply to each component.
 * 0: ax, 1: ay, 2: az.
 * @throws ArithmeticException if any resulting component is not finite.
 */
@Suppress("UNUSED_PARAMETER")
inline fun MutableArea3d.map(
  actionName: String? = null,
  action: (index: Int, value: Area) -> Area,
) {
  val newAx = action(0, ax)
  val newAy = action(1, ay)
  val newAz = action(2, az)
  ax = newAx
  ay = newAy
  az = newAz
}

// endregion

// region implementations

private val AREA3D_ZERO: ImmutableArea3d = ImmutableArea3dImpl(Area.ZERO, Area.ZERO, Area.ZERO)

private data class ImmutableArea3dImpl(
  override var ax: Area,
  override var ay: Area,
  override var az: Area,
) : ImmutableArea3d {
  override fun toString(): String = "Area3d(ax=$ax, ay=$ay, az=$az)"

  override fun equals(other: Any?): Boolean =
    when {
      this === other -> true
      other !is Area3d -> false
      else -> componentsEqual(this, other)
    }

  override fun hashCode(): Int = componentsHash(ax, ay, az)
}

private value class Area3dMutableWrapper(
  private val impl: ImmutableArea3dImpl,
) : MutableArea3d {
  override var ax: Area
    get() = impl.ax
    set(value) {
      impl.ax = value
    }
  override var ay: Area
    get() = impl.ay
    set(value) {
      impl.ay = value
    }
  override var az: Area
    get() = impl.az
    set(value) {
      impl.az = value
    }

  override fun toString(): String = "Area3d(ax=$ax, ay=$ay, az=$az)"

  override val axFlow: StateFlow<Area>
    get() = throw UnsupportedOperationException()
  override val ayFlow: StateFlow<Area>
    get() = throw UnsupportedOperationException()
  override val azFlow: StateFlow<Area>
    get() = throw UnsupportedOperationException()

  override fun observe(): ObserveTicket = throw UnsupportedOperationException()
}

private class MutableArea3dImpl(
  ax: Area,
  ay: Area,
  az: Area,
) : MutableArea3d {
  private var generation: Int = 0
  private val lock = ReentrantLock()
  private val _axFlow: MutableStateFlow<Area> = MutableStateFlow(ax)
  private val _ayFlow: MutableStateFlow<Area> = MutableStateFlow(ay)
  private val _azFlow: MutableStateFlow<Area> = MutableStateFlow(az)

  override var ax: Area
    get() = lock.withLock { _axFlow.value }
    set(value) {
      lock.withLock {
        generation++
        _axFlow.value = value
      }
    }
  override var ay: Area
    get() = lock.withLock { _ayFlow.value }
    set(value) {
      lock.withLock {
        generation++
        _ayFlow.value = value
      }
    }
  override var az: Area
    get() = lock.withLock { _azFlow.value }
    set(value) {
      lock.withLock {
        generation++
        _azFlow.value = value
      }
    }

  override val axFlow: StateFlow<Area> get() = _axFlow.asStateFlow()
  override val ayFlow: StateFlow<Area> get() = _ayFlow.asStateFlow()
  override val azFlow: StateFlow<Area> get() = _azFlow.asStateFlow()

  override fun toString(): String = "Area3d(ax=$ax, ay=$ay, az=$az)"

  override fun equals(other: Any?): Boolean =
    when {
      this === other -> true
      other !is Area3d -> false
      else -> componentsEqual(this, other)
    }

  override fun hashCode(): Int = componentsHash(ax, ay, az)

  override fun observe(): ObserveTicket = Ticket(this)

  private class Ticket(
    original: MutableArea3dImpl,
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
  a: Area3d,
  b: Area3d,
): Boolean =
  a.ax == b.ax &&
    a.ay == b.ay &&
    a.az == b.az

private fun componentsHash(
  ax: Area,
  ay: Area,
  az: Area,
): Int {
  var result = 17
  result = 31 * result + ax.inWholeSquareNanometers.hashCode()
  result = 31 * result + ay.inWholeSquareNanometers.hashCode()
  result = 31 * result + az.inWholeSquareNanometers.hashCode()
  return result
}

// endregion
