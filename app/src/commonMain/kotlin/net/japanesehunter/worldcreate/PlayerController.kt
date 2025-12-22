package net.japanesehunter.worldcreate

import arrow.fx.coroutines.ResourceScope
import io.github.oshai.kotlinlogging.KotlinLogging.logger
import net.japanesehunter.math.Acceleration
import net.japanesehunter.math.Angle
import net.japanesehunter.math.AngleUnit
import net.japanesehunter.math.Direction3
import net.japanesehunter.math.Speed
import net.japanesehunter.math.SpeedUnit
import net.japanesehunter.math.degrees
import net.japanesehunter.math.forward
import net.japanesehunter.math.lookAlong
import net.japanesehunter.math.metersPerSecond
import net.japanesehunter.math.metersPerSecondSquared
import net.japanesehunter.math.rotate
import net.japanesehunter.math.up
import net.japanesehunter.traits.Entity
import net.japanesehunter.traits.get
import net.japanesehunter.traits.has
import net.japanesehunter.worldcreate.input.InputContext
import net.japanesehunter.worldcreate.input.KeyDown
import net.japanesehunter.worldcreate.input.KeyUp
import net.japanesehunter.worldcreate.input.MouseDown
import net.japanesehunter.worldcreate.input.MouseMove
import net.japanesehunter.worldcreate.input.PointerLock
import net.japanesehunter.worldcreate.trait.Grounded
import net.japanesehunter.worldcreate.trait.Rotation
import net.japanesehunter.worldcreate.trait.Velocity
import net.japanesehunter.worldcreate.world.EventSubscription
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.time.Duration

/**
 * Installs a first-person controller that maps mouse and keyboard input to entity velocity and
 * rotation.
 *
 * The controller uses the pointer lock for capturing the pointer and the input context for
 * event delivery. The returned controller stays active until closed through the resource scope
 * or manually.
 *
 * The entity must have [Velocity] and [Rotation] traits attached.
 *
 * @param entity the entity whose velocity and rotation will be controlled.
 * @param settings controller input bindings and motion parameters.
 * @return the installed controller instance.
 */
context(resource: ResourceScope, pointerLock: PointerLock, input: InputContext)
fun playerController(
  entity: Entity,
  settings: PlayerController.Settings = PlayerController.Settings(),
): PlayerController =
  resource.install(
    PlayerController(
      pointerLock = pointerLock,
      input = input,
      entity = entity,
      settings = settings,
    ),
  )

/**
 * Provides pointer-locked first-person control for an entity using keyboard and mouse input.
 *
 * The controller subscribes to input events through the injected [InputContext] and monitors
 * pointer lock state through the injected [PointerLock]. It keeps yaw and pitch within the
 * configured limits, applying rotation to the entity and velocity updates on update.
 *
 * The controlled entity must have [Velocity] and [Rotation] traits. The [Grounded] trait is
 * checked to determine movement parameters and jump availability.
 *
 * Implementations are not thread-safe and must be accessed from a single thread.
 *
 * @param pointerLock the pointer lock provider for capturing and releasing the pointer.
 * @param input the input context providing keyboard and mouse events.
 * @param entity target entity whose rotation and velocity are controlled.
 * @param settings mutable controller bindings and motion parameters.
 */
