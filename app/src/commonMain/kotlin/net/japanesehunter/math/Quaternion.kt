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
import kotlin.math.sqrt

// region interfaces

/**
 * Represents a quaternion in `(x, y, z, w)` order where `(x, y, z)` is the vector part and [w] is the scalar part.
 * Components are always kept finite and are left unnormalized unless explicitly requested, so callers decide when to
 * renormalize for their use case (e.g., long multiplication chains or rotations).
 *
 * @author Int16
 */
sealed interface Quaternion {
  /**
   * The i component.
   */
  val x: Double

  /**
   * The j component.
   */
  val y: Double

  /**
   * The k component.
   */
  val z: Double

  /**
   * The scalar component.
   */
  val w: Double

  operator fun component1() = x

  operator fun component2() = y

  operator fun component3() = z

  operator fun component4() = w

  override fun toString(): String

  override fun equals(other: Any?): Boolean

  override fun hashCode(): Int

  companion object
}

/**
 * Immutable quaternion with value semantics. External implementations are forbidden so the module can safely cache
 * shared instances such as [Quaternion.Companion.identity].
 */
sealed interface ImmutableQuaternion : Quaternion

/**
 * Mutable quaternion. The default implementation protects reads/writes with a lock, emits every component change via
 * [StateFlow], and exposes coarse-grained dirtiness through [Observable.observe].
 */
interface MutableQuaternion :
  Quaternion,
  Observable {
  override var x: Double
  override var y: Double
  override var z: Double
  override var w: Double

  /**
   * A [StateFlow] emitting the current x component.
   */
  val xFlow: StateFlow<Double>

  /**
   * A [StateFlow] emitting the current y component.
   */
  val yFlow: StateFlow<Double>

  /**
   * A [StateFlow] emitting the current z component.
   */
  val zFlow: StateFlow<Double>

  /**
   * A [StateFlow] emitting the current w component.
   */
  val wFlow: StateFlow<Double>

  /**
   * Runs [action] while holding the internal lock when available so compound operations stay consistent.
   */
  fun mutate(action: MutableQuaternion.() -> Unit) = action(this)

  override fun observe(): ObserveTicket

  companion object
}

// endregion

// region constants

/**
 * The zero quaternion (0, 0, 0, 0).
 */
val Quaternion.Companion.zero: ImmutableQuaternion get() = QUATERNION_ZERO

/**
 * The identity quaternion (0, 0, 0, 1), representing no rotation.
 */
val Quaternion.Companion.identity: ImmutableQuaternion get() = QUATERNION_IDENTITY

// endregion

// region factory functions

/**
 * Creates a quaternion. It can be treated as [MutableQuaternion] only inside [mutator]; the returned instance is always
 * immutable and reuses cached singletons for common values such as zero/identity. All components are validated to be
 * finite before construction.
 *
 * @param x The i component.
 * @param y The j component.
 * @param z The k component.
 * @param w The scalar component.
 * @param mutator Optional scope to tweak values before freezing; avoids an extra allocation compared to creating a
 *   mutable copy later.
 * @return The frozen [ImmutableQuaternion].
 */
@Suppress("FunctionName")
fun Quaternion(
  x: Double = 0.0,
  y: Double = 0.0,
  z: Double = 0.0,
  w: Double = 1.0,
  mutator: (MutableQuaternion.() -> Unit)? = null,
): ImmutableQuaternion {
  val impl = createQuaternionImmutable(x, y, z, w)
  if (mutator != null) {
    val mutableWrapper = QuaternionMutableWrapper(impl)
    mutator(mutableWrapper)
  }
  return impl
}

/**
 * Creates an [ImmutableQuaternion] by copying an existing one.
 * If the source is already immutable and [mutator] is null, the same instance is returned.
 *
 * @param copyFrom The source quaternion.
 * @param mutator A scope to adjust values right after copying. If null, nothing is changed.
 * @return The frozen [ImmutableQuaternion].
 */
inline fun Quaternion.Companion.copyOf(
  copyFrom: Quaternion,
  noinline mutator: (MutableQuaternion.() -> Unit)? = null,
): ImmutableQuaternion =
  if (copyFrom is ImmutableQuaternion && mutator == null) {
    copyFrom
  } else {
    Quaternion(
      x = copyFrom.x,
      y = copyFrom.y,
      z = copyFrom.z,
      w = copyFrom.w,
      mutator = mutator,
    )
  }

