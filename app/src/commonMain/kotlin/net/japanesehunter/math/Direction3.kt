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
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.sqrt

// region interfaces

/**
 * Represents a normalized, dimensionless direction in 3D space.
 * Components are always finite and renormalized after any mutation so `ux^2 + uy^2 + uz^2 == 1`.
 *
 * @author Int16
 */
sealed interface Direction3 {
  /**
   * The x component of the direction (dimensionless).
   */
  val ux: Double

  /**
   * The y component of the direction (dimensionless).
   */
  val uy: Double

  /**
   * The z component of the direction (dimensionless).
   */
  val uz: Double

  /**
   * Component operator for destructuring declarations.
   */
  operator fun component1() = ux

  /**
   * Component operator for destructuring declarations.
   */
  operator fun component2() = uy

  /**
   * Component operator for destructuring declarations.
   */
  operator fun component3() = uz

  override fun toString(): String

  override fun equals(other: Any?): Boolean

  override fun hashCode(): Int

  companion object
}

/**
 * Immutable direction with value semantics. External implementations are forbidden so the module can safely cache
 * shared instances such as [Direction3.Companion.forward].
 */
sealed interface ImmutableDirection3 : Direction3

/**
 * Mutable direction. The default implementation guards access with a lock, publishes changes through [StateFlow], and
 * exposes dirtiness via [Observable.observe].
 */
interface MutableDirection3 :
  Direction3,
  Observable {
  override var ux: Double
  override var uy: Double
  override var uz: Double

  /**
   * A [StateFlow] emitting the current x component.
   */
  val uxFlow: StateFlow<Double>

  /**
   * A [StateFlow] emitting the current y component.
   */
  val uyFlow: StateFlow<Double>

  /**
   * A [StateFlow] emitting the current z component.
   */
  val uzFlow: StateFlow<Double>

  /**
   * Runs [action] while holding the internal lock when available so compound operations stay consistent.
   */
  fun mutate(action: MutableDirection3.() -> Unit) = action(this)

  override fun observe(): ObserveTicket

  companion object
}

// endregion

// region constants

/**
 * A unit vector pointing along negative z (-Z).
 */
val Direction3.Companion.forward: ImmutableDirection3 get() = DIRECTION3_FORWARD

/**
 * A unit vector pointing along positive z (+Z).
 */
val Direction3.Companion.back: ImmutableDirection3 get() = DIRECTION3_BACK

/**
 * A unit vector pointing up (+Y).
 */
val Direction3.Companion.up: ImmutableDirection3 get() = DIRECTION3_UP

/**
 * A unit vector pointing down (-Y).
 */
val Direction3.Companion.down: ImmutableDirection3 get() = DIRECTION3_DOWN

/**
 * A unit vector pointing right (+X).
 */
val Direction3.Companion.right: ImmutableDirection3 get() = DIRECTION3_RIGHT

/**
 * A unit vector pointing left (-X).
 */
val Direction3.Companion.left: ImmutableDirection3 get() = DIRECTION3_LEFT

/**
 * A unit vector pointing north (-Z).
 */
val Direction3.Companion.north: ImmutableDirection3 get() = DIRECTION3_FORWARD

/**
 * A unit vector pointing south (+Z).
 */
val Direction3.Companion.south: ImmutableDirection3 get() = DIRECTION3_BACK

/**
 * A unit vector pointing east (+X).
 */
val Direction3.Companion.east: ImmutableDirection3 get() = DIRECTION3_RIGHT

/**
 * A unit vector pointing west (-X).
 */
val Direction3.Companion.west: ImmutableDirection3 get() = DIRECTION3_LEFT

// endregion

// region factory functions

/**
 * Creates a [Direction3]. It can be treated as [MutableDirection3] only inside [mutator]; the returned instance is
 * always immutable and reuses cached singletons for common axes.
 *
 * @param ux The x component (dimensionless).
 * @param uy The y component (dimensionless).
 * @param uz The z component (dimensionless).
 * @param mutator Optional scope to tweak values before freezing; avoids an extra allocation compared to creating a
 *   mutable copy later.
 * @return The frozen [ImmutableDirection3].
 */
