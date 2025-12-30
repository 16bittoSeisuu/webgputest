@file:OptIn(ExperimentalAtomicApi::class)
@file:Suppress("NOTHING_TO_INLINE")

package net.japanesehunter.math

import kotlinx.atomicfu.locks.ReentrantLock
import kotlinx.atomicfu.locks.withLock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.time.Duration

// region interfaces

/**
 * Represents an acceleration in 3D space.
 * The components are stored as acceleration values.
 * This struct may be mutable. If so, then `is MutableAcceleration3 == true`.
 *
 * Implementations guarantee that [ax], [ay], and [az] are interpreted as components of a single acceleration vector.
 * Mutable implementations are expected to update the three components atomically when used through [MutableAcceleration3.mutate].
 *
 * Implementations are not required to be thread-safe.
 *
 * @author Int16
 */
sealed interface Acceleration3 {
  /**
   * The acceleration along the x-axis.
   */
  val ax: Acceleration

  /**
   * The acceleration along the y-axis.
   */
  val ay: Acceleration

  /**
   * The acceleration along the z-axis.
   */
  val az: Acceleration

  /**
   * Component operator for destructuring declarations.
   */
  operator fun component1() =
    ax

  /**
   * Component operator for destructuring declarations.
   */
  operator fun component2() =
    ay

  /**
   * Component operator for destructuring declarations.
   */
  operator fun component3() =
    az

  override fun toString(): String

  override fun equals(
    other: Any?,
  ): Boolean

  override fun hashCode(): Int

  companion object
}

/**
 * Represents an immutable acceleration in 3D space.
 * Because it is immutable, all operations produce a new instance.
 * To preserve immutability, users cannot implement this interface.
 *
 * Instances are inherently thread-safe.
 */
sealed interface ImmutableAcceleration3 : Acceleration3

/**
 * Represents a mutable acceleration in 3D space.
 * Changes in value can be monitored via [StateFlow] and [Observable.observe].
 *
 * Implementations are not required to be thread-safe.
 */
interface MutableAcceleration3 :
  Acceleration3,
  Observable {
  override var ax: Acceleration
  override var ay: Acceleration
  override var az: Acceleration

  /**
   * A [StateFlow] that emits the current x-axis acceleration.
   */
  val axFlow: StateFlow<Acceleration>

  /**
   * A [StateFlow] that emits the current y-axis acceleration.
   */
  val ayFlow: StateFlow<Acceleration>

  /**
   * A [StateFlow] that emits the current z-axis acceleration.
   */
  val azFlow: StateFlow<Acceleration>

  /**
   * Runs [action] while holding the internal lock when available so compound operations stay consistent.
   */
  fun mutate(
    action: MutableAcceleration3.() -> Unit,
  ) =
    action(this)

  override fun observe(): ObserveTicket

  companion object
}

// endregion

// region constants

/**
 * The zero acceleration (0, 0, 0).
 */
val Acceleration3.Companion.zero: ImmutableAcceleration3
  get() = ACCELERATION3_ZERO

// endregion

// region factory functions

/**
 * Creates an [Acceleration3] by specifying each component in three-dimensional space.
 * You can treat it as a [MutableAcceleration3] only at the very beginning using
 * a [mutator], but after that, it is frozen as an [ImmutableAcceleration3].
 * Even if you use `as MutableAcceleration3` after freezing, the value cannot be
 * changed and will result in an error.
 *
 * @param ax The x-axis acceleration.
 * @param ay The y-axis acceleration.
 * @param az The z-axis acceleration.
 * @param mutator A scope for [MutableAcceleration3] for initialization.
 * @return The frozen, immutable acceleration.
 */
@Suppress("FunctionName")
fun Acceleration3(
  ax: Acceleration = Acceleration.ZERO,
  ay: Acceleration = Acceleration.ZERO,
  az: Acceleration = Acceleration.ZERO,
  mutator: (MutableAcceleration3.() -> Unit)? = null,
): ImmutableAcceleration3 {
  if (mutator == null) {
    if (ax.isZero && ay.isZero && az.isZero) return Acceleration3.zero
  }
  val impl = ImmutableAcceleration3Impl(ax, ay, az)
  if (mutator != null) {
    val mutableWrapper = Acceleration3MutableWrapper(impl)
    mutator(mutableWrapper)
  }
  return impl
}

/**
 * Creates an [ImmutableAcceleration3] by copying an existing one.
 * If the original instance is an [ImmutableAcceleration3] and [mutator] is null,
 * the same instance is returned without creating anything new.
 *
 * @param copyFrom The instance to copy from.
 * @param mutator A [MutableAcceleration3] scope to adjust the values immediately after copying.
 * @return The frozen, immutable acceleration.
 */
inline fun Acceleration3.Companion.copyOf(
  copyFrom: Acceleration3,
  noinline mutator: (MutableAcceleration3.() -> Unit)? = null,
): ImmutableAcceleration3 =
  if (copyFrom is ImmutableAcceleration3 && mutator == null) {
    copyFrom
  } else {
    Acceleration3(
      ax = copyFrom.ax,
      ay = copyFrom.ay,
      az = copyFrom.az,
      mutator = mutator,
    )
  }