/**
 * Creates a [MutableQuaternion].
 *
 * @param x The i component.
 * @param y The j component.
 * @param z The k component.
 * @param w The scalar component.
 * @return The created [MutableQuaternion].
 */
fun MutableQuaternion(
  x: Double = 0.0,
  y: Double = 0.0,
  z: Double = 0.0,
  w: Double = 1.0,
): MutableQuaternion = MutableQuaternionImpl(x, y, z, w)

/**
 * Creates a [MutableQuaternion] by copying an existing one.
 *
 * @param copyFrom The source quaternion.
 * @return The created [MutableQuaternion].
 */
fun MutableQuaternion.Companion.copyOf(copyFrom: Quaternion): MutableQuaternion =
  MutableQuaternion(copyFrom.x, copyFrom.y, copyFrom.z, copyFrom.w)

// endregion

// region arithmetic

/**
 * Returns `true` only when all components are exactly `0.0`. No tolerance is applied, making this safe for fast identity
 * checks against [Quaternion.Companion.zero] but unsuitable as a "near zero" predicate.
 */
inline val Quaternion.isZero: Boolean
  get() = x == 0.0 && y == 0.0 && z == 0.0 && w == 0.0

/**
 * Magnitude of the quaternion. Scales by the largest component to reduce overflow/underflow while keeping the result
 * mathematically equivalent to `sqrt(x^2 + y^2 + z^2 + w^2)`.
 */
inline val Quaternion.magnitude: Double
  get() {
    val max = maxOf(abs(x), abs(y), abs(z), abs(w))
    if (max == 0.0) return 0.0
    val sx = x / max
    val sy = y / max
    val sz = z / max
    val sw = w / max
    return max * sqrt(sx * sx + sy * sy + sz * sz + sw * sw)
  }

/**
 * Normalizes this quaternion in place. Throws if the magnitude is zero or non-finite; callers should guard against
 * degenerate inputs when coming from untrusted sources.
 */
inline fun MutableQuaternion.normalize() {
  mutate {
    val mag = magnitude
    require(mag != 0.0 && mag.isFinite()) { "Cannot normalize a zero-length or non-finite quaternion." }
    val inv = 1.0 / mag
    val newX = ensureFiniteComponent(x * inv, "x", "Normalization")
    val newY = ensureFiniteComponent(y * inv, "y", "Normalization")
    val newZ = ensureFiniteComponent(z * inv, "z", "Normalization")
    val newW = ensureFiniteComponent(w * inv, "w", "Normalization")
    x = newX
    y = newY
    z = newZ
    w = newW
  }
}

/**
 * Returns a normalized copy. The original is not modified and remains unnormalized for cases where preserving magnitude
 * is important.
 */
inline fun Quaternion.normalized(): ImmutableQuaternion =
  Quaternion.copyOf(this) {
    normalize()
  }

/**
 * Converts this quaternion to its conjugate in place. For unit quaternions this is equivalent to an inverse, but no
 * normalization is applied automatically.
 */
inline fun MutableQuaternion.conjugate() = map("Conjugation") { index, value -> if (index < 3) -value else value }

/**
 * Returns a conjugated copy.
 */
inline fun Quaternion.conjugated(): ImmutableQuaternion =
  Quaternion.copyOf(this) {
    conjugate()
  }

/**
 * Negates all components in place.
 */
inline fun MutableQuaternion.negate() = map("Negation") { _, value -> -value }

/**
 * Returns a copy with all components negated.
 */
inline operator fun Quaternion.unaryMinus(): ImmutableQuaternion =
  Quaternion.copyOf(this) {
    negate()
  }

/**
 * Adds another quaternion component-wise in place. No normalization is performed.
 */
inline operator fun MutableQuaternion.plusAssign(other: Quaternion) =
  map("Addition of $other") { index, value ->
    when (index) {
      0 -> value + other.x
      1 -> value + other.y
      2 -> value + other.z
      else -> value + other.w
    }
  }

/**
 * Returns the sum as a new [ImmutableQuaternion].
 */
inline operator fun Quaternion.plus(other: Quaternion): ImmutableQuaternion =
  Quaternion.copyOf(this) {
    this += other
  }

/**
 * Subtracts another quaternion component-wise in place. No normalization is performed.
 */
inline operator fun MutableQuaternion.minusAssign(other: Quaternion) =
  map("Subtraction of $other") { index, value ->
    when (index) {
      0 -> value - other.x
      1 -> value - other.y
      2 -> value - other.z
      else -> value - other.w
    }
  }

/**
 * Returns the difference as a new [ImmutableQuaternion].
 */
