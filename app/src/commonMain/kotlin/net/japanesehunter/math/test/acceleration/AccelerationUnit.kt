package net.japanesehunter.math.test.acceleration

import net.japanesehunter.math.test.QuantityUnit
import net.japanesehunter.math.test.speed.SpeedUnit
import kotlin.time.Duration

/**
 * A unit of the [Acceleration] dimension.
 *
 * ## Description
 *
 * This is an alias of [QuantityUnit] specialized for [Acceleration].
 */
typealias AccelerationUnit = QuantityUnit<Acceleration>

/**
 * The canonical unit of the [Acceleration] dimension.
 *
 * ## Description
 *
 * `1 m/s² = 1 m/s²`.
 */
val metersPerSecondSquared: AccelerationUnit by lazy {
  QuantityUnit.base(
    Acceleration,
    name = "Meter per Second squared",
    symbol = "m/s²",
  )
}

/**
 * Creates an acceleration unit by dividing this speed unit by the given [duration].
 *
 * @param duration The time divisor.
 * @return An acceleration unit.
 */
operator fun SpeedUnit.div(
  duration: Duration,
): AccelerationUnit =
  TODO()