@Suppress("FunctionName")
fun Direction3(
  ux: Double = 0.0,
  uy: Double = 0.0,
  uz: Double = -1.0,
  mutator: (MutableDirection3.() -> Unit)? = null,
): ImmutableDirection3 {
  val impl = createDirectionImmutable(ux, uy, uz)
  if (mutator != null) {
    val mutableWrapper = Direction3MutableWrapper(impl)
    mutator(mutableWrapper)
  }
  return impl
}

/**
 * Creates an [ImmutableDirection3] by copying an existing one.
 * If the source is already immutable and [mutator] is null, the same instance is returned.
 *
 * @param copyFrom The source direction.
 * @param mutator A scope to adjust values right after copying. If null, nothing is changed.
 * @return The frozen [ImmutableDirection3].
 */
inline fun Direction3.Companion.copyOf(
  copyFrom: Direction3,
  noinline mutator: (MutableDirection3.() -> Unit)? = null,
): ImmutableDirection3 =
  if (copyFrom is ImmutableDirection3 && mutator == null) {
    copyFrom
  } else {
    Direction3(
      ux = copyFrom.ux,
      uy = copyFrom.uy,
      uz = copyFrom.uz,
      mutator = mutator,
    )
  }

/**
 * Creates a [MutableDirection3].
 *
 * @param ux The x component (dimensionless).
 * @param uy The y component (dimensionless).
 * @param uz The z component (dimensionless).
 * @return The created [MutableDirection3].
 */
fun MutableDirection3(
  ux: Double = 0.0,
  uy: Double = 0.0,
  uz: Double = -1.0,
): MutableDirection3 = MutableDirection3Impl(ux, uy, uz)

/**
 * Creates a [MutableDirection3] by copying an existing one.
 *
 * @param copyFrom The source direction.
 * @return The created [MutableDirection3].
 */
fun MutableDirection3.Companion.copyOf(copyFrom: Direction3): MutableDirection3 = MutableDirection3(copyFrom.ux, copyFrom.uy, copyFrom.uz)

/**
 * Converts a [Length3] displacement into a normalized [Direction3].
 *
 * @param displacement The displacement to convert. Must be non-zero.
 * @return The normalized [Direction3].
 * @throws IllegalArgumentException If the displacement is zero-length or non-finite.
 */
fun Direction3.Companion.from(displacement: Length3): ImmutableDirection3 {
  val unit = selectLengthUnitForDirection(displacement)
  val ux = displacement.dx.toDouble(unit)
  val uy = displacement.dy.toDouble(unit)
  val uz = displacement.dz.toDouble(unit)
  val normalized = normalizeDirectionComponents(ux, uy, uz, "Direction3.from")
  return Direction3(
    ux = normalized.ux,
    uy = normalized.uy,
    uz = normalized.uz,
  )
}

/**
 * Converts this displacement into a normalized [Direction3].
 *
 * @return The normalized [Direction3].
 * @throws IllegalArgumentException If the displacement is zero-length or non-finite.
 */
inline fun Length3.toDirection(): ImmutableDirection3 = Direction3.from(this)

/**
 * Maps this direction to the nearest [Direction16] based on yaw around the Y-axis.
 */
fun Direction3.toDirection16(): Direction16 {
  require(ux != 0.0 || uz != 0.0) {
    "Cannot convert Direction3 with ux=0 and uz=0 to Direction16 (undefined yaw)."
  }
  val yawRadians = atan2(ux, -uz)
  val yawAngle = yawRadians.radians
  return Direction16.from(yawAngle)
}

// endregion

// region arithmetic

/**
 * Returns the dot product of this direction with [other].
 */
inline infix fun Direction3.dot(other: Direction3): Double = ux * other.ux + uy * other.uy + uz * other.uz

