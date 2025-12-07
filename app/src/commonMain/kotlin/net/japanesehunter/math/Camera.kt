@file:Suppress("NOTHING_TO_INLINE")

package net.japanesehunter.math

import kotlinx.atomicfu.locks.ReentrantLock
import kotlinx.atomicfu.locks.withLock

// region interfaces

/**
 * Camera base type. Always satisfies `aspect > 0.0` and valid [fov]/[nearFar] constraints.
 *
 * @author Int16
 */
sealed interface Camera {
  val transform: Transform
  val fov: Fov
  val aspect: Double
  val nearFar: NearFar

  override fun toString(): String

  override fun equals(other: Any?): Boolean

  override fun hashCode(): Int

  companion object
}

/**
 * Immutable camera.
 */
sealed interface StaticCamera : Camera

/**
 * Mutable camera. Default implementations are thread-safe via an internal [ReentrantLock]; callers can also rely on
 * [mutate] to perform compound operations atomically.
 */
interface MovableCamera : Camera {
  override var transform: Transform
  override var fov: Fov
  override var aspect: Double
  override var nearFar: NearFar

  /**
   * Runs [action] while holding an internal lock when available so compound operations stay consistent.
   * Implementations without a lock simply execute [action] directly. For built-in cameras this is safe to call from
   * multiple threads.
   */
  fun mutate(action: MovableCamera.() -> Unit) {
    if (this is LockingMovableCamera) {
      lock.withLock { action(this) }
    } else {
      action(this)
    }
  }

  companion object
}

internal interface LockingMovableCamera : MovableCamera {
  val lock: ReentrantLock
}

// endregion

// region utilities

/**
 * X position of the camera in world space.
 */
inline var MovableCamera.x: Length
  get() = transform.translation.dx
  set(value) {
    mutateTransform {
      translation =
        Length3(
          dx = value,
          dy = translation.dy,
          dz = translation.dz,
        )
    }
  }

/**
 * Y position of the camera in world space.
 */
inline var MovableCamera.y: Length
  get() = transform.translation.dy
  set(value) {
    mutateTransform {
      translation =
        Length3(
          dx = translation.dx,
          dy = value,
          dz = translation.dz,
        )
    }
  }

/**
 * Z position of the camera in world space.
 */
inline var MovableCamera.z: Length
  get() = transform.translation.dz
  set(value) {
    mutateTransform {
      translation =
        Length3(
          dx = translation.dx,
          dy = translation.dy,
          dz = value,
        )
    }
  }

/**
 * Translates the camera by [delta].
 */
inline fun MovableCamera.translate(delta: Length3) =
  mutateTransform {
    mutateTranslation {
      this += delta
    }
  }

/**
 * Sets the camera translation to [position].
 * Copies mutable transform components first so the camera does not share them with callers.
 */
inline fun MovableCamera.setPosition(position: Length3) =
  mutateTransform {
    mutateTranslation {
      dx = position.dx
      dy = position.dy
      dz = position.dz
    }
  }

/**
 * Rotates the camera by [deltaRotation] (post-multiplication).
 */
inline fun MovableCamera.rotate(deltaRotation: Quaternion) =
  mutateTransform {
    mutateRotation {
      this *= deltaRotation
    }
  }

/**
 * Sets the camera rotation to [rotation].
 * Copies mutable transform components first so the camera does not share them with callers.
 */
inline fun MovableCamera.setRotation(rotation: Quaternion) =
  mutateTransform {
    mutateRotation {
      w = rotation.w
      x = rotation.x
      y = rotation.y
      z = rotation.z
    }
  }

/**
 * Rotates the camera so it looks at [at] using [up] as the up reference.
 */
inline fun MovableCamera.lookAt(
  at: Point3,
  up: Direction3 = Direction3.up,
) = mutateTransform {
  val position = Point3.zero + translation
  val dir = (at - position).toDirection()
  mutateRotation {
    lookAlong(dir, up)
  }
}

/**
 * Adjusts the camera aspect ratio.
 */
inline fun MovableCamera.setAspect(aspect: Double) =
  mutate {
    require(aspect.isFinite() && aspect > 0.0) { "aspect must be finite and > 0.0, was $aspect" }
    this.aspect = aspect
  }

/**
 * Adjusts the camera FOV.
 */
inline fun MovableCamera.setFov(fov: Fov) =
  mutate {
    this.fov = fov
  }

/**
 * Adjusts the camera near/far range.
 */
inline fun MovableCamera.setNearFar(nearFar: NearFar) =
  mutate {
    this.nearFar = nearFar
  }

// endregion

// region factory functions

/**
 * Creates a [StaticCamera]. Accepts an optional [mutator] to adjust a mutable view before freezing.
 */
@Suppress("FunctionName")
fun Camera(
  transform: Transform = Transform.identity,
  fov: Fov,
  aspect: Double,
  nearFar: NearFar,
  mutator: (MovableCamera.() -> Unit)? = null,
): StaticCamera {
  validateAspect(aspect)
  if (mutator == null) {
    return StaticCameraImpl(transform, fov, aspect, nearFar)
  }
  val impl = StaticCameraImpl(transform, fov, aspect, nearFar)
  val wrapper = MovableCameraWrapper(impl)
  mutator(wrapper)
  validateAspect(wrapper.aspect)
  return impl
}

