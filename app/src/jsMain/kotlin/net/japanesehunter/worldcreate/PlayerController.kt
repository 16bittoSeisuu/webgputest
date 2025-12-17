package net.japanesehunter.worldcreate

import arrow.fx.coroutines.ResourceScope
import io.github.oshai.kotlinlogging.KotlinLogging.logger
import kotlinx.browser.document
import kotlinx.browser.window
import net.japanesehunter.math.Angle
import net.japanesehunter.math.AngleUnit
import net.japanesehunter.math.Direction3
import net.japanesehunter.math.Length
import net.japanesehunter.math.Length3
import net.japanesehunter.math.LengthUnit
import net.japanesehunter.math.MovableCamera
import net.japanesehunter.math.Quaternion
import net.japanesehunter.math.degrees
import net.japanesehunter.math.forward
import net.japanesehunter.math.lookingAlong
import net.japanesehunter.math.meters
import net.japanesehunter.math.rotate
import net.japanesehunter.math.setPosition
import net.japanesehunter.math.setRotation
import net.japanesehunter.math.up
import net.japanesehunter.webgpu.CanvasContext
import net.japanesehunter.worldcreate.entity.Player
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.events.Event
import org.w3c.dom.events.KeyboardEvent
import org.w3c.dom.events.MouseEvent
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Installs a first-person controller that maps mouse and keyboard input to player velocity and camera rotation.
 *
 * Registers DOM listeners on the canvas and window, requests pointer lock on click when available, and must be called on the browser main thread.
 * The returned controller stays active until closed through the resource scope or manually.
 *
 * @param player the player whose velocity will be controlled.
 * @param settings controller input bindings and motion parameters.
 * @return the installed controller instance.
 */
context(resource: ResourceScope, canvas: CanvasContext)
fun MovableCamera.controller(
  player: Player,
  settings: PlayerController.Settings = PlayerController.Settings(),
): PlayerController =
  resource.install(
    PlayerController(
      canvas = canvas.canvas,
      camera = this,
      player = player,
      settings = settings,
    ),
  )

/**
 * Provides pointer-locked first-person control for a player using keyboard and mouse input.
 *
 * Listeners remain registered on the canvas, document, and window while the instance is alive, and pointer lock is requested when the canvas is clicked and available.
 * The controller keeps yaw and pitch within the configured limits, applying rotation to the camera and velocity to the player on update.
 * Implementations are not thread-safe and must run on the browser main thread where DOM access is allowed.
 *
 * @param canvas HTML canvas that receives clicks and holds pointer lock.
 * @param camera target camera whose rotation is controlled by mouse movement.
 * @param player target player whose horizontal velocity is controlled by keyboard input.
 * @param settings mutable controller bindings and motion parameters.
 */
