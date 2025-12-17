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
 * Represents an axis-aligned bounding box in 3D space.
 *
 * The box is defined by two points.
 * Implementations guarantee that [min] is component-wise less than or equal to [max].
 */
sealed interface Aabb {
  /**
   * Represents the component-wise minimum corner of the box.
   */
  val min: Point3

  /**
   * Represents the component-wise maximum corner of the box.
   */
  val max: Point3

  /**
   * Component operator for destructuring declarations.
   */
  operator fun component1() = min

  /**
   * Component operator for destructuring declarations.
   */
  operator fun component2() = max

  override fun toString(): String

  override fun equals(other: Any?): Boolean

  override fun hashCode(): Int

  companion object
}

/**
 * Represents an immutable axis-aligned bounding box.
 * External implementations are forbidden to preserve value semantics.
 */
sealed interface ImmutableAabb : Aabb

/**
 * Represents a mutable axis-aligned bounding box.
 *
 * Changes can be observed via [StateFlow] and [Observable.observe].
 */
interface MutableAabb :
  Aabb,
  Observable {
  override var min: Point3
  override var max: Point3

  /**
   * A [StateFlow] emitting the current [min] value.
   */
  val minFlow: StateFlow<Point3>

  /**
   * A [StateFlow] emitting the current [max] value.
   */
  val maxFlow: StateFlow<Point3>

  /**
   * Runs [action] while holding the internal lock when available so compound operations stay consistent.
   */
  fun mutate(action: MutableAabb.() -> Unit) = action(this)

  override fun observe(): ObserveTicket

  companion object
}

// endregion

// region constants

/**
 * The zero-sized AABB at the origin.
 */
val Aabb.Companion.zero: ImmutableAabb get() = AABB_ZERO

// endregion

// region factory functions

/**
 * Creates an [Aabb] from two points.
 *
 * The returned instance is always immutable.
 * It can be treated as [MutableAabb] only inside [mutator].
 * The final box is normalized so that [Aabb.min] is component-wise less than or equal to [Aabb.max].
 *
 * @param min The first corner.
 * @param max The second corner.
 * @param mutator A scope for [MutableAabb] for initialization.
 * @return The frozen, immutable [ImmutableAabb].
 */
@Suppress("FunctionName")
fun Aabb(
  min: Point3 = Point3.zero,
  max: Point3 = Point3.zero,
  mutator: (MutableAabb.() -> Unit)? = null,
): ImmutableAabb {
  if (mutator == null && min.isZero && max.isZero) {
    return Aabb.zero
  }

  val impl =
    ImmutableAabbImpl(
      min = Point3.copyOf(min),
      max = Point3.copyOf(max),
    )

  if (mutator != null) {
    val mutableWrapper = AabbMutableWrapper(impl)
    mutator(mutableWrapper)
  }

  impl.normalizeInPlace()
  if (impl.min.isZero && impl.max.isZero) {
    return Aabb.zero
  }
  return impl
}

/**
 * Creates an [ImmutableAabb] by copying an existing one.
 * If the original is already immutable and [mutator] is null, the same instance is returned.
 */
inline fun Aabb.Companion.copyOf(
  copyFrom: Aabb,
  noinline mutator: (MutableAabb.() -> Unit)? = null,
): ImmutableAabb =
  if (copyFrom is ImmutableAabb && mutator == null) {
    copyFrom
  } else {
    Aabb(
      min = copyFrom.min,
      max = copyFrom.max,
      mutator = mutator,
    )
  }

/**
 * Creates a [MutableAabb] from two points.
 *
 * The created instance is normalized immediately.
 */
fun MutableAabb(
  min: Point3 = Point3.zero,
  max: Point3 = Point3.zero,
): MutableAabb = MutableAabbImpl(min = Point3.copyOf(min), max = Point3.copyOf(max)).also { it.normalizeInPlace() }

/**
 * Creates a [MutableAabb] by copying an existing [Aabb].
 */
fun MutableAabb.Companion.copyOf(copyFrom: Aabb): MutableAabb = MutableAabb(copyFrom.min, copyFrom.max)

// endregion

// region geometry

/**
 * Returns `true` when this box overlaps [other].
 */
fun Aabb.intersects(other: Aabb): Boolean =
  min.x <= other.max.x &&
    max.x >= other.min.x &&
    min.y <= other.max.y &&
    max.y >= other.min.y &&
    min.z <= other.max.z &&
    max.z >= other.min.z

// endregion

// region implementations

private val AABB_ZERO: ImmutableAabb = ImmutableAabbImpl(Point3.zero, Point3.zero)

