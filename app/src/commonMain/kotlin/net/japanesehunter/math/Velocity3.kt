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
 * Represents a velocity in 3D space.
 * The components are stored as distances per second.
 * This struct may be mutable. If so, then `is MutableVelocity3 == true`.
 *
 * Implementations guarantee that [vx], [vy], and [vz] are interpreted as components of a single velocity vector.
 * Mutable implementations are expected to update the three components atomically when used through [MutableVelocity3.mutate].
 *
 * Implementations are not required to be thread-safe.
 *
 * @author Int16
 */
sealed interface Velocity3 {
  /**
   * The velocity along the x-axis expressed as a distance per second.
   */
  val vx: Length

  /**
   * The velocity along the y-axis expressed as a distance per second.
   */
  val vy: Length

  /**
   * The velocity along the z-axis expressed as a distance per second.
   */
  val vz: Length

  /**
   * Component operator for destructuring declarations.
   */
  operator fun component1() = vx

  /**
   * Component operator for destructuring declarations.
   */
  operator fun component2() = vy

  /**
   * Component operator for destructuring declarations.
   */
  operator fun component3() = vz

  override fun toString(): String

  override fun equals(other: Any?): Boolean

  override fun hashCode(): Int

  companion object
}

/**
 * Represents an immutable velocity in 3D space.
 * Because it is immutable, all operations produce a new instance.
 * To preserve immutability, users cannot implement this interface.
 *
 * Instances are inherently thread-safe.
 */
sealed interface ImmutableVelocity3 : Velocity3

/**
 * Represents a mutable velocity in 3D space.
 * Changes in value can be monitored via [StateFlow] and [Observable.observe].
 *
 * Implementations are not required to be thread-safe.
 */
interface MutableVelocity3 :
  Velocity3,
  Observable {
  override var vx: Length
  override var vy: Length
  override var vz: Length

  /**
   * A [StateFlow] that emits the current x-axis velocity.
   */
  val vxFlow: StateFlow<Length>

  /**
   * A [StateFlow] that emits the current y-axis velocity.
   */
  val vyFlow: StateFlow<Length>

  /**
   * A [StateFlow] that emits the current z-axis velocity.
   */
  val vzFlow: StateFlow<Length>

  /**
   * Runs [action] while holding the internal lock when available so compound operations stay consistent.
   */
  fun mutate(action: MutableVelocity3.() -> Unit) = action(this)

  override fun observe(): ObserveTicket

  companion object
}

// endregion

// region constants

/**
 * The zero velocity (0, 0, 0).
 */
val Velocity3.Companion.zero: ImmutableVelocity3 get() = VELOCITY3_ZERO

// endregion

// region factory functions

/**
 * Creates a [Velocity3] by specifying each component in three-dimensional space.
 * You can treat it as a [MutableVelocity3] only at the very beginning using
 * a [mutator], but after that, it is frozen as an [ImmutableVelocity3].
 * Even if you use `as MutableVelocity3` after freezing, the value cannot be
 * changed and will result in an error.
 *
 * @param vx The x-axis velocity expressed as a distance per second.
 * @param vy The y-axis velocity expressed as a distance per second.
 * @param vz The z-axis velocity expressed as a distance per second.
 * @param mutator A scope for [MutableVelocity3] for initialization.
 * @return The frozen, immutable velocity.
 */
@Suppress("FunctionName")
fun Velocity3(
  vx: Length = Length.ZERO,
  vy: Length = Length.ZERO,
  vz: Length = Length.ZERO,
  mutator: (MutableVelocity3.() -> Unit)? = null,
): ImmutableVelocity3 {
  if (mutator == null) {
    if (vx.isZero && vy.isZero && vz.isZero) return Velocity3.zero
  }
  val impl = ImmutableVelocity3Impl(vx, vy, vz)
  if (mutator != null) {
    val mutableWrapper = Velocity3MutableWrapper(impl)
    mutator(mutableWrapper)
  }
  return impl
}

/**
 * Creates an [ImmutableVelocity3] by copying an existing one.
 * If the original instance is an [ImmutableVelocity3] and [mutator] is null,
 * the same instance is returned without creating anything new.
 *
 * @param copyFrom The instance to copy from.
 * @param mutator A [MutableVelocity3] scope to adjust the values immediately after copying.
 * @return The frozen, immutable velocity.
 */
