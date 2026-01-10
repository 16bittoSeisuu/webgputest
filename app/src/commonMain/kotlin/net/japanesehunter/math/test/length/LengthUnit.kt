package net.japanesehunter.math.test.length

import net.japanesehunter.math.test.QuantityUnit

/**
 * A unit of the [Length] dimension.
 *
 * ## Description
 *
 * This is an alias of [QuantityUnit] specialized for [Length].
 */
typealias LengthUnit = QuantityUnit<Length>

/**
 * The canonical unit of the [Length] dimension.
 */
val meter: LengthUnit by lazy {
  QuantityUnit.base(
    Length,
    name = "Meter",
    symbol = "m",
  )
}

/**
 * A length unit where `1 km = 1000 m`.
 *
 * ## Description
 *
 * This unit is derived from [meter].
 */
val kilometer: LengthUnit by lazy {
  meter.derive(1e3, name = "Kilometer", symbol = "km")
}

/**
 * A length unit where `1 cm = 0.01 m`.
 *
 * ## Description
 *
 * This unit is derived from [meter].
 */
val centimeter: LengthUnit by lazy {
  meter.derive(1e-2, name = "Centimeter", symbol = "cm")
}

/**
 * A length unit where `1 mm = 0.001 m`.
 *
 * ## Description
 *
 * This unit is derived from [meter].
 */
val millimeter: LengthUnit by lazy {
  meter.derive(1e-3, name = "Millimeter", symbol = "mm")
}

/**
 * A length unit where `1 μm = 1e-6 m`.
 *
 * ## Description
 *
 * This unit is derived from [millimeter].
 */
val micrometer: LengthUnit by lazy {
  millimeter.derive(1e-3, name = "Micrometer", symbol = "μm")
}

/**
 * A length unit where `1 nm = 1e-9 m`.
 *
 * ## Description
 *
 * This unit is derived from [micrometer].
 */
val nanometer: LengthUnit by lazy {
  micrometer.derive(1e-3, name = "Nanometer", symbol = "nm")
}

/**
 * A length unit where `1 mi = 1609.344 m`.
 *
 * ## Description
 *
 * This unit is derived from [kilometer].
 */
val mile: LengthUnit by lazy {
  kilometer.derive(1.609344, name = "Mile", symbol = "mi")
}

/**
 * A length unit where `1 in = 0.0254 m`.
 *
 * ## Description
 *
 * This unit is derived from [centimeter].
 */
val inch: LengthUnit by lazy {
  centimeter.derive(2.54, name = "Inch", symbol = "in")
}

/**
 * A length unit where `1 ft = 0.3048 m`.
 *
 * ## Description
 *
 * This unit is derived from [inch].
 */
val foot: LengthUnit by lazy {
  inch.derive(12.0, name = "Foot", symbol = "ft")
}