private class ImmutableAabbImpl(
  min: ImmutablePoint3,
  max: ImmutablePoint3,
) : ImmutableAabb {
  var minPoint: ImmutablePoint3 = min
  var maxPoint: ImmutablePoint3 = max

  override val min: ImmutablePoint3
    get() = minPoint

  override val max: ImmutablePoint3
    get() = maxPoint

  fun normalizeInPlace() {
    val (normalizedMin, normalizedMax) = normalize(minPoint, maxPoint)
    minPoint = normalizedMin
    maxPoint = normalizedMax
  }

  override fun toString(): String = "Aabb(min=$min, max=$max)"

  override fun equals(other: Any?): Boolean =
    when {
      this === other -> true
      other !is Aabb -> false
      else -> componentsEqual(this, other)
    }

  override fun hashCode(): Int = componentsHash(min, max)
}

private value class AabbMutableWrapper(
  private val impl: ImmutableAabbImpl,
) : MutableAabb {
  override var min: Point3
    get() = impl.min
    set(value) {
      val (normalizedMin, normalizedMax) = normalize(value, impl.max)
      impl.minPoint = normalizedMin
      impl.maxPoint = normalizedMax
    }

  override var max: Point3
    get() = impl.max
    set(value) {
      val (normalizedMin, normalizedMax) = normalize(impl.min, value)
      impl.minPoint = normalizedMin
      impl.maxPoint = normalizedMax
    }

  override val minFlow: StateFlow<Point3>
    get() = throw UnsupportedOperationException()

  override val maxFlow: StateFlow<Point3>
    get() = throw UnsupportedOperationException()

  override fun observe(): ObserveTicket = throw UnsupportedOperationException()

  override fun toString(): String = "Aabb(min=$min, max=$max)"
}

private class MutableAabbImpl(
  min: ImmutablePoint3,
  max: ImmutablePoint3,
) : MutableAabb {
  private var generation: Int = 0
  private val lock = ReentrantLock()
  private val _minFlow: MutableStateFlow<Point3> = MutableStateFlow(min)
  private val _maxFlow: MutableStateFlow<Point3> = MutableStateFlow(max)

  override var min: Point3
    get() = lock.withLock { _minFlow.value }
    set(value) {
      lock.withLock {
        generation++
        val currentMax = _maxFlow.value
        val (normalizedMin, normalizedMax) = normalize(value, currentMax)
        _minFlow.value = normalizedMin
        _maxFlow.value = normalizedMax
      }
    }

  override var max: Point3
    get() = lock.withLock { _maxFlow.value }
    set(value) {
      lock.withLock {
        generation++
        val currentMin = _minFlow.value
        val (normalizedMin, normalizedMax) = normalize(currentMin, value)
        _minFlow.value = normalizedMin
        _maxFlow.value = normalizedMax
      }
    }

  override val minFlow: StateFlow<Point3> get() = _minFlow.asStateFlow()
  override val maxFlow: StateFlow<Point3> get() = _maxFlow.asStateFlow()

  override fun mutate(action: MutableAabb.() -> Unit) {
    lock.withLock { action(this) }
  }

  fun normalizeInPlace() {
    lock.withLock {
      generation++
      val (normalizedMin, normalizedMax) = normalize(_minFlow.value, _maxFlow.value)
      _minFlow.value = normalizedMin
      _maxFlow.value = normalizedMax
    }
  }

  override fun observe(): ObserveTicket = Ticket(this)

  override fun toString(): String = "Aabb(min=$min, max=$max)"

  override fun equals(other: Any?): Boolean =
    when {
      this === other -> true
      other !is Aabb -> false
      else -> componentsEqual(this, other)
    }

  override fun hashCode(): Int = componentsHash(min, max)

  private class Ticket(
    original: MutableAabbImpl,
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

private fun normalize(
  min: Point3,
  max: Point3,
): Pair<ImmutablePoint3, ImmutablePoint3> {
  val minX = if (min.x <= max.x) min.x else max.x
  val minY = if (min.y <= max.y) min.y else max.y
  val minZ = if (min.z <= max.z) min.z else max.z

  val maxX = if (min.x >= max.x) min.x else max.x
  val maxY = if (min.y >= max.y) min.y else max.y
  val maxZ = if (min.z >= max.z) min.z else max.z

  val normalizedMin = Point3(x = minX, y = minY, z = minZ)
  val normalizedMax = Point3(x = maxX, y = maxY, z = maxZ)
  return normalizedMin to normalizedMax
}

private fun componentsEqual(
  a: Aabb,
  b: Aabb,
): Boolean =
  a.min == b.min &&
    a.max == b.max

private fun componentsHash(
  min: Point3,
  max: Point3,
): Int {
  var result = 17
  result = 31 * result + min.hashCode()
  result = 31 * result + max.hashCode()
  return result
}

// endregion