/**
 * Returns the cross product of this direction with [other], normalized to unit length.
 *
 * @param other The direction to cross with.
 * @return A new [ImmutableDirection3] perpendicular to both directions.
 * @throws IllegalArgumentException If the directions are collinear or non-finite.
 */
inline infix fun Direction3.cross(other: Direction3): ImmutableDirection3 =
  Direction3(
    ux = uy * other.uz - uz * other.uy,
    uy = uz * other.ux - ux * other.uz,
    uz = ux * other.uy - uy * other.ux,
  )

/**
 * Returns the left-handed cross product of this direction with [other], normalized to unit length.
 * Equivalent to the negated right-handed cross when interpreted in a right-handed system.
 *
 * @param other The direction to cross with.
 * @return A new [ImmutableDirection3] perpendicular to both directions using left-handed orientation.
 * @throws IllegalArgumentException If the directions are collinear or non-finite.
 */
inline infix fun Direction3.crossLH(other: Direction3): ImmutableDirection3 =
  Direction3(
    ux = uz * other.uy - uy * other.uz,
    uy = ux * other.uz - uz * other.ux,
    uz = uy * other.ux - ux * other.uy,
  )

/**
 * Negates all components of this mutable direction.
 */
inline fun MutableDirection3.negate() = map({ "Negation" }) { _, value -> -value }

/**
 * Returns a new [ImmutableDirection3] with all components negated.
 */
inline operator fun Direction3.unaryMinus(): ImmutableDirection3 = Direction3.copyOf(this) { negate() }

/**
 * Maps each component using [action] and writes the normalized result back.
 *
 * @param actionName Optional name for diagnostics.
 * @param action Called in order for indices 0:ux, 1:uy, 2:uz.
 */
inline fun MutableDirection3.map(
  noinline actionName: (() -> String)? = null,
  crossinline action: (index: Int, value: Double) -> Double,
) {
  val actionNameValue = actionName?.invoke()
  mutate {
    val newUx = action(0, ux)
    val newUy = action(1, uy)
    val newUz = action(2, uz)
    setNormalized(newUx, newUy, newUz, actionNameValue)
  }
}

@PublishedApi
internal fun MutableDirection3.setNormalized(
  ux: Double,
  uy: Double,
  uz: Double,
  actionName: String? = null,
) {
  val normalized = normalizeDirectionComponents(ux, uy, uz, actionName)
  when (this) {
    is MutableDirection3Impl -> {
      updateNormalized(normalized.ux, normalized.uy, normalized.uz, actionName)
    }

    else -> {
      this.ux = normalized.ux
      this.uy = normalized.uy
      this.uz = normalized.uz
    }
  }
}

// endregion

// region implementations

private val DIRECTION3_FORWARD: ImmutableDirection3Impl = ImmutableDirection3Impl(0.0, 0.0, -1.0)
private val DIRECTION3_BACK: ImmutableDirection3Impl = ImmutableDirection3Impl(0.0, 0.0, 1.0)
private val DIRECTION3_UP: ImmutableDirection3Impl = ImmutableDirection3Impl(0.0, 1.0, 0.0)
private val DIRECTION3_DOWN: ImmutableDirection3Impl = ImmutableDirection3Impl(0.0, -1.0, 0.0)
private val DIRECTION3_RIGHT: ImmutableDirection3Impl = ImmutableDirection3Impl(1.0, 0.0, 0.0)
private val DIRECTION3_LEFT: ImmutableDirection3Impl = ImmutableDirection3Impl(-1.0, 0.0, 0.0)

private data class ImmutableDirection3Impl(
  override var ux: Double,
  override var uy: Double,
  override var uz: Double,
) : ImmutableDirection3 {
  override fun toString(): String = "Direction3(ux=$ux, uy=$uy, uz=$uz)"

  override fun equals(other: Any?): Boolean =
    when {
      this === other -> true
      other !is Direction3 -> false
      else -> componentsEqual(this, other)
    }

  override fun hashCode(): Int = componentsHash(ux, uy, uz)

  fun setNormalized(
    ux: Double,
    uy: Double,
    uz: Double,
    actionName: String? = null,
  ) {
    val normalized = normalizeDirectionComponents(ux, uy, uz, actionName)
    this.ux = normalized.ux
    this.uy = normalized.uy
    this.uz = normalized.uz
  }
}

