@file:OptIn(ExperimentalAtomicApi::class)
@file:Suppress("NOTHING_TO_INLINE")

package net.japanesehunter.math

import kotlinx.atomicfu.locks.ReentrantLock
import kotlinx.atomicfu.locks.withLock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.concurrent.atomics.ExperimentalAtomicApi

// region interfaces

/**
 * Represents per-axis scale multipliers for 3D transforms.
 * Components are stored as dimensionless, finite [Double] values.
 *
 * @author Int16
 */
sealed interface Scale3 {
  /**
   * The scale factor along the x-axis.
   */
  val sx: Double

  /**
   * The scale factor along the y-axis.
   */
  val sy: Double

  /**
   * The scale factor along the z-axis.
   */
  val sz: Double

  /**
   * Component operator for destructuring declarations.
   */
  operator fun component1() = sx

  /**
   * Component operator for destructuring declarations.
   */
  operator fun component2() = sy

  /**
   * Component operator for destructuring declarations.
   */
  operator fun component3() = sz

  override fun toString(): String

  override fun equals(other: Any?): Boolean

  override fun hashCode(): Int

  companion object
}

/**
 * Immutable scale vector. External implementations are forbidden so cached instances like
 * [Scale3.Companion.identity] remain safe to reuse.
 */
sealed interface ImmutableScale3 : Scale3

/**
 * Mutable scale vector. The default implementation guards access with a lock, publishes changes through [StateFlow],
 * and exposes dirtiness via [Observable.observe].
 */
interface MutableScale3 :
  Scale3,
  Observable {
  override var sx: Double
  override var sy: Double
  override var sz: Double

  /**
   * A [StateFlow] emitting the current x-axis scale.
   */
  val sxFlow: StateFlow<Double>

  /**
   * A [StateFlow] emitting the current y-axis scale.
   */
  val syFlow: StateFlow<Double>

  /**
   * A [StateFlow] emitting the current z-axis scale.
   */
  val szFlow: StateFlow<Double>

  override fun observe(): ObserveTicket

  companion object
}

// endregion

// region constants

/**
 * The identity scale (1, 1, 1).
 */
val Scale3.Companion.identity: ImmutableScale3 get() = SCALE3_IDENTITY

/**
 * The zero scale (0, 0, 0).
 */
val Scale3.Companion.zero: ImmutableScale3 get() = SCALE3_ZERO

// endregion

// region factory functions

/**
 * Creates a [Scale3]. It can be treated as [MutableScale3] only inside [mutator]; the returned instance is always
 * immutable and reuses cached singletons for common values such as zero/identity. All components are validated to be
 * finite before construction.
 *
 * @param sx The scale factor along the x-axis.
 * @param sy The scale factor along the y-axis.
 * @param sz The scale factor along the z-axis.
 * @param mutator Optional scope to tweak values before freezing; avoids an extra allocation compared to creating a
 *   mutable copy later.
 * @return The frozen [ImmutableScale3].
 */
@Suppress("FunctionName")
fun Scale3(
  sx: Double = 1.0,
  sy: Double = 1.0,
  sz: Double = 1.0,
  mutator: (MutableScale3.() -> Unit)? = null,
): ImmutableScale3 {
  val impl = createScaleImmutable(sx, sy, sz)
  if (mutator != null) {
    val mutableWrapper = Scale3MutableWrapper(impl)
    mutator(mutableWrapper)
  }
  return impl
}

/**
 * Creates an [ImmutableScale3] by copying an existing one.
 * If the source is already immutable and [mutator] is null, the same instance is returned.
 *
 * @param copyFrom The source scale vector.
 * @param mutator A scope to adjust values right after copying. If null, nothing is changed.
 * @return The frozen [ImmutableScale3].
 */
inline fun Scale3.Companion.copyOf(
  copyFrom: Scale3,
  noinline mutator: (MutableScale3.() -> Unit)? = null,
): ImmutableScale3 =
  if (copyFrom is ImmutableScale3 && mutator == null) {
    copyFrom
  } else {
    Scale3(
      sx = copyFrom.sx,
      sy = copyFrom.sy,
      sz = copyFrom.sz,
      mutator = mutator,
    )
  }

/**
 * Creates a [MutableScale3].
 *
 * @param sx The scale factor along the x-axis.
 * @param sy The scale factor along the y-axis.
 * @param sz The scale factor along the z-axis.
 * @return The created [MutableScale3].
 */
fun MutableScale3(
  sx: Double = 1.0,
  sy: Double = 1.0,
  sz: Double = 1.0,
): MutableScale3 = MutableScale3Impl(sx, sy, sz)

/**
 * Creates a [MutableScale3] by copying an existing one.
 *
 * @param copyFrom The source scale vector.
 * @return The created [MutableScale3].
 */
fun MutableScale3.Companion.copyOf(copyFrom: Scale3): MutableScale3 = MutableScale3(copyFrom.sx, copyFrom.sy, copyFrom.sz)

// endregion

// region arithmetic

/**
 * Returns `true` only when all components are exactly `1.0`.
 */