/**
 * Creates a [MutableAcceleration3] by specifying each component in three-dimensional space.
 *
 * @param ax The x-axis acceleration.
 * @param ay The y-axis acceleration.
 * @param az The z-axis acceleration.
 * @return The created mutable acceleration.
 */
fun MutableAcceleration3(
  ax: Acceleration = Acceleration.ZERO,
  ay: Acceleration = Acceleration.ZERO,
  az: Acceleration = Acceleration.ZERO,
): MutableAcceleration3 =
  MutableAcceleration3Impl(ax, ay, az)

/**
 * Creates a [MutableAcceleration3] by copying an existing [Acceleration3].
 *
 * @param copyFrom The instance to copy from.
 * @return The created mutable acceleration.
 */
fun MutableAcceleration3.Companion.copyOf(
  copyFrom: Acceleration3,
): MutableAcceleration3 =
  MutableAcceleration3(copyFrom.ax, copyFrom.ay, copyFrom.az)

// endregion

// region arithmetic

/**
 * Whether this acceleration is exactly (0, 0, 0).
 */
inline val Acceleration3.isZero: Boolean
  get() = ax.isZero && ay.isZero && az.isZero

/**
 * Negates all components of this mutable acceleration.
 * After this operation, ax, ay, and az become -ax, -ay, and -az respectively.
 */
fun MutableAcceleration3.negate() =
  mutate {
    ax = -ax
    ay = -ay
    az = -az
  }

/**
 * Returns a new [ImmutableAcceleration3] with all components negated.
 * This operation does not mutate the receiver.
 *
 * @return A new acceleration with negated components.
 */
operator fun Acceleration3.unaryMinus(): ImmutableAcceleration3 =
  Acceleration3.copyOf(this) {
    negate()
  }

/**
 * Adds the corresponding components of [other] to this mutable acceleration.
 *
 * @param other The acceleration to add.
 */
operator fun MutableAcceleration3.plusAssign(
  other: Acceleration3,
) =
  mutate {
    ax += other.ax
    ay += other.ay
    az += other.az
  }

/**
 * Returns a new [ImmutableAcceleration3] representing the sum of this acceleration and [other].
 * This operation does not mutate the receiver.
 *
 * @param other The acceleration to add.
 * @return A new acceleration containing the component-wise sum.
 */
operator fun Acceleration3.plus(
  other: Acceleration3,
): ImmutableAcceleration3 =
  Acceleration3.copyOf(this) {
    this += other
  }

/**
 * Subtracts the corresponding components of [other] from this mutable acceleration.
 *
 * @param other The acceleration to subtract.
 */
operator fun MutableAcceleration3.minusAssign(
  other: Acceleration3,
) =
  mutate {
    ax -= other.ax
    ay -= other.ay
    az -= other.az
  }

/**
 * Returns a new [ImmutableAcceleration3] representing the difference of this acceleration and [other].
 * This operation does not mutate the receiver.
 *
 * @param other The acceleration to subtract.
 * @return A new acceleration containing the component-wise difference.
 */
operator fun Acceleration3.minus(
  other: Acceleration3,
): ImmutableAcceleration3 =
  Acceleration3.copyOf(this) {
    this -= other
  }

/**
 * Multiplies this acceleration by a finite scalar factor.
 * This operation does not mutate the receiver.
 *
 * @param factor The scale factor.
 *   NaN: throws [IllegalArgumentException]
 *   Infinity: throws [IllegalArgumentException]
 * @return A new acceleration scaled by [factor].
 * @throws IllegalArgumentException if [factor] is not finite.
 * @throws ArithmeticException if the calculation overflows.
 */
operator fun Acceleration3.times(
  factor: Double,
): ImmutableAcceleration3 =
  Acceleration3(
    ax = ax * factor,
    ay = ay * factor,
    az = az * factor,
  )

/**
 * Multiplies this acceleration by a scalar Long factor.
 * This operation does not mutate the receiver.
 *
 * @param factor The scale factor.
 * @return A new acceleration scaled by [factor].
 * @throws ArithmeticException if the calculation overflows.
 */
operator fun Acceleration3.times(
  factor: Long,
): ImmutableAcceleration3 =
  Acceleration3(
    ax = ax * factor,
    ay = ay * factor,
    az = az * factor,
  )

/**
 * Divides this acceleration by a finite non-zero scalar factor.
 * This operation does not mutate the receiver.
 *
 * @param divisor The divisor.
 *   NaN: throws [IllegalArgumentException]
 *   Infinity: throws [IllegalArgumentException]
 * @return A new acceleration divided by [divisor].
 * @throws IllegalArgumentException if [divisor] is not finite or is zero.
 * @throws ArithmeticException if the calculation overflows.
 */
operator fun Acceleration3.div(
  divisor: Double,
): ImmutableAcceleration3 =
  Acceleration3(
    ax = ax / divisor,
    ay = ay / divisor,
    az = az / divisor,
  )