inline operator fun Quaternion.minus(other: Quaternion): ImmutableQuaternion =
  Quaternion.copyOf(this) {
    this -= other
  }

/**
 * Scales this quaternion by a finite scalar in place. Rejects non-finite scalars to prevent NaN propagation.
 */
inline operator fun MutableQuaternion.timesAssign(scalar: Double) {
  require(scalar.isFinite()) { "Cannot scale a quaternion by a non-finite value: $scalar" }
  map("Multiplication by $scalar") { _, value -> value * scalar }
}

/**
 * Returns a new quaternion scaled by a finite scalar.
 */
inline operator fun Quaternion.times(scalar: Double): ImmutableQuaternion =
  Quaternion.copyOf(this) {
    this *= scalar
  }

/**
 * Divides this quaternion by a finite, non-zero scalar in place.
 */
inline operator fun MutableQuaternion.divAssign(scalar: Double) {
  require(scalar.isFinite() && scalar != 0.0) { "Cannot divide a quaternion by $scalar." }
  map("Division by $scalar") { _, value -> value / scalar }
}

/**
 * Returns a new quaternion divided by a finite, non-zero scalar.
 */
inline operator fun Quaternion.div(scalar: Double): ImmutableQuaternion =
  Quaternion.copyOf(this) {
    this /= scalar
  }

/**
 * Multiplies by [other] on the right (Hamilton product), mutating this quaternion. Order matters: `this *= other`
 * differs from `other * this` because quaternion multiplication is not commutative.
 */
inline operator fun MutableQuaternion.timesAssign(other: Quaternion) {
  mutate {
    val newX = w * other.x + x * other.w + y * other.z - z * other.y
    val newY = w * other.y - x * other.z + y * other.w + z * other.x
    val newZ = w * other.z + x * other.y - y * other.x + z * other.w
    val newW = w * other.w - x * other.x - y * other.y - z * other.z
    x = ensureFiniteComponent(newX, "x")
    y = ensureFiniteComponent(newY, "y")
    z = ensureFiniteComponent(newZ, "z")
    w = ensureFiniteComponent(newW, "w")
  }
}

/**
 * Returns the quaternion product as a new [ImmutableQuaternion].
 */
inline operator fun Quaternion.times(other: Quaternion): ImmutableQuaternion =
  Quaternion.copyOf(this) {
    this *= other
  }

/**
 * Rotates a [Length3] vector by this quaternion. The quaternion is normalized
 * internally, so passing a zero quaternion will throw. Because
 * normalization occurs first, the rotation direction depends solely
 * on orientation and not on input magnitude. The length unit used for
 * conversion is chosen automatically based on the largest component to reduce
 * overflow/underflow risk.
 *
 * @param vector The vector to rotate.
 * @return The rotated [ImmutableLength3].
 */
fun Quaternion.rotate(vector: Length3): ImmutableLength3 {
  val (qx, qy, qz, qw) = normalized()
  val unit = selectLengthUnitForRotation(vector)
  val vx = vector.dx.toDouble(unit)
  val vy = vector.dy.toDouble(unit)
  val vz = vector.dz.toDouble(unit)

  val uvx = qy * vz - qz * vy
  val uvy = qz * vx - qx * vz
  val uvz = qx * vy - qy * vx

  val uuvx = qy * uvz - qz * uvy
  val uuvy = qz * uvx - qx * uvz
  val uuvz = qx * uvy - qy * uvx

  val rx = vx + (uvx * qw + uuvx) * 2.0
  val ry = vy + (uvy * qw + uuvy) * 2.0
  val rz = vz + (uvz * qw + uuvz) * 2.0

  return Length3(
    dx = Length.from(rx, unit),
    dy = Length.from(ry, unit),
    dz = Length.from(rz, unit),
  )
}

/**
 * Rotates an [Area3] vector by this quaternion. The quaternion is normalized internally, so passing a zero quaternion
 * will throw. Because normalization occurs first, the rotation direction depends solely on orientation and not on input
 * magnitude. The area unit used for conversion is chosen automatically based on the largest component to reduce
 * overflow/underflow risk.
 *
 * @param area The area vector to rotate.
 * @return The rotated [ImmutableArea3].
 */