class PlayerController internal constructor(
  private val pointerLock: PointerLock,
  private val input: InputContext,
  private val entity: Entity,
  private val settings: Settings = Settings(),
) : AutoCloseable {
  /**
   * Holds mutable input bindings and movement parameters for the player controller.
   *
   * Key codes follow the `KeyboardEvent.code` values.
   * Speeds are expressed in meters per second, and the pitch limit must remain below a right angle
   * to avoid gimbal lock.
   * Settings are not thread-safe and are intended for single-threaded use.
   *
   * @param forwardKey key code used for forward movement.
   * @param backwardKey key code used for backward movement.
   * @param leftKey key code used for strafing left.
   * @param rightKey key code used for strafing right.
   * @param jumpKey key code used for jumping.
   * @param mouseSensitivityDegPerDot mouse sensitivity in degrees per pointer movement dot.
   *
   *   range: mouseSensitivityDegPerDot >= 0.0
   *
   *   NaN: treated as invalid and rejected
   *
   *   Infinity: treated as invalid and rejected
   * @param horizontalSpeed planar movement speed.
   * @param groundAcceleration acceleration rate when on ground.
   * @param groundDeceleration deceleration rate when on ground with no input.
   * @param airAcceleration acceleration rate when in air.
   * @param airDeceleration deceleration rate when in air with no input.
   * @param jumpSpeed initial vertical velocity when jumping.
   * @param maxPitch maximum absolute pitch angle allowed before clamping.
   *
   *   range: 0 <= maxPitch <= 90 degrees
   */
  data class Settings(
    var forwardKey: String = "KeyW",
    var backwardKey: String = "KeyS",
    var leftKey: String = "KeyA",
    var rightKey: String = "KeyD",
    var jumpKey: String = "Space",
    var mouseSensitivityDegPerDot: Double = 0.15,
    var horizontalSpeed: Speed = 5.metersPerSecond,
    var groundAcceleration: Acceleration = 40.metersPerSecondSquared,
    var groundDeceleration: Acceleration = 40.metersPerSecondSquared,
    var airAcceleration: Acceleration = 10.metersPerSecondSquared,
    var airDeceleration: Acceleration = 2.metersPerSecondSquared,
    var jumpSpeed: Speed = 9.metersPerSecond,
    var maxPitch: Angle = 89.0.degrees,
  ) {
    init {
      require(mouseSensitivityDegPerDot.isFinite() && mouseSensitivityDegPerDot >= 0.0) {
        "mouseSensitivityDegPerDot must be finite and non-negative: $mouseSensitivityDegPerDot"
      }
      val maxPitchRadians = maxPitch.toDouble(AngleUnit.RADIAN)
      require(maxPitchRadians in 0.0..(0.5 * PI)) {
        "maxPitch must be in [0, 90] degrees: $maxPitch"
      }
    }
  }

  private val maxPitchRadians get() = settings.maxPitch.toDouble(AngleUnit.RADIAN)
  private val mouseSensitivityRadiansPerDot
    get() = settings.mouseSensitivityDegPerDot * (PI / 180.0)
  private val activeKeys = mutableSetOf<String>()
  private var yaw: Double
  private var pitch: Double
  private var mouseDeltaX = 0.0
  private var mouseDeltaY = 0.0
  private var closed = false

  private val inputSubscription: EventSubscription
  private val pointerLockSubscription: EventSubscription

  init {
    val rotation =
      entity.get<Rotation>()
        ?: error("Entity must have Rotation trait")
    val initialForward = rotation.value.rotate(Direction3.forward)
    yaw = atan2(initialForward.ux, -initialForward.uz)
    pitch = asin(initialForward.uy).coerceIn(-maxPitchRadians, maxPitchRadians)

    inputSubscription =
      input.events().subscribe { event ->
        when (event) {
          is KeyDown -> handleKeyDown(event.code)
          is KeyUp -> handleKeyUp(event.code)
          is MouseMove -> handleMouseMove(event.deltaX, event.deltaY)
          is MouseDown -> pointerLock.requestPointerLock()
          else -> Unit
        }
      }

    pointerLockSubscription =
      pointerLock.pointerLockEvents().subscribe { event ->
        if (!event.locked) {
          activeKeys.clear()
          clearVelocity()
          logger.debug { "Pointer lock released" }
        } else {
          logger.debug { "Pointer lock acquired" }
        }
      }
  }

  /**
   * Advances controller state by applying accumulated mouse movement and active key input.
   *
   * Returns immediately when pointer lock is not active.
   * Updates the player rotation and velocity based on input.
   *
   * @param dt the time elapsed since the last update.
   *
   *   range: dt > 0
   */
  fun update(dt: Duration) {
    require(dt.isPositive()) { "dt must be positive: $dt" }
    if (!pointerLock.isPointerLocked) return

    if (mouseDeltaX != 0.0 || mouseDeltaY != 0.0) {
      yaw += mouseDeltaX * mouseSensitivityRadiansPerDot
      pitch =
        (pitch - mouseDeltaY * mouseSensitivityRadiansPerDot)
          .coerceIn(-maxPitchRadians, maxPitchRadians)
      mouseDeltaX = 0.0
      mouseDeltaY = 0.0
      applyRotation()
    }

    updatePlayerVelocity(dt)
  }

  override fun close() {
    if (closed) return
    closed = true
    inputSubscription.close()
    pointerLockSubscription.close()
    if (pointerLock.isPointerLocked) {
      pointerLock.exitPointerLock()
    }
  }

  private fun handleKeyDown(code: String) {
    if (code == settings.jumpKey) {
      if (entity.has<Grounded>()) {
        entity.get<Velocity>()?.value?.vy = settings.jumpSpeed
      }
    } else if (code in navKeys()) {
      activeKeys.add(code)
    }
  }

  private fun handleKeyUp(code: String) {
    if (code in navKeys()) {
      activeKeys.remove(code)
    }
  }

  private fun handleMouseMove(
    dx: Double,
    dy: Double,
  ) {
    if (!pointerLock.isPointerLocked) return
    mouseDeltaX += dx
    mouseDeltaY += dy
  }

  private fun applyRotation() {
    val rotation = entity.get<Rotation>() ?: return
    val cosPitch = cos(pitch)
    val forward =
      Direction3(
        ux = sin(yaw) * cosPitch,
        uy = sin(pitch),
        uz = -cos(yaw) * cosPitch,
      )
    rotation.value.lookAlong(forward, Direction3.up)
  }

  private fun updatePlayerVelocity(dt: Duration) {
    val planarForward =
      Direction3(
        ux = sin(yaw),
        uy = 0.0,
        uz = -cos(yaw),
      )
    val planarRight =
      Direction3(
        ux = cos(yaw),
        uy = 0.0,
        uz = sin(yaw),
      )

    var inputX = 0.0
    var inputZ = 0.0

    if (settings.forwardKey in activeKeys) {
      inputX += planarForward.ux
      inputZ += planarForward.uz
    }
    if (settings.backwardKey in activeKeys) {
      inputX -= planarForward.ux
      inputZ -= planarForward.uz
    }
    if (settings.rightKey in activeKeys) {
      inputX += planarRight.ux
      inputZ += planarRight.uz
    }
    if (settings.leftKey in activeKeys) {
      inputX -= planarRight.ux
      inputZ -= planarRight.uz
    }

    val velocity = entity.get<Velocity>() ?: return
    val isGrounded = entity.has<Grounded>()

    val currentVx = velocity.value.vx
    val currentVz = velocity.value.vz

    val accel =
      if (isGrounded) settings.groundAcceleration else settings.airAcceleration
    val decel =
      if (isGrounded) settings.groundDeceleration else settings.airDeceleration
    val maxSpeed = settings.horizontalSpeed

    val inputLenSq = inputX * inputX + inputZ * inputZ
    val hasInput = inputLenSq > 0.0

    val (newVx, newVz) =
      if (hasInput) {
        val invLen = 1.0 / sqrt(inputLenSq)
        val dirX = inputX * invLen
        val dirZ = inputZ * invLen
        val targetVx = maxSpeed * dirX
        val targetVz = maxSpeed * dirZ

        val diffX = targetVx - currentVx
        val diffZ = targetVz - currentVz
        val diffLen = hypot(diffX, diffZ)

        if (diffLen.isPositive) {
          val maxChange = accel * dt
          val change = minOf(diffLen, maxChange)
          val newX = currentVx + change * (diffX / diffLen)
          val newZ = currentVz + change * (diffZ / diffLen)
          newX to newZ
        } else {
          currentVx to currentVz
        }
      } else {
        val currentSpeed = hypot(currentVx, currentVz)
        if (currentSpeed.isPositive) {
          val maxDrop = decel * dt
          val newSpeed = maxOf(Speed.ZERO, currentSpeed - maxDrop)
          val scale = newSpeed / currentSpeed
          (currentVx * scale) to (currentVz * scale)
        } else {
          Speed.ZERO to Speed.ZERO
        }
      }

    velocity.value.vx = newVx
    velocity.value.vz = newVz
  }

  private fun clearVelocity() {
    val velocity = entity.get<Velocity>() ?: return
    velocity.value.vx = Speed.ZERO
    velocity.value.vz = Speed.ZERO
  }

  private fun navKeys(): Set<String> =
    setOf(
      settings.forwardKey,
      settings.backwardKey,
      settings.leftKey,
      settings.rightKey,
    )
}

private val logger = logger("PlayerController")

private fun hypot(
  x: Speed,
  y: Speed,
): Speed =
  hypot(
    x.toDouble(SpeedUnit.METER_PER_SECOND),
    y.toDouble(SpeedUnit.METER_PER_SECOND),
  ).metersPerSecond
