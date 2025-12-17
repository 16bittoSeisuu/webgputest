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
 * Represents an axis-aligned bounding box in three-dimensional space.
 *
 * The box is axis-aligned and is defined by two corners.
 * Implementations guarantee that the stored bounds are normalized so the minimum corner is component-wise less than or
 * equal to the maximum corner.
 *
 * Both boundaries are treated as inclusive.
 *
 * Implementations are required to be safe for concurrent reads.
 */
sealed interface Aabb {
  /**
   * Represents the component-wise minimum corner of the box.
   *
   * The boundary is inclusive.
   */
  val min: Point3

  /**
   * Represents the component-wise maximum corner of the box.
   *
   * The boundary is inclusive.
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
 *
 * Instances are safe to share across threads.
 */
sealed interface ImmutableAabb : Aabb

/**
 * Represents a mutable axis-aligned bounding box.
 *
 * Changes can be observed via [StateFlow] and [Observable.observe].
 *
 * Implementations are required to be thread-safe.
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
 * Represents a zero-sized bounding box at the origin.
 */
val Aabb.Companion.zero: ImmutableAabb get() = AABB_ZERO

// endregion

// region factory functions

/**
 * Creates an axis-aligned bounding box from two corners.
 *
 * The returned instance is always immutable.
 * The produced bounds are normalized so the stored minimum corner is component-wise less than or equal to the stored
 * maximum corner.
 *
 * This function has no side effects.
 *
 * @param min the first corner.
 * @param max the second corner.
 * @param mutator a scope for initializing the bounds.
 * @return the created bounding box.
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
 * Creates an immutable bounding box by copying [copyFrom].
 *
 * When [copyFrom] is already immutable and [mutator] is null, the original instance is reused.
 *
 * This function has no side effects.
 *
 * @param copyFrom the instance to copy from.
 * @param mutator a scope for initializing the bounds.
 * @return the copied bounding box.
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
 * Creates a mutable bounding box from two corners.
 *
 * The created instance is normalized immediately.
 *
 * This function has no side effects.
 *
 * @param min the first corner.
 * @param max the second corner.
 * @return the created bounding box.
 */
fun MutableAabb(
  min: Point3 = Point3.zero,
  max: Point3 = Point3.zero,
): MutableAabb = MutableAabbImpl(min = Point3.copyOf(min), max = Point3.copyOf(max)).also { it.normalizeInPlace() }

/**
 * Creates a mutable bounding box by copying [copyFrom].
 *
 * This function has no side effects.
 *
 * @param copyFrom the instance to copy from.
 * @return the copied bounding box.
 */
fun MutableAabb.Companion.copyOf(copyFrom: Aabb): MutableAabb = MutableAabb(copyFrom.min, copyFrom.max)

// endregion

// region geometry

/**
 * Tests whether this box intersects [other].
 *
 * Touching at a face, an edge, or a single point is treated as an intersection.
 *
 * This function has no side effects.
 *
 * @param other the box to test against.
 * @return true when the boxes intersect.
 */
fun Aabb.intersects(other: Aabb): Boolean =
  min.x <= other.max.x &&
    max.x >= other.min.x &&
    min.y <= other.max.y &&
    max.y >= other.min.y &&
    min.z <= other.max.z &&
    max.z >= other.min.z

/**
 * Computes the component-wise size of this box.
 *
 * This function has no side effects.
 *
 * @return the size as max minus min.
 */
inline val Aabb.size: ImmutableLength3
  get() = max - min

/**
 * Creates a box translated by [distance].
 *
 * This function has no side effects.
 *
 * @param distance the displacement to add to both corners.
 * @return the translated box.
 * @throws ArithmeticException arithmetic overflow in underlying length operations.
 */
inline fun Aabb.translatedBy(distance: Length3): ImmutableAabb =
  Aabb(
    min = min + distance,
    max = max + distance,
  )

/**
 * Translates this mutable box by [distance].
 *
 * This function mutates the receiver.
 *
 * @param distance the displacement to add to both corners.
 * @throws ArithmeticException arithmetic overflow in underlying length operations.
 */
fun MutableAabb.translateBy(distance: Length3) {
  if (this is MutableAabbImpl) {
    translateInPlace(distance)
    return
  }

  mutate {
    val newMin = min + distance
    val newMax = max + distance
    min = newMin
    max = newMax
  }
}

/**
 * Creates a box expanded by [padding] in every direction.
 *
 * The padding must be non-negative.
 *
 * This function has no side effects.
 *
 * @param padding the amount of padding added to each side.
 * @return the expanded box.
 * @throws ArithmeticException arithmetic overflow in underlying length operations.
 * @throws IllegalArgumentException when padding is negative.
 */
inline fun Aabb.expandedBy(padding: Length): ImmutableAabb =
  expandedBy(
    Length3(
      dx = padding,
      dy = padding,
      dz = padding,
    ),
  )

/**
 * Creates a box expanded by [padding] in each axis direction.
 *
 * The padding must be non-negative.
 *
 * This function has no side effects.
 *
 * @param padding the per-axis padding added to each side.
 * @return the expanded box.
 * @throws ArithmeticException arithmetic overflow in underlying length operations.
 * @throws IllegalArgumentException when any component of padding is negative.
 */
inline fun Aabb.expandedBy(padding: Length3): ImmutableAabb {
  require(!padding.dx.isNegative && !padding.dy.isNegative && !padding.dz.isNegative) {
    "Padding must be non-negative: $padding"
  }
  return Aabb(
    min = min - padding,
    max = max + padding,
  )
}

/**
 * Creates a box that contains both this box and this box translated by [distance].
 *
 * This function has no side effects.
 *
 * @param distance the displacement used to compute the end position.
 * @return the smallest box that contains both the start and end boxes.
 * @throws ArithmeticException arithmetic overflow in underlying length operations.
 */
fun Aabb.sweptBy(distance: Length3): ImmutableAabb {
  val end = translatedBy(distance)

  val minX = if (min.x <= end.min.x) min.x else end.min.x
  val minY = if (min.y <= end.min.y) min.y else end.min.y
  val minZ = if (min.z <= end.min.z) min.z else end.min.z

  val maxX = if (max.x >= end.max.x) max.x else end.max.x
  val maxY = if (max.y >= end.max.y) max.y else end.max.y
  val maxZ = if (max.z >= end.max.z) max.z else end.max.z

  return Aabb(
    min = Point3(x = minX, y = minY, z = minZ),
    max = Point3(x = maxX, y = maxY, z = maxZ),
  )
}

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

  fun translateInPlace(distance: Length3) {
    lock.withLock {
      generation++
      val currentMin = _minFlow.value
      val currentMax = _maxFlow.value
      val (normalizedMin, normalizedMax) = normalize(currentMin + distance, currentMax + distance)
      _minFlow.value = normalizedMin
      _maxFlow.value = normalizedMax
    }
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
