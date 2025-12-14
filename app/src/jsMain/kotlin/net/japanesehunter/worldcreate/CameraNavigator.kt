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
import net.japanesehunter.math.zero
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.events.Event
import org.w3c.dom.events.KeyboardEvent
import org.w3c.dom.events.MouseEvent
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sign
import kotlin.math.sin
import kotlin.math.sqrt

class CameraNavigator(
  private val canvas: HTMLCanvasElement,
  private val camera: MovableCamera,
  private val settings: Settings = Settings(),
) : AutoCloseable {
  data class Settings(
    var forwardKey: String = "KeyW",
    var backwardKey: String = "KeyS",
    var leftKey: String = "KeyA",
    var rightKey: String = "KeyD",
    var upKey: String = "Space",
    var downKey: String = "ShiftLeft",
    var mouseSensitivityDegPerDot: Double = 0.15,
    var horizontalSpeedMetersPerSecond: Double = 4.0,
    var verticalSpeedMetersPerSecond: Double = 4.0,
    var maxPitch: Angle = 89.0.degrees,
  ) {
    init {
      require(mouseSensitivityDegPerDot.isFinite() && mouseSensitivityDegPerDot >= 0.0) {
        "mouseSensitivityDegPerDot must be finite and >= 0.0."
      }
      require(horizontalSpeedMetersPerSecond.isFinite() && horizontalSpeedMetersPerSecond >= 0.0) {
        "horizontalSpeedMetersPerSecond must be finite and >= 0.0."
      }
      require(verticalSpeedMetersPerSecond.isFinite() && verticalSpeedMetersPerSecond >= 0.0) {
        "verticalSpeedMetersPerSecond must be finite and >= 0.0."
      }
      val maxPitchRadians = maxPitch.toDouble(AngleUnit.RADIAN)
      require(maxPitchRadians in 0.0..(0.5 * PI)) {
        "maxPitch must be between 0 and 90 degrees."
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
      activeKeys.add(keyEvent.code)
      if (keyEvent.code in navKeys()) {
        keyEvent.preventDefault()
      }
    }

  private val keyUpListener: (Event) -> Unit =
    fun(event: Event) {
      val keyEvent = event as? KeyboardEvent ?: return
      activeKeys.remove(keyEvent.code)
      if (keyEvent.code in navKeys()) {
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
        lastUnlockAtMillis = window.performance.now()
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
      yaw += mouseDeltaX * mouseSensitivityRadiansPerDot
      pitch =
        (pitch - mouseDeltaY * mouseSensitivityRadiansPerDot)
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
    if (settings.upKey in activeKeys) {
      dy += 1.0
    }
    if (settings.downKey in activeKeys) {
      dy -= 1.0
    }

    val horizontalLenSq = dx * dx + dz * dz
    val horizontalMove =
      if (horizontalLenSq > 0.0 && settings.horizontalSpeedMetersPerSecond > 0.0) {
        val invLen = 1.0 / sqrt(horizontalLenSq)
        val distance = settings.horizontalSpeedMetersPerSecond.meters * deltaSeconds
        Length3(
          dx = distance * dx * invLen,
          dy = Length.ZERO,
          dz = distance * dz * invLen,
        )
      } else {
        Length3.zero
      }

    val verticalMove =
      if (dy != 0.0 && settings.verticalSpeedMetersPerSecond > 0.0) {
        val distance = settings.verticalSpeedMetersPerSecond.meters * deltaSeconds
        Length3(
          dx = Length.ZERO,
          dy = distance * sign(dy),
          dz = Length.ZERO,
        )
      } else {
        Length3.zero
      }

    val total =
      Length3(
        dx = horizontalMove.dx + verticalMove.dx,
        dy = horizontalMove.dy + verticalMove.dy,
        dz = horizontalMove.dz + verticalMove.dz,
      )
    return if (total == Length3.zero) null else total
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
      settings.upKey,
      settings.downKey,
    )
}

private val logger = logger("CameraNavigator")