/**
 * Divides this acceleration by a non-zero scalar Long divisor.
 * This operation does not mutate the receiver.
 *
 * @param divisor The divisor.
 * @return A new acceleration divided by [divisor].
 * @throws ArithmeticException if [divisor] is zero.
 */
operator fun Acceleration3.div(
  divisor: Long,
): ImmutableAcceleration3 =
  Acceleration3(
    ax = ax / divisor,
    ay = ay / divisor,
    az = az / divisor,
  )

/**
 * Converts this acceleration into a velocity change over [duration].
 * This operation does not mutate the receiver.
 *
 * @param duration The time interval.
 * @return A velocity equal to this acceleration integrated over [duration].
 * @throws IllegalArgumentException if [duration] is not finite.
 * @throws ArithmeticException if the calculation overflows.
 */
operator fun Acceleration3.times(
  duration: Duration,
): ImmutableVelocity3 {
  require(duration.isFinite()) { "Duration must be finite: $duration" }
  return Velocity3(
    vx = ax * duration,
    vy = ay * duration,
    vz = az * duration,
  )
}

// endregion

// region implementations

private val ACCELERATION3_ZERO: ImmutableAcceleration3 =
  ImmutableAcceleration3Impl(
    Acceleration.ZERO,
    Acceleration.ZERO,
    Acceleration.ZERO,
  )

private data class ImmutableAcceleration3Impl(
  override var ax: Acceleration,
  override var ay: Acceleration,
  override var az: Acceleration,
) : ImmutableAcceleration3 {
  override fun toString(): String =
    "Acceleration3(ax=$ax, ay=$ay, az=$az)"

  override fun equals(
    other: Any?,
  ): Boolean =
    when {
      this === other -> true
      other !is Acceleration3 -> false
      else -> ax == other.ax && ay == other.ay && az == other.az
    }

  override fun hashCode(): Int =
    componentsHash(ax, ay, az)
}

private value class Acceleration3MutableWrapper(
  private val impl: ImmutableAcceleration3Impl,
) : MutableAcceleration3 {
  override var ax: Acceleration
    get() = impl.ax
    set(value) {
      impl.ax = value
    }

  override var ay: Acceleration
    get() = impl.ay
    set(value) {
      impl.ay = value
    }

  override var az: Acceleration
    get() = impl.az
    set(value) {
      impl.az = value
    }

  override val axFlow: StateFlow<Acceleration>
    get() = throw UnsupportedOperationException()
  override val ayFlow: StateFlow<Acceleration>
    get() = throw UnsupportedOperationException()
  override val azFlow: StateFlow<Acceleration>
    get() = throw UnsupportedOperationException()

  override fun observe(): ObserveTicket =
    throw UnsupportedOperationException()
}

private class MutableAcceleration3Impl(
  ax: Acceleration,
  ay: Acceleration,
  az: Acceleration,
) : MutableAcceleration3 {
  private var generation: Int = 0
  private val lock = ReentrantLock()
  private val _axFlow: MutableStateFlow<Acceleration> = MutableStateFlow(ax)
  private val _ayFlow: MutableStateFlow<Acceleration> = MutableStateFlow(ay)
  private val _azFlow: MutableStateFlow<Acceleration> = MutableStateFlow(az)

  override var ax: Acceleration
    get() = lock.withLock { _axFlow.value }
    set(value) {
      lock.withLock {
        generation++
        _axFlow.value = value
      }
    }

  override var ay: Acceleration
    get() = lock.withLock { _ayFlow.value }
    set(value) {
      lock.withLock {
        generation++
        _ayFlow.value = value
      }
    }

  override var az: Acceleration
    get() = lock.withLock { _azFlow.value }
    set(value) {
      lock.withLock {
        generation++
        _azFlow.value = value
      }
    }

  override val axFlow: StateFlow<Acceleration> get() = _axFlow.asStateFlow()
  override val ayFlow: StateFlow<Acceleration> get() = _ayFlow.asStateFlow()
  override val azFlow: StateFlow<Acceleration> get() = _azFlow.asStateFlow()

  override fun mutate(
    action: MutableAcceleration3.() -> Unit,
  ) {
    lock.withLock { action(this) }
  }

  override fun toString(): String =
    "Acceleration3(ax=$ax, ay=$ay, az=$az)"

  override fun equals(
    other: Any?,
  ): Boolean =
    when {
      this === other -> true
      other !is Acceleration3 -> false
      else -> ax == other.ax && ay == other.ay && az == other.az
    }

  override fun hashCode(): Int =
    componentsHash(ax, ay, az)

  override fun observe(): ObserveTicket =
    Ticket(this)

  private class Ticket(original: MutableAcceleration3Impl) : ObserveTicket {
    private val weakOriginal by WeakProperty(original)
    private val knownGeneration: Int =
      original.lock.withLock {
        original.generation
      }

    override val isDirty: Boolean
      get() =
        weakOriginal?.let {
          it.lock
            .withLock { it.generation != knownGeneration }
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

private fun componentsHash(
  ax: Acceleration,
  ay: Acceleration,
  az: Acceleration,
): Int {
  var result = ax.hashCode()
  result = 31 * result + ay.hashCode()
  result = 31 * result + az.hashCode()
  return result
}

// endregion
