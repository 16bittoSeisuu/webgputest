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
 * Represents a 3D transform composed of translation, rotation, and scale.
 * The application order is `scale -> rotation -> translation`.
 *
 * @author Int16
 */
sealed interface Transform {
  /**
   * The translation component applied last.
   */
  val translation: Length3

  /**
   * The rotation component applied after scaling.
   */
  val rotation: Quaternion

  /**
   * The per-axis scale applied first.
   */
  val scale: Scale3

  /**
   * Component operator for destructuring declarations.
   */
  operator fun component1() = translation

  /**
   * Component operator for destructuring declarations.
   */
  operator fun component2() = rotation

  /**
   * Component operator for destructuring declarations.
   */
  operator fun component3() = scale

  override fun toString(): String

  override fun equals(other: Any?): Boolean

  override fun hashCode(): Int

  companion object
}

/**
 * Immutable transform with value semantics. External implementations are forbidden so cached instances such as
 * [Transform.Companion.identity] remain safe to reuse.
 */
sealed interface ImmutableTransform : Transform

/**
 * Mutable transform. The default implementation guards access with a lock, publishes changes through [StateFlow], and
 * exposes dirtiness via [Observable.observe].
 */
interface MutableTransform :
  Transform,
  Observable {
  override var translation: Length3
  override var rotation: Quaternion
  override var scale: Scale3

  /**
   * A [StateFlow] emitting the current translation.
   */
  val translationFlow: StateFlow<Length3>

  /**
   * A [StateFlow] emitting the current rotation.
   */
  val rotationFlow: StateFlow<Quaternion>

  /**
   * A [StateFlow] emitting the current scale.
   */
  val scaleFlow: StateFlow<Scale3>

  /**
   * Runs [action] while holding the internal lock when available so compound operations stay consistent.
   */
  fun mutate(action: MutableTransform.() -> Unit) = action(this)

  override fun observe(): ObserveTicket

  companion object
}

// endregion

// region constants

/**
 * The identity transform (no translation, identity rotation, unit scale).
 */
val Transform.Companion.identity: ImmutableTransform get() = TRANSFORM_IDENTITY

// endregion

// region factory functions

/**
 * Creates a transform. It can be treated as [MutableTransform] only inside [mutator]; the returned instance is always
 * immutable and reuses cached singletons for common values such as identity. Components are copied to immutable
 * instances to prevent external mutation.
 *
 * @param translation The translation component applied last.
 * @param rotation The rotation component applied after scaling.
 * @param scale The per-axis scale applied first.
 * @param mutator Optional scope to tweak values before freezing; avoids an extra allocation compared to creating a
 *   mutable copy later.
 * @return The frozen [ImmutableTransform].
 */
@Suppress("FunctionName")
fun Transform(
  translation: Length3 = Length3.zero,
  rotation: Quaternion = Quaternion.identity,
  scale: Scale3 = Scale3.identity,
  mutator: (MutableTransform.() -> Unit)? = null,
): ImmutableTransform {
  val impl = createTransformImmutable(translation, rotation, scale)
  if (mutator != null) {
    val mutableWrapper = TransformMutableWrapper(impl)
    mutator(mutableWrapper)
  }
  return impl
}

/**
 * Creates an [ImmutableTransform] by copying an existing one.
 * If the source is already immutable and [mutator] is null, the same instance is returned.
 *
 * @param copyFrom The source transform.
 * @param mutator A scope to adjust values right after copying. If null, nothing is changed.
 * @return The frozen [ImmutableTransform].
 */
inline fun Transform.Companion.copyOf(
  copyFrom: Transform,
  noinline mutator: (MutableTransform.() -> Unit)? = null,
): ImmutableTransform =
  if (copyFrom is ImmutableTransform && mutator == null) {
    copyFrom
  } else {
    Transform(
      translation = copyFrom.translation,
      rotation = copyFrom.rotation,
      scale = copyFrom.scale,
      mutator = mutator,
    )
  }

/**
 * Creates a [MutableTransform].
 *
 * @param translation The translation component applied last.
 * @param rotation The rotation component applied after scaling.
 * @param scale The per-axis scale applied first.
 * @return The created [MutableTransform].
 */
fun MutableTransform(
  translation: Length3 = Length3.zero,
  rotation: Quaternion = Quaternion.identity,
  scale: Scale3 = Scale3.identity,
): MutableTransform = MutableTransformImpl(translation, rotation, scale)

/**
 * Creates a [MutableTransform] by copying an existing one.
 *
 * @param copyFrom The source transform.
 * @return The created [MutableTransform].
 */
fun MutableTransform.Companion.copyOf(copyFrom: Transform): MutableTransform =
  MutableTransform(copyFrom.translation, copyFrom.rotation, copyFrom.scale)

