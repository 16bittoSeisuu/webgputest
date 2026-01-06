package net.japanesehunter.math.test.length

import net.japanesehunter.math.test.QuantityUnit

typealias LengthUnit = QuantityUnit<Length>

val meters: LengthUnit by lazy {
  QuantityUnit.base(
    Length,
    name = "Meter",
    symbol = "m",
  )
}

val kilometers: LengthUnit by lazy {
  meters.derive(1e3, name = "Kilometer", symbol = "km")
}

val centimeters: LengthUnit by lazy {
  meters.derive(1e-2, name = "Centimeter", symbol = "cm")
}

val millimeters: LengthUnit by lazy {
  meters.derive(1e-3, name = "Millimeter", symbol = "mm")
}

val micrometers: LengthUnit by lazy {
  millimeters.derive(1e-3, name = "Micrometer", symbol = "μm")
}

val nanometers: LengthUnit by lazy {
  micrometers.derive(1e-3, name = "Nanometer", symbol = "nm")
}

val miles: LengthUnit by lazy {
  kilometers.derive(1.609344, name = "Mile", symbol = "mi")
}

val inches: LengthUnit by lazy {
  centimeters.derive(2.54, name = "Inch", symbol = "in")
}

val feet: LengthUnit by lazy {
  inches.derive(12.0, name = "Foot", symbol = "ft")
}