inline val Scale3.isIdentity: Boolean
  get() = sx == 1.0 && sy == 1.0 && sz == 1.0

/**
 * Returns `true` only when all components are exactly `0.0`.
 */
inline val Scale3.isZero: Boolean
  get() = sx == 0.0 && sy == 0.0 && sz == 0.0

/**
 * Scales all components of this mutable scale by a finite scalar in place.
 */
inline operator fun MutableScale3.timesAssign(scalar: Double) {
  require(scalar.isFinite()) { "Cannot scale Scale3 by a non-finite value: $scalar" }
  map("Multiplication by $scalar") { _, value -> value * scalar }
}

/**
 * Returns a new scale vector with all components multiplied by the given scalar.
 */
inline operator fun Scale3.times(scalar: Double): ImmutableScale3 =
  Scale3.copyOf(this) {
    this *= scalar
  }

/**
 * Divides all components of this mutable scale by a finite, non-zero scalar.
 */
inline operator fun MutableScale3.divAssign(scalar: Double) {
  require(scalar.isFinite() && scalar != 0.0) { "Cannot divide Scale3 by $scalar." }
  map("Division by $scalar") { _, value -> value / scalar }
}

/**
 * Returns a new scale vector with all components divided by the given scalar.
 */
inline operator fun Scale3.div(scalar: Double): ImmutableScale3 =
  Scale3.copyOf(this) {
    this /= scalar
  }

/**
 * Multiplies this mutable scale component-wise by [other].
 */
inline operator fun MutableScale3.timesAssign(other: Scale3) =
  map("Component-wise multiplication by $other") { index, value ->
    when (index) {
      0 -> value * other.sx
      1 -> value * other.sy
      else -> value * other.sz
    }
  }

/**
 * Returns the component-wise product of this scale and [other].
 */
inline operator fun Scale3.times(other: Scale3): ImmutableScale3 =
  Scale3.copyOf(this) {
    this *= other
  }

/**
 * Divides this mutable scale component-wise by [other]. Throws if any divisor is zero or non-finite.
 */
inline operator fun MutableScale3.divAssign(other: Scale3) =
  map("Component-wise division by $other") { index, value ->
    val divisor =
      when (index) {
        0 -> ensureFiniteScaleComponent(other.sx, "sx")
        1 -> ensureFiniteScaleComponent(other.sy, "sy")
        else -> ensureFiniteScaleComponent(other.sz, "sz")
      }
    require(divisor != 0.0) { "Cannot divide Scale3 by zero on component $index." }
    value / divisor
  }

/**
 * Returns the component-wise quotient of this scale and [other]. Throws if any divisor is zero or non-finite.
 */
inline operator fun Scale3.div(other: Scale3): ImmutableScale3 =
  Scale3.copyOf(this) {
    this /= other
  }

/**
 * Applies this scale to a [Length3] distance.
 */
fun Scale3.scale(distance: Length3): ImmutableLength3 =
  Length3(
    dx = distance.dx * sx,
    dy = distance.dy * sy,
    dz = distance.dz * sz,
  )

/**
 * Applies this scale to a [Point3] position relative to the origin.
 */
fun Scale3.scale(point: Point3): ImmutablePoint3 =
  Point3(
    x = point.x * sx,
    y = point.y * sy,
    z = point.z * sz,
  )

/**
 * Returns a copy of this [Length3] scaled by [scale].
 */
inline fun Length3.scaledBy(scale: Scale3): ImmutableLength3 = scale.scale(this)

/**
 * Returns a copy of this [Point3] scaled by [scale].
 */
inline fun Point3.scaledBy(scale: Scale3): ImmutablePoint3 = scale.scale(this)

/**
 * Maps each component using [action] and writes the result back.
 *
 * @param actionName Optional name for diagnostics.
 * @param action Called in order for indices 0:sx, 1:sy, 2:sz.
 *
 * Each output component is validated with [ensureFiniteScaleComponent] before assignment; throwing early prevents
 * partially updated vectors from leaking NaN or infinite values.
 */
inline fun MutableScale3.map(
  actionName: String? = null,
  action: (index: Int, value: Double) -> Double,
) {
  val newSx = ensureFiniteScaleComponent(action(0, sx), "sx", actionName)
  val newSy = ensureFiniteScaleComponent(action(1, sy), "sy", actionName)
  val newSz = ensureFiniteScaleComponent(action(2, sz), "sz", actionName)
  sx = newSx
  sy = newSy
  sz = newSz
}

// endregion

// region implementations

private val SCALE3_IDENTITY: ImmutableScale3Impl = ImmutableScale3Impl(1.0, 1.0, 1.0)
private val SCALE3_ZERO: ImmutableScale3Impl = ImmutableScale3Impl(0.0, 0.0, 0.0)

private data class ImmutableScale3Impl(
  override var sx: Double,
  override var sy: Double,
  override var sz: Double,
) : ImmutableScale3 {
  init {
    ensureFiniteScaleComponents(sx, sy, sz)
  }

  override fun toString(): String = "Scale3(sx=$sx, sy=$sy, sz=$sz)"

  override fun equals(other: Any?): Boolean =
    when {
      this === other -> true
      other !is Scale3 -> false
      else -> componentsEqual(this, other)
    }

  override fun hashCode(): Int = componentsHash(sx, sy, sz)
}