/**
 * Copies [camera] into a new immutable instance. If [mutator] is null and the source is already [StaticCamera], the same
 * instance is returned.
 */
inline fun Camera.Companion.copyOf(
  camera: Camera,
  noinline mutator: (MovableCamera.() -> Unit)? = null,
): StaticCamera =
  if (camera is StaticCamera && mutator == null) {
    camera
  } else {
    Camera(
      transform = camera.transform,
      fov = camera.fov,
      aspect = camera.aspect,
      nearFar = camera.nearFar,
      mutator = mutator,
    )
  }

/**
 * Creates a [MovableCamera].
 */
fun MovableCamera(
  transform: Transform = Transform.identity,
  fov: Fov,
  aspect: Double,
  nearFar: NearFar,
): MovableCamera = MovableCameraImpl(transform, fov, aspect, nearFar)

/**
 * Creates a [MovableCamera] copied from [camera].
 */
fun MovableCamera.Companion.copyOf(camera: Camera): MovableCamera =
  MovableCamera(
    transform = camera.transform,
    fov = camera.fov,
    aspect = camera.aspect,
    nearFar = camera.nearFar,
  )

// endregion

// region implementations

private data class StaticCameraImpl(
  override var transform: Transform,
  override var fov: Fov,
  override var aspect: Double,
  override var nearFar: NearFar,
) : StaticCamera {
  init {
    validateAspect(aspect)
  }

  override fun toString(): String = "Camera(transform=$transform, fov=$fov, aspect=$aspect, nearFar=$nearFar)"

  override fun equals(other: Any?): Boolean =
    when {
      this === other -> true
      other !is Camera -> false
      else -> componentsEqual(this, other)
    }

  override fun hashCode(): Int = componentsHash(transform, fov, aspect, nearFar)
}

private class MovableCameraImpl(
  transform: Transform,
  fov: Fov,
  aspect: Double,
  nearFar: NearFar,
) : MovableCamera,
  LockingMovableCamera {
  override val lock: ReentrantLock = ReentrantLock()
  private var _transform: Transform = transform
  private var _fov: Fov = fov
  private var _aspect: Double = aspect
  private var _nearFar: NearFar = nearFar

  override var transform: Transform
    get() = lock.withLock { _transform }
    set(value) {
      lock.withLock {
        _transform = value
      }
    }

  override var fov: Fov
    get() = lock.withLock { _fov }
    set(value) {
      lock.withLock {
        _fov = value
      }
    }

  override var aspect: Double
    get() = lock.withLock { _aspect }
    set(value) {
      lock.withLock {
        _aspect = value
      }
    }

  override var nearFar: NearFar
    get() = lock.withLock { _nearFar }
    set(value) {
      lock.withLock {
        _nearFar = value
      }
    }

  init {
    validateAspect(aspect)
  }

  override fun toString(): String = "Camera(transform=$transform, fov=$fov, aspect=$aspect, nearFar=$nearFar)"

  override fun equals(other: Any?): Boolean =
    when {
      this === other -> true
      other !is Camera -> false
      else -> componentsEqual(this, other)
    }

  override fun hashCode(): Int = componentsHash(transform, fov, aspect, nearFar)
}

private class MovableCameraWrapper(
  private val impl: StaticCameraImpl,
) : MovableCamera,
  LockingMovableCamera {
  override val lock: ReentrantLock = ReentrantLock()

  override var transform: Transform
    get() = lock.withLock { impl.transform }
    set(value) {
      lock.withLock { impl.transform = value }
    }

  override var fov: Fov
    get() = lock.withLock { impl.fov }
    set(value) {
      lock.withLock { impl.fov = value }
    }

  override var aspect: Double
    get() = lock.withLock { impl.aspect }
    set(value) {
      lock.withLock { impl.aspect = value }
    }

  override var nearFar: NearFar
    get() = lock.withLock { impl.nearFar }
    set(value) {
      lock.withLock { impl.nearFar = value }
    }

  override fun toString(): String = impl.toString()

  override fun equals(other: Any?): Boolean = impl == other

  override fun hashCode(): Int = impl.hashCode()
}

private fun validateAspect(aspect: Double) {
  require(aspect.isFinite() && aspect > 0.0) { "aspect must be finite and > 0.0, was $aspect" }
}

private fun componentsEqual(
  a: Camera,
  b: Camera,
): Boolean =
  a.transform == b.transform &&
    a.fov == b.fov &&
    a.aspect == b.aspect &&
    a.nearFar == b.nearFar

private fun componentsHash(
  transform: Transform,
  fov: Fov,
  aspect: Double,
  nearFar: NearFar,
): Int {
  var result = 17
  result = 31 * result + transform.hashCode()
  result = 31 * result + fov.hashCode()
  result = 31 * result + aspect.hashCode()
  result = 31 * result + nearFar.hashCode()
  return result
}

@PublishedApi
internal inline fun MovableCamera.mutateTransform(noinline action: MutableTransform.() -> Unit) {
  when (val t = transform) {
    is MutableTransform -> {
      t.mutate {
        action()
      }
    }

    else -> {
      mutate {
        transform = Transform.copyOf(t, mutator = action)
      }
    }
  }
}

// endregion
