package net.japanesehunter.worldcreate

import io.github.oshai.kotlinlogging.KotlinLogging.logger
import kotlinx.browser.document
import kotlinx.browser.window
import net.japanesehunter.math.Angle
import net.japanesehunter.math.AngleUnit
import net.japanesehunter.math.Direction3
import net.japanesehunter.math.Length
import net.japanesehunter.math.Length3
import net.japanesehunter.math.MovableCamera
import net.japanesehunter.math.Quaternion
import net.japanesehunter.math.degrees
import net.japanesehunter.math.forward
import net.japanesehunter.math.lookingAlong
import net.japanesehunter.math.meters
import net.japanesehunter.math.rotate
import net.japanesehunter.math.setRotation
import net.japanesehunter.math.translate
import net.japanesehunter.math.up
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

class CameraNavigator(
  private val canvas: HTMLCanvasElement,
  private val camera: MovableCamera,
  private val config: Config = Config(),
) : AutoCloseable {
  data class Config(
    val forwardKey: String = "KeyW",
    val backwardKey: String = "KeyS",
    val leftKey: String = "KeyA",
    val rightKey: String = "KeyD",
    val upKey: String = "Space",
    val downKey: String = "ShiftLeft",
    val mouseSensitivity: Double = 0.0025,
    val moveSpeed: Length = 4.0.meters,
    val maxPitch: Angle = 89.0.degrees,
  ) {
    init {
      require(mouseSensitivity.isFinite() && mouseSensitivity > 0.0) {
        "mouseSensitivity must be finite and positive."
      }
      require(moveSpeed.isPositive) {
        "moveSpeed must be positive."
      }
      val maxPitchRadians = maxPitch.toDouble(AngleUnit.RADIAN)
      require(maxPitchRadians in 0.0..(0.5 * PI)) {
        "maxPitch must be between 0 and 90 degrees."
      }
    }
  }

  private val navKeys: Set<String> =
    setOf(
      config.forwardKey,
      config.backwardKey,
      config.leftKey,
      config.rightKey,
      config.upKey,
      config.downKey,
    )

  private val maxPitchRadians = config.maxPitch.toDouble(AngleUnit.RADIAN)
  private val activeKeys = mutableSetOf<String>()
  private var yaw: Double
  private var pitch: Double
  private var lastTimestamp = window.performance.now()
  private var mouseDeltaX = 0.0
  private var mouseDeltaY = 0.0
  private var closed = false

  private val keyDownListener: (Event) -> Unit =
    fun(event: Event) {
      val keyEvent = event as? KeyboardEvent ?: return
      if (keyEvent.code == "Escape" && isPointerLocked()) {
        exitPointerLock()
        return
      }
      activeKeys.add(keyEvent.code)
      if (keyEvent.code in navKeys) {
        keyEvent.preventDefault()
      }
    }

  private val keyUpListener: (Event) -> Unit =
    fun(event: Event) {
      val keyEvent = event as? KeyboardEvent ?: return
      activeKeys.remove(keyEvent.code)
      if (keyEvent.code in navKeys) {
        keyEvent.preventDefault()
      }
    }

  private val mouseMoveListener: (Event) -> Unit =
    fun(event: Event) {
      val mouseEvent = event as? MouseEvent ?: return
      if (!isPointerLocked()) return
      val dx = mouseEvent.asDynamic().movementX as? Double ?: 0.0
      val dy = mouseEvent.asDynamic().movementY as? Double ?: 0.0
      mouseDeltaX += dx
      mouseDeltaY += dy
    }

  private val pointerLockChangeListener: (Event) -> Unit =
    fun(_: Event) {
      if (isPointerLocked()) {
        mouseDeltaX = 0.0
        mouseDeltaY = 0.0
        lastTimestamp = window.performance.now()
      } else {
        activeKeys.clear()
      }
    }

  private val pointerLockErrorListener: (Event) -> Unit =
    {
      logger.warn { "Pointer lock request failed." }
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

  fun update() {
    val now = window.performance.now()
    val deltaSeconds = ((now - lastTimestamp) / 1000.0).coerceAtLeast(0.0)
    lastTimestamp = now

    if (!isPointerLocked()) return

    if (mouseDeltaX != 0.0 || mouseDeltaY != 0.0) {
      yaw += mouseDeltaX * config.mouseSensitivity
      pitch =
        (pitch - mouseDeltaY * config.mouseSensitivity)
          .coerceIn(-maxPitchRadians, maxPitchRadians)
      mouseDeltaX = 0.0
      mouseDeltaY = 0.0
      applyRotation()
    }

    val translation = computeTranslation(deltaSeconds)
    if (translation != null) {
      camera.translate(translation)
    }
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

  private fun computeTranslation(deltaSeconds: Double): Length3? {
    if (deltaSeconds <= 0.0) return null

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
    var dy = 0.0
    var dz = 0.0

    if (config.forwardKey in activeKeys) {
      dx += planarForward.ux
      dz += planarForward.uz
    }
    if (config.backwardKey in activeKeys) {
      dx -= planarForward.ux
      dz -= planarForward.uz
    }
    if (config.rightKey in activeKeys) {
      dx += planarRight.ux
      dz += planarRight.uz
    }
    if (config.leftKey in activeKeys) {
      dx -= planarRight.ux
      dz -= planarRight.uz
    }
    if (config.upKey in activeKeys) {
      dy += 1.0
    }
    if (config.downKey in activeKeys) {
      dy -= 1.0
    }

    val lengthSq = dx * dx + dy * dy + dz * dz
    if (lengthSq == 0.0) return null
    val invLen = 1.0 / sqrt(lengthSq)
    val distance = config.moveSpeed * deltaSeconds
    return Length3(
      dx = distance * dx * invLen,
      dy = distance * dy * invLen,
      dz = distance * dz * invLen,
    )
  }

  private fun isPointerLocked(): Boolean = document.asDynamic().pointerLockElement == canvas

  private fun requestPointerLock() {
    canvas
      .asDynamic()
      .requestPointerLock()
  }

  private fun exitPointerLock() {
    document
      .asDynamic()
      .exitPointerLock()
  }
}

private val logger = logger("CameraNavigator")
