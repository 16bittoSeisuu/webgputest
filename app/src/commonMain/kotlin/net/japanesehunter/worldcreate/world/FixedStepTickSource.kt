package net.japanesehunter.worldcreate.world

import kotlinx.atomicfu.locks.ReentrantLock
import kotlinx.atomicfu.locks.withLock
import kotlin.time.Duration

/**
 * Creates a fixed-step tick source and a separate driver sink.
 *
 * The returned source emits a constant tick duration.
 * The returned sink accepts the frame delta duration and advances the source.
 *
 * @param targetStep The fixed simulation step duration.
 *
 *   range: targetStep > 0
 *
 *   Infinity: throws [IllegalArgumentException]
 * @param maxStepsPerDrive The maximum number of steps performed for one drive event.
 *
 *   range: 1 <= maxStepsPerDrive
 * @return A pair of the created tick source and its driver sink.
 * @throws IllegalArgumentException If [targetStep] is not finite or is not positive.
 * @throws IllegalArgumentException If [maxStepsPerDrive] is less than 1.
 */
fun createFixedStepTickSource(
  targetStep: Duration,
  maxStepsPerDrive: Int = 5,
): Pair<TickSource, TickSink> {
  val source = FixedStepTickSource(targetStep, maxStepsPerDrive)
  val sink = TickSink { frameDelta -> source.drive(frameDelta) }
  return source to sink
}

/**
 * Represents a fixed-step tick source driven by externally supplied frame deltas.
 *
 * Instances emit a sequence of tick events where each event uses the same duration.
 * When the driver provides a large frame delta, the source performs at most a bounded number of steps and
 * discards any remaining accumulated time to avoid unbounded catch-up.
 *
 * Exceptions thrown by subscribed sinks are caught and ignored so other sinks still receive events.
 *
 * Subscription management is thread-safe.
 * Tick emission is sequential and uses a single thread.
 */
class FixedStepTickSource internal constructor(
  private val targetStep: Duration,
  private val maxStepsPerDrive: Int,
) : TickSource {
  private val lock = ReentrantLock()
  private val sinks: LinkedHashSet<TickSink> = LinkedHashSet()
  private var accumulator: Duration = Duration.ZERO

  init {
    require(targetStep.isFinite() && targetStep > Duration.ZERO) {
      "targetStep must be finite and positive: $targetStep"
    }
    require(maxStepsPerDrive >= 1) { "maxStepsPerDrive must be at least 1: $maxStepsPerDrive" }
  }

  override fun subscribe(sink: TickSink): EventSubscription {
    lock.withLock {
      sinks.add(sink)
    }
    return EventSubscription {
      lock.withLock {
        sinks.remove(sink)
      }
    }
  }

  internal fun drive(frameDelta: Duration) {
    require(frameDelta.isFinite()) { "frameDelta must be finite: $frameDelta" }
    if (frameDelta <= Duration.ZERO) return

    lock.withLock {
      accumulator += frameDelta
    }

    var steps = 0
    while (true) {
      val sinksSnapshot: List<TickSink> =
        lock.withLock {
          if (accumulator < targetStep) return
          if (steps >= maxStepsPerDrive) {
            if (accumulator >= targetStep) accumulator = Duration.ZERO
            return
          }
          accumulator -= targetStep
          steps++
          sinks.toList()
        }

      for (sink in sinksSnapshot) {
        runCatching {
          sink.onEvent(targetStep)
        }
      }
    }
  }
}