inline fun Velocity3.Companion.copyOf(
  copyFrom: Velocity3,
  noinline mutator: (MutableVelocity3.() -> Unit)? = null,
): ImmutableVelocity3 =
  if (copyFrom is ImmutableVelocity3 && mutator == null) {
    copyFrom
  } else {
    Velocity3(
      vx = copyFrom.vx,
      vy = copyFrom.vy,
      vz = copyFrom.vz,
      mutator = mutator,
    )
  }

/**
 * Creates a [MutableVelocity3] by specifying each component in three-dimensional space.
 *
 * @param vx The x-axis velocity expressed as a distance per second.
 * @param vy The y-axis velocity expressed as a distance per second.
 * @param vz The z-axis velocity expressed as a distance per second.
 * @return The created mutable velocity.
 */
fun MutableVelocity3(
  vx: Length = Length.ZERO,
  vy: Length = Length.ZERO,
  vz: Length = Length.ZERO,
): MutableVelocity3 = MutableVelocity3Impl(vx, vy, vz)

/**
 * Creates a [MutableVelocity3] by copying an existing [Velocity3].
 *
 * @param copyFrom The instance to copy from.
 * @return The created mutable velocity.
 */
fun MutableVelocity3.Companion.copyOf(copyFrom: Velocity3): MutableVelocity3 = MutableVelocity3(copyFrom.vx, copyFrom.vy, copyFrom.vz)

// endregion

// region arithmetic

/**
 * Whether this velocity is exactly (0, 0, 0).
 */
inline val Velocity3.isZero: Boolean
  get() = vx.isZero && vy.isZero && vz.isZero

/**
 * Negates all components of this mutable velocity.
 * After this operation, vx, vy, and vz become -vx, -vy, and -vz respectively.
 */
fun MutableVelocity3.negate() =
  mutate {
    vx = -vx
    vy = -vy
    vz = -vz
  }

/**
 * Returns a new [ImmutableVelocity3] with all components negated.
 * This operation does not mutate the receiver.
 *
 * @return A new velocity with negated components.
 */
operator fun Velocity3.unaryMinus(): ImmutableVelocity3 =
  Velocity3.copyOf(this) {
    negate()
  }

/**
 * Adds the corresponding components of [other] to this mutable velocity.
 *
 * @param other The velocity to add.
 */
operator fun MutableVelocity3.plusAssign(other: Velocity3) =
  mutate {
    vx += other.vx
    vy += other.vy
    vz += other.vz
  }

/**
 * Returns a new [ImmutableVelocity3] representing the sum of this velocity and [other].
 * This operation does not mutate the receiver.
 *
 * @param other The velocity to add.
 * @return A new velocity containing the component-wise sum.
 */
operator fun Velocity3.plus(other: Velocity3): ImmutableVelocity3 =
  Velocity3.copyOf(this) {
    this += other
  }

/**
 * Subtracts the corresponding components of [other] from this mutable velocity.
 *
 * @param other The velocity to subtract.
 */
operator fun MutableVelocity3.minusAssign(other: Velocity3) =
  mutate {
    vx -= other.vx
    vy -= other.vy
    vz -= other.vz
  }

/**
 * Returns a new [ImmutableVelocity3] representing the difference of this velocity and [other].
 * This operation does not mutate the receiver.
 *
 * @param other The velocity to subtract.
 * @return A new velocity containing the component-wise difference.
 */
operator fun Velocity3.minus(other: Velocity3): ImmutableVelocity3 =
  Velocity3.copyOf(this) {
    this -= other
  }

/**
 * Multiplies this velocity by a finite scalar factor.
 * This operation does not mutate the receiver.
 *
 * @param factor The scale factor.
 *   NaN: throws [IllegalArgumentException]
 *   Infinity: throws [IllegalArgumentException]
 * @return A new velocity scaled by [factor].
 * @throws IllegalArgumentException if [factor] is not finite.
 * @throws ArithmeticException if the calculation overflows.
 */
operator fun Velocity3.times(factor: Double): ImmutableVelocity3 =
  Velocity3(
    vx = vx * factor,
    vy = vy * factor,
    vz = vz * factor,
  )

/**
 * Divides this velocity by a finite non-zero scalar factor.
 * This operation does not mutate the receiver.
 *
 * @param divisor The divisor.
 *   NaN: throws [IllegalArgumentException]
 *   Infinity: throws [IllegalArgumentException]
 * @return A new velocity divided by [divisor].
 * @throws IllegalArgumentException if [divisor] is not finite or is zero.
 * @throws ArithmeticException if the calculation overflows.
 */
