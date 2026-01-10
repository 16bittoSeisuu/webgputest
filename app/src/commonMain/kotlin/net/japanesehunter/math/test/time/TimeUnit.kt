package net.japanesehunter.math.test.time

import net.japanesehunter.math.test.QuantityUnit
import kotlin.time.Duration

/**
 * A unit of the [Time] dimension.
 *
 * ## Description
 *
 * This is an alias of [QuantityUnit] specialized for [Time].
 * It is designed to be interoperable with [Duration].
 */
typealias TimeUnit = QuantityUnit<Time>

/**
 * The canonical unit of the [Time] dimension.
 *
 * ## Description
 *
 * This unit corresponds to [kotlin.time.DurationUnit.SECONDS].
 */
val second: TimeUnit by lazy {
  QuantityUnit.base(
    Time,
    name = "Second",
    symbol = "s",
  )
}

/**
 * A time unit where `1 min = 60 s`.
 *
 * ## Description
 *
 * This unit is derived from [second].
 * This unit corresponds to [kotlin.time.DurationUnit.MINUTES].
 */
val minute: TimeUnit by lazy {
  second.derive(
    newToThisFactor = 60.0,
    name = "Minute",
    symbol = "min",
  )
}

/**
 * A time unit where `1 h = 3600 s`.
 *
 * ## Description
 *
 * This unit is derived from [minute] (or [second]).
 * This unit corresponds to [kotlin.time.DurationUnit.HOURS].
 */
val hour: TimeUnit by lazy {
  minute.derive(
    newToThisFactor = 60.0,
    name = "Hour",
    symbol = "h",
  )
}

/**
 * A time unit where `1 d = 86400 s`.
 *
 * ## Description
 *
 * This unit is derived from [hour] (or [second]).
 * This unit corresponds to [kotlin.time.DurationUnit.DAYS].
 */
val day: TimeUnit by lazy {
  hour.derive(
    newToThisFactor = 24.0,
    name = "Day",
    symbol = "d",
  )
}

/**
 * A time unit where `1 ms = 0.001 s`.
 *
 * ## Description
 *
 * This unit is derived from [second].
 * This unit corresponds to [kotlin.time.DurationUnit.MILLISECONDS].
 */
val millisecond: TimeUnit by lazy {
  second.derive(
    newToThisFactor = 1e-3,
    name = "Millisecond",
    symbol = "ms",
  )
}

/**
 * A time unit where `1 us = 1e-6 s`.
 *
 * ## Description
 *
 * This unit is derived from [millisecond] (or [second]).
 * This unit corresponds to [kotlin.time.DurationUnit.MICROSECONDS].
 */
val microsecond: TimeUnit by lazy {
  millisecond.derive(
    newToThisFactor = 1e-3,
    name = "Microsecond",
    symbol = "us",
  )
}

/**
 * A time unit where `1 ns = 1e-9 s`.
 *
 * ## Description
 *
 * This unit is derived from [microsecond] (or [second]).
 * This unit corresponds to [kotlin.time.DurationUnit.NANOSECONDS].
 */
val nanosecond: TimeUnit by lazy {
  microsecond.derive(
    newToThisFactor = 1e-3,
    name = "Nanosecond",
    symbol = "ns",
  )
}
