
import io.github.oshai.kotlinlogging.Level
import io.ygdrasil.webgpu.canvasContextRenderer

fun main() =
  application(loggerLevel = Level.DEBUG) {
    val canvas = canvasContextRenderer(width = 800, height = 600)
  }