fun Quaternion.rotate(area: Area3): ImmutableArea3 {
  val (qx, qy, qz, qw) = normalized()
  val unit = selectAreaUnitForRotation(area)
  val ax = area.ax.toDouble(unit)
  val ay = area.ay.toDouble(unit)
  val az = area.az.toDouble(unit)

  val uvx = qy * az - qz * ay
  val uvy = qz * ax - qx * az
  val uvz = qx * ay - qy * ax

  val uuvx = qy * uvz - qz * uvy
  val uuvy = qz * uvx - qx * uvz
  val uuvz = qx * uvy - qy * uvx

  val rx = ax + (uvx * qw + uuvx) * 2.0
  val ry = ay + (uvy * qw + uuvy) * 2.0
  val rz = az + (uvz * qw + uuvz) * 2.0

  return Area3(
    ax = Area.from(rx, unit),
    ay = Area.from(ry, unit),
    az = Area.from(rz, unit),
  )
}

/**
 * Returns a copy of this [Length3] rotated by [quaternion].
 */
inline fun Length3.rotatedBy(quaternion: Quaternion): ImmutableLength3 = quaternion.rotate(this)

/**
 * Returns a copy of this [Area3] rotated by [quaternion].
 */
inline fun Area3.rotatedBy(quaternion: Quaternion): ImmutableArea3 = quaternion.rotate(this)

/**
 * Orients this quaternion so its forward axis (-Z) points along [dir] while keeping [up] as the up reference.
 *
 * @param dir The direction to look toward. Must be normalized and non-zero.
 * @param up The world up reference used to resolve roll. Must not be collinear with [dir].
 * @throws IllegalArgumentException If [dir] is zero-length or collinear with [up].
 */
fun MutableQuaternion.lookAlong(
  dir: Direction3,
  up: Direction3 = Direction3.up,
) {
  val right = dir cross up
  val orthoUp = right cross dir

  val components =
    quaternionFromRotationMatrix(
      r00 = right.ux,
      r01 = orthoUp.ux,
      r02 = -dir.ux,
      r10 = right.uy,
      r11 = orthoUp.uy,
      r12 = -dir.uy,
      r20 = right.uz,
      r21 = orthoUp.uz,
      r22 = -dir.uz,
    )

  mutate {
    x = components.x
    y = components.y
    z = components.z
    w = components.w
  }
  normalize()
}

/**
 * Builds an immutable quaternion that looks along [dir] with [up] as the up reference.
 *
 * @param dir The direction to look toward. Must be normalized and non-zero.
 * @param up The world up reference used to resolve roll. Must not be collinear with [dir].
 * @throws IllegalArgumentException If [dir] is zero-length or collinear with [up].
 */
fun Quaternion.Companion.lookingAlong(
  dir: Direction3,
  up: Direction3 = Direction3.up,
): ImmutableQuaternion = Quaternion(mutator = { lookAlong(dir, up) })

/**
 * Maps each component using [action] and writes the result back.
 *
 * @param actionName Optional name for diagnostics.
 * @param action Called in order for indices 0:x, 1:y, 2:z, 3:w.
 *
 * Each output component is validated with [ensureFiniteComponent] before assignment; throwing early prevents partially
 * updated quaternions from leaking NaN or infinite values.
 */
inline fun MutableQuaternion.map(
  actionName: String? = null,
  crossinline action: (index: Int, value: Double) -> Double,
) {
  mutate {
    val newX = ensureFiniteComponent(action(0, x), "x", actionName)
    val newY = ensureFiniteComponent(action(1, y), "y", actionName)
    val newZ = ensureFiniteComponent(action(2, z), "z", actionName)
    val newW = ensureFiniteComponent(action(3, w), "w", actionName)
    x = newX
    y = newY
    z = newZ
    w = newW
  }
}

// endregion

// region implementations

private val QUATERNION_ZERO: ImmutableQuaternionImpl = ImmutableQuaternionImpl(0.0, 0.0, 0.0, 0.0)
private val QUATERNION_IDENTITY: ImmutableQuaternionImpl = ImmutableQuaternionImpl(0.0, 0.0, 0.0, 1.0)

private data class ImmutableQuaternionImpl(
  override var x: Double,
  override var y: Double,
  override var z: Double,
  override var w: Double,
) : ImmutableQuaternion {
  init {
    ensureFiniteComponents(x, y, z, w)
  }

  override fun toString(): String = "Quaternion(x=$x, y=$y, z=$z, w=$w)"

  override fun equals(other: Any?): Boolean =
    when {
      this === other -> true
      other !is Quaternion -> false
      else -> componentsEqual(this, other)
    }

  override fun hashCode(): Int = componentsHash(x, y, z, w)
}

