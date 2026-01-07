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
 *
 * ## Description
 *
 * `1 m = 1 m`.
 */
val meters: LengthUnit by lazy {
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
 * This unit is derived from [meters].
 */
val kilometers: LengthUnit by lazy {
  meters.derive(1e3, name = "Kilometer", symbol = "km")
}

/**
 * A length unit where `1 cm = 0.01 m`.
 *
 * ## Description
 *
 * This unit is derived from [meters].
 */
val centimeters: LengthUnit by lazy {
  meters.derive(1e-2, name = "Centimeter", symbol = "cm")
}

/**
 * A length unit where `1 mm = 0.001 m`.
 *
 * ## Description
 *
 * This unit is derived from [meters].
 */
val millimeters: LengthUnit by lazy {
  meters.derive(1e-3, name = "Millimeter", symbol = "mm")
}

/**
 * A length unit where `1 μm = 1e-6 m`.
 *
 * ## Description
 *
 * This unit is derived from [millimeters].
 */
val micrometers: LengthUnit by lazy {
  millimeters.derive(1e-3, name = "Micrometer", symbol = "μm")
}

/**
 * A length unit where `1 nm = 1e-9 m`.
 *
 * ## Description
 *
 * This unit is derived from [micrometers].
 */
val nanometers: LengthUnit by lazy {
  micrometers.derive(1e-3, name = "Nanometer", symbol = "nm")
}

/**
 * A length unit where `1 mi = 1609.344 m`.
 *
 * ## Description
 *
 * This unit is derived from [kilometers].
 */
val miles: LengthUnit by lazy {
  kilometers.derive(1.609344, name = "Mile", symbol = "mi")
}

/**
 * A length unit where `1 in = 0.0254 m`.
 *
 * ## Description
 *
 * This unit is derived from [centimeters].
 */
val inches: LengthUnit by lazy {
  centimeters.derive(2.54, name = "Inch", symbol = "in")
}

/**
 * A length unit where `1 ft = 0.3048 m`.
 *
 * ## Description
 *
 * This unit is derived from [inches].
 */
val feet: LengthUnit by lazy {
  inches.derive(12.0, name = "Foot", symbol = "ft")
}