operator fun Velocity3.div(divisor: Double): ImmutableVelocity3 =
  Velocity3(
    vx = vx / divisor,
    vy = vy / divisor,
    vz = vz / divisor,
  )

/**
 * Converts this velocity into a displacement over [duration].
 * This operation does not mutate the receiver.
 *
 * @param duration The time interval.
 * @return A displacement equal to this velocity integrated over [duration].
 * @throws IllegalArgumentException if [duration] is not finite.
 * @throws ArithmeticException if the calculation overflows.
 */
operator fun Velocity3.times(duration: Duration): ImmutableLength3 {
  require(duration.isFinite()) { "Duration must be finite: $duration" }
  val seconds = duration.inWholeNanoseconds.toDouble() / 1_000_000_000.0
  require(seconds.isFinite()) { "Duration conversion must be finite: $duration" }
  return Length3(
    dx = vx * seconds,
    dy = vy * seconds,
    dz = vz * seconds,
  )
}

// endregion

// region implementations

private val VELOCITY3_ZERO: ImmutableVelocity3 = ImmutableVelocity3Impl(Length.ZERO, Length.ZERO, Length.ZERO)

private data class ImmutableVelocity3Impl(
  override var vx: Length,
  override var vy: Length,
  override var vz: Length,
) : ImmutableVelocity3 {
  override fun toString(): String = "Velocity3(vx=$vx/s, vy=$vy/s, vz=$vz/s)"

  override fun equals(other: Any?): Boolean =
    when {
      this === other -> true
      other !is Velocity3 -> false
      else -> vx == other.vx && vy == other.vy && vz == other.vz
    }

  override fun hashCode(): Int = componentsHash(vx, vy, vz)
}

private value class Velocity3MutableWrapper(
  private val impl: ImmutableVelocity3Impl,
) : MutableVelocity3 {
  override var vx: Length
    get() = impl.vx
    set(value) {
      impl.vx = value
    }

  override var vy: Length
    get() = impl.vy
    set(value) {
      impl.vy = value
    }

  override var vz: Length
    get() = impl.vz
    set(value) {
      impl.vz = value
    }

  override val vxFlow: StateFlow<Length>
    get() = throw UnsupportedOperationException()
  override val vyFlow: StateFlow<Length>
    get() = throw UnsupportedOperationException()
  override val vzFlow: StateFlow<Length>
    get() = throw UnsupportedOperationException()

  override fun observe(): ObserveTicket = throw UnsupportedOperationException()
}

private class MutableVelocity3Impl(
  vx: Length,
  vy: Length,
  vz: Length,
) : MutableVelocity3 {
  private var generation: Int = 0
  private val lock = ReentrantLock()
  private val _vxFlow: MutableStateFlow<Length> = MutableStateFlow(vx)
  private val _vyFlow: MutableStateFlow<Length> = MutableStateFlow(vy)
  private val _vzFlow: MutableStateFlow<Length> = MutableStateFlow(vz)

  override var vx: Length
    get() = lock.withLock { _vxFlow.value }
    set(value) {
      lock.withLock {
        generation++
        _vxFlow.value = value
      }
    }

  override var vy: Length
    get() = lock.withLock { _vyFlow.value }
    set(value) {
      lock.withLock {
        generation++
        _vyFlow.value = value
      }
    }

  override var vz: Length
    get() = lock.withLock { _vzFlow.value }
    set(value) {
      lock.withLock {
        generation++
        _vzFlow.value = value
      }
    }

  override val vxFlow: StateFlow<Length> get() = _vxFlow.asStateFlow()
  override val vyFlow: StateFlow<Length> get() = _vyFlow.asStateFlow()
  override val vzFlow: StateFlow<Length> get() = _vzFlow.asStateFlow()

  override fun mutate(action: MutableVelocity3.() -> Unit) {
    lock.withLock { action(this) }
  }

  override fun toString(): String = "Velocity3(vx=$vx/s, vy=$vy/s, vz=$vz/s)"

  override fun equals(other: Any?): Boolean =
    when {
      this === other -> true
      other !is Velocity3 -> false
      else -> vx == other.vx && vy == other.vy && vz == other.vz
    }

  override fun hashCode(): Int = componentsHash(vx, vy, vz)

  override fun observe(): ObserveTicket = Ticket(this)

  private class Ticket(
    original: MutableVelocity3Impl,
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

private fun componentsHash(
  vx: Length,
  vy: Length,
  vz: Length,
): Int {
  var result = vx.hashCode()
  result = 31 * result + vy.hashCode()
  result = 31 * result + vz.hashCode()
  return result
}

// endregion
