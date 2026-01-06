package net.japanesehunter.math.test.length

import net.japanesehunter.math.test.QuantityUnit

typealias LengthUnit = QuantityUnit<Length>

val meters: LengthUnit =
  QuantityUnit.base(
    Length,
    name = "Meter",
    symbol = "m",
  )

val kilometers: LengthUnit =
  meters.derive(1e3, name = "Kilometer", symbol = "km")

val centimeters: LengthUnit =
  meters.derive(1e-2, name = "Centimeter", symbol = "cm")

val millimeters: LengthUnit =
  meters.derive(1e-3, name = "Millimeter", symbol = "mm")

val micrometers: LengthUnit =
  millimeters.derive(1e-3, name = "Micrometer", symbol = "μm")

val nanometers: LengthUnit =
  micrometers.derive(1e-3, name = "Nanometer", symbol = "nm")

val miles: LengthUnit =
  kilometers.derive(1.609344, name = "Mile", symbol = "mi")

val inches: LengthUnit =
  centimeters.derive(2.54, name = "Inch", symbol = "in")

val feet: LengthUnit =
  inches.derive(12.0, name = "Foot", symbol = "ft")
