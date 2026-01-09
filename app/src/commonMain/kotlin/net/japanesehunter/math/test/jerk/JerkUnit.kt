package net.japanesehunter.math.test.jerk

import net.japanesehunter.math.test.QuantityUnit
import net.japanesehunter.math.test.acceleration.AccelerationUnit
import kotlin.time.Duration

/**
 * A unit of the [Jerk] dimension.
 *
 * ## Description
 *
 * This is an alias of [QuantityUnit] specialized for [Jerk].
 */
typealias JerkUnit = QuantityUnit<Jerk>

/**
 * The canonical unit of the [Jerk] dimension.
 *
 * ## Description
 *
 * `1 m/s³ = 1 m/s³`.
 */
val metersPerSecondCubed: JerkUnit by lazy {
  QuantityUnit.base(
    Jerk,
    name = "Meter per Second cubed",
    symbol = "m/s³",
  )
}

/**
 * Creates a jerk unit by dividing this acceleration unit by the given [duration].
 *
 * @param duration The time divisor.
 * @return A jerk unit.
 */
operator fun AccelerationUnit.div(
  duration: Duration,
): JerkUnit =
  TODO()