// endregion

// region operations

/**
 * Returns `true` if this transform has zero translation, identity rotation, and unit scale.
 */
inline val Transform.isIdentity: Boolean
  get() = translation.isZero && rotation == Quaternion.identity && scale.isIdentity

/**
 * Applies this transform to a [Point3] using `scale -> rotation -> translation`.
 */
fun Transform.transform(point: Point3): ImmutablePoint3 {
  val scaled = scale.scale(point.toVectorFromOrigin())
  val rotated = rotation.rotate(scaled)
  return Point3.zero + (translation + rotated)
}

/**
 * Applies this transform to a [Length3] direction using `scale -> rotation`.
 */
fun Transform.transform(distance: Length3): ImmutableLength3 {
  val scaled = scale.scale(distance)
  return rotation.rotate(scaled)
}

/**
 * Applies the inverse of this transform to a [Point3]. Requires all scale components to be non-zero.
 */
fun Transform.inverseTransform(point: Point3): ImmutablePoint3 {
  val invScale = inverseScale()
  val invRotation = rotation.normalized().conjugated()
  val untranslated = point - translation
  val unrotated = invRotation.rotate(untranslated.toVectorFromOrigin())
  val rescaled = invScale.scale(unrotated)
  return Point3.zero + rescaled
}

/**
 * Applies the inverse of this transform to a [Length3] direction. Requires all scale components to be non-zero.
 */
fun Transform.inverseTransform(distance: Length3): ImmutableLength3 {
  val invScale = inverseScale()
  val invRotation = rotation.normalized().conjugated()
  val rescaled = invScale.scale(distance)
  return invRotation.rotate(rescaled)
}

/**
 * Returns a new transform equivalent to applying this transform, then [next], using TRS composition
 * (`scale -> rotation -> translation`). Shear is discarded, so results are exact when scales are uniform or
 * rotation aligns with the scale axes.
 */
fun Transform.then(next: Transform): ImmutableTransform {
  val combinedTranslation = next.translation + next.rotation.rotate(next.scale.scale(translation))
  val combinedRotation = next.rotation * rotation
  val combinedScale = next.scale * scale
  return Transform(
    translation = combinedTranslation,
    rotation = combinedRotation,
    scale = combinedScale,
  )
}

/**
 * Returns the composition [before] -> this.
 */
fun Transform.prepend(before: Transform): ImmutableTransform = before.then(this)

/**
 * Returns a copy of this [Point3] transformed by [transform].
 */
inline fun Point3.transformedBy(transform: Transform): ImmutablePoint3 = transform.transform(this)

/**
 * Returns a copy of this [Length3] transformed by [transform].
 */
inline fun Length3.transformedBy(transform: Transform): ImmutableLength3 = transform.transform(this)

/**
 * Returns a copy of this [Point3] transformed by the inverse of [transform].
 */
inline fun Point3.inverseTransformedBy(transform: Transform): ImmutablePoint3 = transform.inverseTransform(this)

/**
 * Returns a copy of this [Length3] transformed by the inverse of [transform].
 */
inline fun Length3.inverseTransformedBy(transform: Transform): ImmutableLength3 = transform.inverseTransform(this)

inline fun MutableTransform.mutateTranslation(action: MutableLength3.() -> Unit) {
  when (val t = translation) {
    is MutableLength3 -> {
      t.action()
    }

    else -> {
      val mutable = MutableLength3.copyOf(t)
      mutable.action()
      translation = mutable
    }
  }
}

inline fun MutableTransform.mutateRotation(action: MutableQuaternion.() -> Unit) {
  when (val r = rotation) {
    is MutableQuaternion -> {
      r.action()
    }

    else -> {
      val mutable = MutableQuaternion.copyOf(r)
      mutable.action()
      rotation = mutable
    }
  }
}

inline fun MutableTransform.mutateScale(action: MutableScale3.() -> Unit) {
  when (val s = scale) {
    is MutableScale3 -> {
      s.action()
    }

    else -> {
      val mutable = MutableScale3.copyOf(s)
      mutable.action()
      scale = mutable
    }
  }
}

// endregion

// region implementations

private val TRANSFORM_IDENTITY: ImmutableTransformImpl =
  ImmutableTransformImpl(Length3.zero, Quaternion.identity, Scale3.identity)

private data class ImmutableTransformImpl(
  override var translation: Length3,
  override var rotation: Quaternion,
  override var scale: Scale3,
) : ImmutableTransform {
  override fun toString(): String = "Transform(translation=$translation, rotation=$rotation, scale=$scale)"

  override fun equals(other: Any?): Boolean =
    when {
      this === other -> true
      other !is Transform -> false
      else -> componentsEqual(this, other)
    }

  override fun hashCode(): Int = componentsHash(translation, rotation, scale)
}