private value class Scale3MutableWrapper(
  private val impl: ImmutableScale3Impl,
) : MutableScale3 {
  override var sx: Double
    get() = impl.sx
    set(value) {
      impl.sx = ensureFiniteScaleComponent(value, "sx")
    }
  override var sy: Double
    get() = impl.sy
    set(value) {
      impl.sy = ensureFiniteScaleComponent(value, "sy")
    }
  override var sz: Double
    get() = impl.sz
    set(value) {
      impl.sz = ensureFiniteScaleComponent(value, "sz")
    }

  override val sxFlow: StateFlow<Double>
    get() = throw UnsupportedOperationException()
  override val syFlow: StateFlow<Double>
    get() = throw UnsupportedOperationException()
  override val szFlow: StateFlow<Double>
    get() = throw UnsupportedOperationException()

  override fun observe(): ObserveTicket = throw UnsupportedOperationException()

  override fun toString(): String = "Scale3(sx=$sx, sy=$sy, sz=$sz)"
}

private class MutableScale3Impl(
  sx: Double,
  sy: Double,
  sz: Double,
) : MutableScale3 {
  private var generation: Int = 0
  private val lock = ReentrantLock()
  private val _sxFlow: MutableStateFlow<Double> = MutableStateFlow(ensureFiniteScaleComponent(sx, "sx"))
  private val _syFlow: MutableStateFlow<Double> = MutableStateFlow(ensureFiniteScaleComponent(sy, "sy"))
  private val _szFlow: MutableStateFlow<Double> = MutableStateFlow(ensureFiniteScaleComponent(sz, "sz"))

  override var sx: Double
    get() = lock.withLock { _sxFlow.value }
    set(value) {
      val finite = ensureFiniteScaleComponent(value, "sx")
      lock.withLock {
        generation++
        _sxFlow.value = finite
      }
    }
  override var sy: Double
    get() = lock.withLock { _syFlow.value }
    set(value) {
      val finite = ensureFiniteScaleComponent(value, "sy")
      lock.withLock {
        generation++
        _syFlow.value = finite
      }
    }
  override var sz: Double
    get() = lock.withLock { _szFlow.value }
    set(value) {
      val finite = ensureFiniteScaleComponent(value, "sz")
      lock.withLock {
        generation++
        _szFlow.value = finite
      }
    }

  override val sxFlow: StateFlow<Double> get() = _sxFlow.asStateFlow()
  override val syFlow: StateFlow<Double> get() = _syFlow.asStateFlow()
  override val szFlow: StateFlow<Double> get() = _szFlow.asStateFlow()

  override fun observe(): ObserveTicket = Ticket(this)

  override fun toString(): String = "Scale3(sx=$sx, sy=$sy, sz=$sz)"

  override fun equals(other: Any?): Boolean =
    when {
      this === other -> true
      other !is Scale3 -> false
      else -> componentsEqual(this, other)
    }

  override fun hashCode(): Int = componentsHash(sx, sy, sz)

  private class Ticket(
    original: MutableScale3Impl,
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
  a: Scale3,
  b: Scale3,
): Boolean =
  a.sx == b.sx &&
    a.sy == b.sy &&
    a.sz == b.sz

private fun componentsHash(
  sx: Double,
  sy: Double,
  sz: Double,
): Int {
  var result = 17
  result = 31 * result + sx.hashCode()
  result = 31 * result + sy.hashCode()
  result = 31 * result + sz.hashCode()
  return result
}

private fun createScaleImmutable(
  sx: Double,
  sy: Double,
  sz: Double,
): ImmutableScale3Impl {
  val finiteSx = ensureFiniteScaleComponent(sx, "sx")
  val finiteSy = ensureFiniteScaleComponent(sy, "sy")
  val finiteSz = ensureFiniteScaleComponent(sz, "sz")
  return when {
    finiteSx == 1.0 && finiteSy == 1.0 && finiteSz == 1.0 -> SCALE3_IDENTITY
    finiteSx == 0.0 && finiteSy == 0.0 && finiteSz == 0.0 -> SCALE3_ZERO
    else -> ImmutableScale3Impl(finiteSx, finiteSy, finiteSz)
  }
}

@PublishedApi
internal fun ensureFiniteScaleComponent(
  value: Double,
  name: String,
  actionName: String? = null,
): Double {
  require(value.isFinite()) {
    if (actionName != null) {
      "Scale3 component $name (during $actionName) must be finite: $value"
    } else {
      "Scale3 component $name must be finite: $value"
    }
  }
  return value
}

@PublishedApi
internal fun ensureFiniteScaleComponents(
  sx: Double,
  sy: Double,
  sz: Double,
  actionName: String? = null,
) {
  ensureFiniteScaleComponent(sx, "sx", actionName)
  ensureFiniteScaleComponent(sy, "sy", actionName)
  ensureFiniteScaleComponent(sz, "sz", actionName)
}

// endregion
