package net.japanesehunter.math.test.speed

import net.japanesehunter.math.test.QuantityUnit
import net.japanesehunter.math.test.length.LengthUnit
import kotlin.time.Duration

/**
 * A unit of the [Speed] dimension.
 *
 * ## Description
 *
 * This is an alias of [QuantityUnit] specialized for [Speed].
 */
typealias SpeedUnit = QuantityUnit<Speed>

/**
 * The canonical unit of the [Speed] dimension.
 *
 * ## Description
 *
 * `1 m/s = 1 m/s`.
 */
val metersPerSecond: SpeedUnit by lazy {
  QuantityUnit.base(
    Speed,
    name = "Meter per Second",
    symbol = "m/s",
  )
}

/**
 * A speed unit where `1 km/h = (1/3.6) m/s`.
 *
 * ## Description
 *
 * This unit is derived from [metersPerSecond].
 */
val kilometersPerHour: SpeedUnit by lazy {
  metersPerSecond
    .derive(1.0 / 3.6, name = "Kilometer per Hour", symbol = "km/h")
}

/**
 * Creates a speed unit by dividing this length unit by the given [duration].
 *
 * @param duration The time divisor.
 * @return A speed unit.
 */
operator fun LengthUnit.div(
  duration: Duration,
): SpeedUnit =
  TODO()

/**
 * Creates a speed unit by dividing this length unit by the given [duration].
 *
 * @param duration The time divisor.
 * @return A speed unit.
 */
infix fun LengthUnit.per(
  duration: Duration,
): SpeedUnit =
  this / duration