class PlayerController internal constructor(
  private val canvas: HTMLCanvasElement,
  private val camera: MovableCamera,
  private val player: Player,
  private val settings: Settings = Settings(),
) : AutoCloseable {
  /**
   * Holds mutable input bindings and movement parameters for the player controller.
   *
   * Key codes follow the `KeyboardEvent.code` values.
   * Speeds are expressed in meters per second, and the pitch limit must remain below a right angle to avoid gimbal lock.
   * Settings are not thread-safe and are intended for use on the browser main thread.
   *
   * @param forwardKey key code used for forward movement.
   * @param backwardKey key code used for backward movement.
   * @param leftKey key code used for strafing left.
   * @param rightKey key code used for strafing right.
   * @param mouseSensitivityDegPerDot mouse sensitivity in degrees per pointer movement dot.
   *
   *   range: mouseSensitivityDegPerDot >= 0.0
   *
   *   NaN: treated as invalid and rejected
   *
   *   Infinity: treated as invalid and rejected
   * @param horizontalSpeedMetersPerSecond planar movement speed in meters per second.
   *
   *   range: horizontalSpeedMetersPerSecond >= 0.0
   *
   *   NaN: treated as invalid and rejected
   *
   *   Infinity: treated as invalid and rejected
   * @param eyeHeight the vertical offset from player position to camera position.
   *
   *   NaN: treated as invalid and rejected
   *
   *   Infinity: treated as invalid and rejected
   * @param maxPitch maximum absolute pitch angle allowed before clamping.
   *   range: 0 <= maxPitch <= 90 degrees
   */
  data class Settings(
    var forwardKey: String = "KeyW",
    var backwardKey: String = "KeyS",
    var leftKey: String = "KeyA",
    var rightKey: String = "KeyD",
    var mouseSensitivityDegPerDot: Double = 0.15,
    var horizontalSpeedMetersPerSecond: Double = 4.0,
    var eyeHeight: Length = 1.62.meters,
    var maxPitch: Angle = 89.0.degrees,
  ) {
    init {
      require(mouseSensitivityDegPerDot.isFinite() && mouseSensitivityDegPerDot >= 0.0) {
        "mouseSensitivityDegPerDot must be finite and non-negative: $mouseSensitivityDegPerDot"
      }
      require(horizontalSpeedMetersPerSecond.isFinite() && horizontalSpeedMetersPerSecond >= 0.0) {
        "horizontalSpeedMetersPerSecond must be finite and non-negative: $horizontalSpeedMetersPerSecond"
      }
      require(eyeHeight.toDouble(LengthUnit.METER).isFinite()) {
        "eyeHeight must be finite: $eyeHeight"
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
  private var lastTimestamp = window.performance.now()
  private var mouseDeltaX = 0.0
  private var mouseDeltaY = 0.0
  private var lastUnlockAtMillis = Double.NEGATIVE_INFINITY
  private var closed = false

  private val keyDownListener: (Event) -> Unit =
    fun(event: Event) {
      val keyEvent = event as? KeyboardEvent ?: return
      if (keyEvent.code in navKeys()) {
        activeKeys.add(keyEvent.code)
      }
    }

  private val keyUpListener: (Event) -> Unit =
    fun(event: Event) {
      val keyEvent = event as? KeyboardEvent ?: return
      if (keyEvent.code in navKeys()) {
        activeKeys.remove(keyEvent.code)
      }
    }

  private val mouseMoveListener: (Event) -> Unit =
    fun(event: Event) {
      if (!isPointerLocked()) return
      val mouseEvent = event as? MouseEvent ?: return
      mouseDeltaX += mouseEvent.asDynamic().movementX as? Double ?: 0.0
      mouseDeltaY += mouseEvent.asDynamic().movementY as? Double ?: 0.0
    }

  private val pointerLockChangeListener: (Event) -> Unit =
    fun(_: Event) {
      if (!isPointerLocked()) {
        lastUnlockAtMillis = window.performance.now()
        activeKeys.clear()
        clearVelocity()
        logger.debug { "Pointer lock released" }
      } else {
        logger.debug { "Pointer lock acquired" }
      }
    }

  private val pointerLockErrorListener: (Event) -> Unit =
    {
      logger.warn { "Pointer lock error" }
    }

  private val clickListener: (Event) -> Unit =
    {
      if (!isPointerLocked()) {
        requestPointerLock()
      }
    }

  init {
    val initialForward = camera.transform.rotation.rotate(Direction3.forward)
    yaw = atan2(initialForward.ux, -initialForward.uz)
    pitch = asin(initialForward.uy).coerceIn(-maxPitchRadians, maxPitchRadians)
    registerListeners()
  }

  /**
   * Advances controller state by applying accumulated mouse movement and active key input.
   *
   * Returns immediately when pointer lock is not active.
   * Updates the camera rotation and player velocity based on input, then synchronizes the camera position to the player.
   */
  fun update() {
    val now = window.performance.now()
    lastTimestamp = now

    if (!isPointerLocked()) return

    if (mouseDeltaX != 0.0 || mouseDeltaY != 0.0) {
      yaw += mouseDeltaX * mouseSensitivityRadiansPerDot
      pitch =
        (pitch - mouseDeltaY * mouseSensitivityRadiansPerDot)
          .coerceIn(-maxPitchRadians, maxPitchRadians)
      mouseDeltaX = 0.0
      mouseDeltaY = 0.0
      applyRotation()
    }

    updatePlayerVelocity()

    syncCameraPosition()
  }

  override fun close() {
    if (closed) return
    closed = true
    canvas.removeEventListener("click", clickListener)
    document.removeEventListener("pointerlockchange", pointerLockChangeListener)
    document.removeEventListener("pointerlockerror", pointerLockErrorListener)
    window.removeEventListener("mousemove", mouseMoveListener)
    window.removeEventListener("keydown", keyDownListener)
    window.removeEventListener("keyup", keyUpListener)
    if (isPointerLocked()) {
      exitPointerLock()
    }
  }

  private fun registerListeners() {
    canvas.addEventListener("click", clickListener)
    document.addEventListener("pointerlockchange", pointerLockChangeListener)
    document.addEventListener("pointerlockerror", pointerLockErrorListener)
    window.addEventListener("mousemove", mouseMoveListener)
    window.addEventListener("keydown", keyDownListener)
    window.addEventListener("keyup", keyUpListener)
  }

  private fun applyRotation() {
    val cosPitch = cos(pitch)
    val forward =
      Direction3(
        ux = sin(yaw) * cosPitch,
        uy = sin(pitch),
        uz = -cos(yaw) * cosPitch,
      )
    val rotation = Quaternion.lookingAlong(forward, Direction3.up)
    camera.setRotation(rotation)
  }

  private fun syncCameraPosition() {
    val playerPos = player.position
    val eyeOffset = settings.eyeHeight
    camera.setPosition(
      Length3(
        dx = playerPos.x,
        dy = playerPos.y + eyeOffset,
        dz = playerPos.z,
      ),
    )
  }

  private fun updatePlayerVelocity() {
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

    var dx = 0.0
    var dz = 0.0

    if (settings.forwardKey in activeKeys) {
      dx += planarForward.ux
      dz += planarForward.uz
    }
    if (settings.backwardKey in activeKeys) {
      dx -= planarForward.ux
      dz -= planarForward.uz
    }
    if (settings.rightKey in activeKeys) {
      dx += planarRight.ux
      dz += planarRight.uz
    }
    if (settings.leftKey in activeKeys) {
      dx -= planarRight.ux
      dz -= planarRight.uz
    }

    val horizontalLenSq = dx * dx + dz * dz
    if (horizontalLenSq > 0.0 && settings.horizontalSpeedMetersPerSecond > 0.0) {
      val invLen = 1.0 / sqrt(horizontalLenSq)
      val speed = settings.horizontalSpeedMetersPerSecond.meters
      player.velocity.vx = speed * dx * invLen
      player.velocity.vz = speed * dz * invLen
    } else {
      player.velocity.vx = Length.ZERO
      player.velocity.vz = Length.ZERO
    }
  }

  private fun clearVelocity() {
    player.velocity.vx = Length.ZERO
    player.velocity.vz = Length.ZERO
  }

  private fun isPointerLocked(): Boolean = document.asDynamic().pointerLockElement == canvas

  private fun requestPointerLock() {
    val now = window.performance.now()
    if (now - lastUnlockAtMillis < 2000.0) {
      return
    }
    canvas
      .asDynamic()
      .requestPointerLock()
  }

  private fun exitPointerLock() {
    document
      .asDynamic()
      .exitPointerLock()
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