private value class TransformMutableWrapper(
  private val impl: ImmutableTransformImpl,
) : MutableTransform {
  override var translation: Length3
    get() = impl.translation
    set(value) {
      impl.translation = Length3.copyOf(value)
    }
  override var rotation: Quaternion
    get() = impl.rotation
    set(value) {
      impl.rotation = Quaternion.copyOf(value)
    }
  override var scale: Scale3
    get() = impl.scale
    set(value) {
      impl.scale = Scale3.copyOf(value)
    }

  override val translationFlow: StateFlow<Length3>
    get() = throw UnsupportedOperationException()
  override val rotationFlow: StateFlow<Quaternion>
    get() = throw UnsupportedOperationException()
  override val scaleFlow: StateFlow<Scale3>
    get() = throw UnsupportedOperationException()

  override fun observe(): ObserveTicket = throw UnsupportedOperationException()

  override fun toString(): String = "Transform(translation=$translation, rotation=$rotation, scale=$scale)"
}

private class MutableTransformImpl(
  translation: Length3,
  rotation: Quaternion,
  scale: Scale3,
) : MutableTransform {
  private var generation: Int = 0
  private val lock = ReentrantLock()
  private val _translationFlow: MutableStateFlow<Length3> = MutableStateFlow(Length3.copyOf(translation))
  private val _rotationFlow: MutableStateFlow<Quaternion> = MutableStateFlow(Quaternion.copyOf(rotation))
  private val _scaleFlow: MutableStateFlow<Scale3> = MutableStateFlow(Scale3.copyOf(scale))

  override var translation: Length3
    get() = lock.withLock { _translationFlow.value }
    set(value) {
      val frozen = Length3.copyOf(value)
      lock.withLock {
        generation++
        _translationFlow.value = frozen
      }
    }
  override var rotation: Quaternion
    get() = lock.withLock { _rotationFlow.value }
    set(value) {
      val frozen = Quaternion.copyOf(value)
      lock.withLock {
        generation++
        _rotationFlow.value = frozen
      }
    }
  override var scale: Scale3
    get() = lock.withLock { _scaleFlow.value }
    set(value) {
      val frozen = Scale3.copyOf(value)
      lock.withLock {
        generation++
        _scaleFlow.value = frozen
      }
    }

  override val translationFlow: StateFlow<Length3> get() = _translationFlow.asStateFlow()
  override val rotationFlow: StateFlow<Quaternion> get() = _rotationFlow.asStateFlow()
  override val scaleFlow: StateFlow<Scale3> get() = _scaleFlow.asStateFlow()

  override fun mutate(action: MutableTransform.() -> Unit) {
    lock.withLock { action(this) }
  }

  override fun observe(): ObserveTicket = Ticket(this)

  override fun toString(): String = "Transform(translation=$translation, rotation=$rotation, scale=$scale)"

  override fun equals(other: Any?): Boolean =
    when {
      this === other -> true
      other !is Transform -> false
      else -> componentsEqual(this, other)
    }

  override fun hashCode(): Int = componentsHash(translation, rotation, scale)

  private class Ticket(
    original: MutableTransformImpl,
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
  a: Transform,
  b: Transform,
): Boolean =
  a.translation == b.translation &&
    a.rotation == b.rotation &&
    a.scale == b.scale

private fun componentsHash(
  translation: Length3,
  rotation: Quaternion,
  scale: Scale3,
): Int {
  var result = 17
  result = 31 * result + translation.hashCode()
  result = 31 * result + rotation.hashCode()
  result = 31 * result + scale.hashCode()
  return result
}

private fun createTransformImmutable(
  translation: Length3,
  rotation: Quaternion,
  scale: Scale3,
): ImmutableTransformImpl {
  val frozenTranslation = Length3.copyOf(translation)
  val frozenRotation = Quaternion.copyOf(rotation)
  val frozenScale = Scale3.copyOf(scale)
  return when {
    frozenTranslation.isZero && frozenRotation == Quaternion.identity && frozenScale.isIdentity -> TRANSFORM_IDENTITY
    else -> ImmutableTransformImpl(frozenTranslation, frozenRotation, frozenScale)
  }
}

private fun Transform.inverseScale(): ImmutableScale3 {
  val sx = scale.sx
  val sy = scale.sy
  val sz = scale.sz
  require(sx != 0.0 && sy != 0.0 && sz != 0.0) { "Cannot invert a transform with zero scale." }
  return Scale3(
    sx = 1.0 / sx,
    sy = 1.0 / sy,
    sz = 1.0 / sz,
  )
}

private fun Point3.toVectorFromOrigin(): Length3 = Length3(dx = x, dy = y, dz = z)

// endregion