private value class Direction3MutableWrapper(
  private val impl: ImmutableDirection3Impl,
) : MutableDirection3 {
  override var ux: Double
    get() = impl.ux
    set(value) {
      impl.setNormalized(value, impl.uy, impl.uz, "ux assignment")
    }
  override var uy: Double
    get() = impl.uy
    set(value) {
      impl.setNormalized(impl.ux, value, impl.uz, "uy assignment")
    }
  override var uz: Double
    get() = impl.uz
    set(value) {
      impl.setNormalized(impl.ux, impl.uy, value, "uz assignment")
    }

  override val uxFlow: StateFlow<Double>
    get() = throw UnsupportedOperationException()
  override val uyFlow: StateFlow<Double>
    get() = throw UnsupportedOperationException()
  override val uzFlow: StateFlow<Double>
    get() = throw UnsupportedOperationException()

  override fun observe(): ObserveTicket = throw UnsupportedOperationException()
}

private class MutableDirection3Impl(
  ux: Double,
  uy: Double,
  uz: Double,
) : MutableDirection3 {
  private var generation: Int = 0
  private val lock = ReentrantLock()
  private val _uxFlow: MutableStateFlow<Double>
  private val _uyFlow: MutableStateFlow<Double>
  private val _uzFlow: MutableStateFlow<Double>

  init {
    val normalized = normalizeDirectionComponents(ux, uy, uz, "MutableDirection3 init")
    _uxFlow = MutableStateFlow(normalized.ux)
    _uyFlow = MutableStateFlow(normalized.uy)
    _uzFlow = MutableStateFlow(normalized.uz)
  }

  override var ux: Double
    get() = lock.withLock { _uxFlow.value }
    set(value) {
      lock.withLock {
        updateNormalized(value, _uyFlow.value, _uzFlow.value, "ux assignment")
      }
    }
  override var uy: Double
    get() = lock.withLock { _uyFlow.value }
    set(value) {
      lock.withLock {
        updateNormalized(_uxFlow.value, value, _uzFlow.value, "uy assignment")
      }
    }
  override var uz: Double
    get() = lock.withLock { _uzFlow.value }
    set(value) {
      lock.withLock {
        updateNormalized(_uxFlow.value, _uyFlow.value, value, "uz assignment")
      }
    }

  override val uxFlow: StateFlow<Double> get() = _uxFlow.asStateFlow()
  override val uyFlow: StateFlow<Double> get() = _uyFlow.asStateFlow()
  override val uzFlow: StateFlow<Double> get() = _uzFlow.asStateFlow()

  override fun mutate(action: MutableDirection3.() -> Unit) {
    lock.withLock { action(this) }
  }

  override fun toString(): String = "Direction3(ux=$ux, uy=$uy, uz=$uz)"

  override fun equals(other: Any?): Boolean =
    when {
      this === other -> true
      other !is Direction3 -> false
      else -> componentsEqual(this, other)
    }

  override fun hashCode(): Int = componentsHash(ux, uy, uz)

  override fun observe(): ObserveTicket = Ticket(this)

  fun updateNormalized(
    ux: Double,
    uy: Double,
    uz: Double,
    actionName: String? = null,
  ) {
    val normalized = normalizeDirectionComponents(ux, uy, uz, actionName)
    generation++
    _uxFlow.value = normalized.ux
    _uyFlow.value = normalized.uy
    _uzFlow.value = normalized.uz
  }

  private class Ticket(
    original: MutableDirection3Impl,
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
  a: Direction3,
  b: Direction3,
): Boolean =
  a.ux == b.ux &&
    a.uy == b.uy &&
    a.uz == b.uz

private fun componentsHash(
  ux: Double,
  uy: Double,
  uz: Double,
): Int {
  var result = 17
  result = 31 * result + ux.hashCode()
  result = 31 * result + uy.hashCode()
  result = 31 * result + uz.hashCode()
  return result
}

private fun createDirectionImmutable(
  ux: Double,
  uy: Double,
  uz: Double,
): ImmutableDirection3Impl {
  val normalized = normalizeDirectionComponents(ux, uy, uz, "Direction3 creation")
  return when {
    normalized.ux == 0.0 && normalized.uy == 0.0 && normalized.uz == -1.0 -> DIRECTION3_FORWARD
    normalized.ux == 0.0 && normalized.uy == 0.0 && normalized.uz == 1.0 -> DIRECTION3_BACK
    normalized.ux == 0.0 && normalized.uy == 1.0 && normalized.uz == 0.0 -> DIRECTION3_UP
    normalized.ux == 0.0 && normalized.uy == -1.0 && normalized.uz == 0.0 -> DIRECTION3_DOWN
    normalized.ux == 1.0 && normalized.uy == 0.0 && normalized.uz == 0.0 -> DIRECTION3_RIGHT
    normalized.ux == -1.0 && normalized.uy == 0.0 && normalized.uz == 0.0 -> DIRECTION3_LEFT
    else -> ImmutableDirection3Impl(normalized.ux, normalized.uy, normalized.uz)
  }
}

@PublishedApi
internal fun ensureFiniteDirectionComponent(
  value: Double,
  name: String,
  actionName: String? = null,
): Double {
  require(value.isFinite()) {
    if (actionName != null) {
      "Direction3 component $name (during $actionName) must be finite: $value"
    } else {
      "Direction3 component $name must be finite: $value"
    }
  }
  return value
}

private fun normalizeDirectionComponents(
  ux: Double,
  uy: Double,
  uz: Double,
  actionName: String? = null,
): DirectionComponents {
  val finiteUx = ensureFiniteDirectionComponent(ux, "ux", actionName)
  val finiteUy = ensureFiniteDirectionComponent(uy, "uy", actionName)
  val finiteUz = ensureFiniteDirectionComponent(uz, "uz", actionName)
  val max = maxOf(abs(finiteUx), abs(finiteUy), abs(finiteUz))
  require(max != 0.0) {
    if (actionName != null) {
      "Cannot normalize a zero-length Direction3 during $actionName."
    } else {
      "Cannot normalize a zero-length Direction3."
    }
  }
  val minThreshold = 1e-154
  val maxThreshold = 1e154
  val magnitude =
    if (minThreshold < max && max < maxThreshold) {
      sqrt(finiteUx * finiteUx + finiteUy * finiteUy + finiteUz * finiteUz)
    } else {
      hypot(hypot(finiteUx, finiteUy), finiteUz)
    }
  val inv = 1.0 / magnitude
  return DirectionComponents(
    ux = finiteUx * inv,
    uy = finiteUy * inv,
    uz = finiteUz * inv,
  )
}

private data class DirectionComponents(
  val ux: Double,
  val uy: Double,
  val uz: Double,
)

private fun selectLengthUnitForDirection(displacement: Length3): LengthUnit {
  val maxNm =
    maxOf(
      abs(displacement.dx.toDouble(LengthUnit.NANOMETER)),
      abs(displacement.dy.toDouble(LengthUnit.NANOMETER)),
      abs(displacement.dz.toDouble(LengthUnit.NANOMETER)),
    )
  return when {
    maxNm >= 1e12 -> LengthUnit.KILOMETER
    maxNm >= 1e9 -> LengthUnit.METER
    maxNm >= 1e7 -> LengthUnit.CENTIMETER
    maxNm >= 1e6 -> LengthUnit.MILLIMETER
    maxNm >= 1e3 -> LengthUnit.MICROMETER
    else -> LengthUnit.NANOMETER
  }
}

// endregion