private value class QuaternionMutableWrapper(
  private val impl: ImmutableQuaternionImpl,
) : MutableQuaternion {
  override var x: Double
    get() = impl.x
    set(value) {
      impl.x = ensureFiniteComponent(value, "x")
    }
  override var y: Double
    get() = impl.y
    set(value) {
      impl.y = ensureFiniteComponent(value, "y")
    }
  override var z: Double
    get() = impl.z
    set(value) {
      impl.z = ensureFiniteComponent(value, "z")
    }
  override var w: Double
    get() = impl.w
    set(value) {
      impl.w = ensureFiniteComponent(value, "w")
    }

  override val xFlow: StateFlow<Double>
    get() = throw UnsupportedOperationException()
  override val yFlow: StateFlow<Double>
    get() = throw UnsupportedOperationException()
  override val zFlow: StateFlow<Double>
    get() = throw UnsupportedOperationException()
  override val wFlow: StateFlow<Double>
    get() = throw UnsupportedOperationException()

  override fun observe(): ObserveTicket = throw UnsupportedOperationException()

  override fun toString(): String = "Quaternion(x=$x, y=$y, z=$z, w=$w)"
}

private class MutableQuaternionImpl(
  x: Double,
  y: Double,
  z: Double,
  w: Double,
) : MutableQuaternion {
  private var generation: Int = 0
  private val lock = ReentrantLock()
  private val _xFlow: MutableStateFlow<Double> = MutableStateFlow(ensureFiniteComponent(x, "x"))
  private val _yFlow: MutableStateFlow<Double> = MutableStateFlow(ensureFiniteComponent(y, "y"))
  private val _zFlow: MutableStateFlow<Double> = MutableStateFlow(ensureFiniteComponent(z, "z"))
  private val _wFlow: MutableStateFlow<Double> = MutableStateFlow(ensureFiniteComponent(w, "w"))

  override var x: Double
    get() = lock.withLock { _xFlow.value }
    set(value) {
      val finite = ensureFiniteComponent(value, "x")
      lock.withLock {
        generation++
        _xFlow.value = finite
      }
    }
  override var y: Double
    get() = lock.withLock { _yFlow.value }
    set(value) {
      val finite = ensureFiniteComponent(value, "y")
      lock.withLock {
        generation++
        _yFlow.value = finite
      }
    }
  override var z: Double
    get() = lock.withLock { _zFlow.value }
    set(value) {
      val finite = ensureFiniteComponent(value, "z")
      lock.withLock {
        generation++
        _zFlow.value = finite
      }
    }
  override var w: Double
    get() = lock.withLock { _wFlow.value }
    set(value) {
      val finite = ensureFiniteComponent(value, "w")
      lock.withLock {
        generation++
        _wFlow.value = finite
      }
    }

  override val xFlow: StateFlow<Double> get() = _xFlow.asStateFlow()
  override val yFlow: StateFlow<Double> get() = _yFlow.asStateFlow()
  override val zFlow: StateFlow<Double> get() = _zFlow.asStateFlow()
  override val wFlow: StateFlow<Double> get() = _wFlow.asStateFlow()

  override fun mutate(action: MutableQuaternion.() -> Unit) {
    lock.withLock { action(this) }
  }

  override fun observe(): ObserveTicket = Ticket(this)

  override fun toString(): String = "Quaternion(x=$x, y=$y, z=$z, w=$w)"

  override fun equals(other: Any?): Boolean =
    when {
      this === other -> true
      other !is Quaternion -> false
      else -> componentsEqual(this, other)
    }

  override fun hashCode(): Int = componentsHash(x, y, z, w)

  private class Ticket(
    original: MutableQuaternionImpl,
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
  a: Quaternion,
  b: Quaternion,
): Boolean =
  a.x == b.x &&
    a.y == b.y &&
    a.z == b.z &&
    a.w == b.w

private fun componentsHash(
  x: Double,
  y: Double,
  z: Double,
  w: Double,
): Int {
  var result = 17
  result = 31 * result + x.hashCode()
  result = 31 * result + y.hashCode()
  result = 31 * result + z.hashCode()
  result = 31 * result + w.hashCode()
  return result
}

private fun createQuaternionImmutable(
  x: Double,
  y: Double,
  z: Double,
  w: Double,
): ImmutableQuaternionImpl {
  val finiteX = ensureFiniteComponent(x, "x")
  val finiteY = ensureFiniteComponent(y, "y")
  val finiteZ = ensureFiniteComponent(z, "z")
  val finiteW = ensureFiniteComponent(w, "w")
  return when (finiteX) {
    0.0 if finiteY == 0.0 && finiteZ == 0.0 && finiteW == 0.0 -> QUATERNION_ZERO
    0.0 if finiteY == 0.0 && finiteZ == 0.0 && finiteW == 1.0 -> QUATERNION_IDENTITY
    else -> ImmutableQuaternionImpl(finiteX, finiteY, finiteZ, finiteW)
  }
}

@PublishedApi
internal fun ensureFiniteComponent(
  value: Double,
  name: String,
  actionName: String? = null,
): Double {
  require(value.isFinite()) {
    if (actionName != null) {
      "Quaternion component $name (during $actionName) must be finite: $value"
    } else {
      "Quaternion component $name must be finite: $value"
    }
  }
  return value
}

@PublishedApi
internal fun ensureFiniteComponents(
  x: Double,
  y: Double,
  z: Double,
  w: Double,
  actionName: String? = null,
) {
  ensureFiniteComponent(x, "x", actionName)
  ensureFiniteComponent(y, "y", actionName)
  ensureFiniteComponent(z, "z", actionName)
  ensureFiniteComponent(w, "w", actionName)
}

private fun selectLengthUnitForRotation(vector: Length3): LengthUnit {
  val maxNm =
    maxOf(
      abs(vector.dx.toDouble(LengthUnit.NANOMETER)),
      abs(vector.dy.toDouble(LengthUnit.NANOMETER)),
      abs(vector.dz.toDouble(LengthUnit.NANOMETER)),
    )
  return when {
    maxNm >= 1e12 -> LengthUnit.KILOMETER

    // 1e12 nm = 1 km
    maxNm >= 1e9 -> LengthUnit.METER

    // 1e9 nm = 1 m
    maxNm >= 1e7 -> LengthUnit.CENTIMETER

    // 1e7 nm = 1 cm
    maxNm >= 1e6 -> LengthUnit.MILLIMETER

    // 1e6 nm = 1 mm
    maxNm >= 1e3 -> LengthUnit.MICROMETER

    // 1e3 nm = 1 µm
    else -> LengthUnit.NANOMETER
  }
}

private fun selectAreaUnitForRotation(area: Area3): AreaUnit {
  val maxNm2 =
    maxOf(
      abs(area.ax.toDouble(AreaUnit.SQUARE_NANOMETER)),
      abs(area.ay.toDouble(AreaUnit.SQUARE_NANOMETER)),
      abs(area.az.toDouble(AreaUnit.SQUARE_NANOMETER)),
    )
  return when {
    maxNm2 >= 1e24 -> AreaUnit.SQUARE_KILOMETER

    // (1e12 nm)^2
    maxNm2 >= 1e18 -> AreaUnit.SQUARE_METER

    // (1e9 nm)^2
    maxNm2 >= 1e12 -> AreaUnit.SQUARE_MILLIMETER

    // (1e6 nm)^2
    maxNm2 >= 1e6 -> AreaUnit.SQUARE_MICROMETER

    // (1e3 nm)^2
    else -> AreaUnit.SQUARE_NANOMETER
  }
}

private fun quaternionFromRotationMatrix(
  r00: Double,
  r01: Double,
  r02: Double,
  r10: Double,
  r11: Double,
  r12: Double,
  r20: Double,
  r21: Double,
  r22: Double,
): Quaternion {
  val trace = r00 + r11 + r22
  return if (trace > 0.0) {
    val s = sqrt(trace + 1.0) * 2.0
    Quaternion(
      x = (r21 - r12) / s,
      y = (r02 - r20) / s,
      z = (r10 - r01) / s,
      w = 0.25 * s,
    )
  } else if (r00 >= r11 && r00 >= r22) {
    val s = sqrt(1.0 + r00 - r11 - r22) * 2.0
    Quaternion(
      x = 0.25 * s,
      y = (r01 + r10) / s,
      z = (r02 + r20) / s,
      w = (r21 - r12) / s,
    )
  } else if (r11 > r22) {
    val s = sqrt(1.0 + r11 - r00 - r22) * 2.0
    Quaternion(
      x = (r01 + r10) / s,
      y = 0.25 * s,
      z = (r12 + r21) / s,
      w = (r02 - r20) / s,
    )
  } else {
    val s = sqrt(1.0 + r22 - r00 - r11) * 2.0
    Quaternion(
      x = (r02 + r20) / s,
      y = (r12 + r21) / s,
      z = 0.25 * s,
      w = (r10 - r01) / s,
    )
  }
}

// endregion
