package net.japanesehunter.math.test.length

import net.japanesehunter.math.test.length.centimeters as centimeter_unit
import net.japanesehunter.math.test.length.feet as feet_unit
import net.japanesehunter.math.test.length.inches as inch_unit
import net.japanesehunter.math.test.length.kilometers as kilometer_unit
import net.japanesehunter.math.test.length.meters as meter_unit
import net.japanesehunter.math.test.length.micrometers as micrometer_unit
import net.japanesehunter.math.test.length.miles as mile_unit
import net.japanesehunter.math.test.length.millimeters as millimeter_unit
import net.japanesehunter.math.test.length.nanometers as nanometer_unit

interface LengthProvider {
  // region operators

  operator fun Number.times(
    unit: LengthUnit,
  ): LengthQuantity =
    toDouble() * unit

  operator fun Int.times(
    unit: LengthUnit,
  ): LengthQuantity =
    toLong() * unit

  operator fun Long.times(
    unit: LengthUnit,
  ): LengthQuantity

  operator fun Float.times(
    unit: LengthUnit,
  ): LengthQuantity =
    toDouble() * unit

  operator fun Double.times(
    unit: LengthUnit,
  ): LengthQuantity

  // endregion
  val zero: LengthQuantity get() = 0.meters
  // region SI
  // region meters

  val Number.meters: LengthQuantity get() = times(meter_unit)

  val Int.meters: LengthQuantity get() = times(meter_unit)

  val Long.meters: LengthQuantity get() = times(meter_unit)

  val Float.meters: LengthQuantity get() = times(meter_unit)

  val Double.meters: LengthQuantity get() = times(meter_unit)

  // endregion
  // region kilometers

  val Number.kilometers: LengthQuantity get() = times(kilometer_unit)

  val Int.kilometers: LengthQuantity get() = times(kilometer_unit)

  val Long.kilometers: LengthQuantity get() = times(kilometer_unit)

  val Float.kilometers: LengthQuantity get() = times(kilometer_unit)

  val Double.kilometers: LengthQuantity get() = times(kilometer_unit)

  // endregion
  // region centimeters

  val Number.centimeters: LengthQuantity get() = times(centimeter_unit)

  val Int.centimeters: LengthQuantity get() = times(centimeter_unit)

  val Long.centimeters: LengthQuantity get() = times(centimeter_unit)

  val Float.centimeters: LengthQuantity get() = times(centimeter_unit)

  val Double.centimeters: LengthQuantity get() = times(centimeter_unit)

  // endregion
  // region millimeters

  val Number.millimeters: LengthQuantity get() = times(millimeter_unit)

  val Int.millimeters: LengthQuantity get() = times(millimeter_unit)

  val Long.millimeters: LengthQuantity get() = times(millimeter_unit)

  val Float.millimeters: LengthQuantity get() = times(millimeter_unit)

  val Double.millimeters: LengthQuantity get() = times(millimeter_unit)

  // endregion
  // region micrometers

  val Number.micrometers: LengthQuantity get() = times(micrometer_unit)

  val Int.micrometers: LengthQuantity get() = times(micrometer_unit)

  val Long.micrometers: LengthQuantity get() = times(micrometer_unit)

  val Float.micrometers: LengthQuantity get() = times(micrometer_unit)

  val Double.micrometers: LengthQuantity get() = times(micrometer_unit)

  // endregion
  // region nanometers

  val Number.nanometers: LengthQuantity get() = times(nanometer_unit)

  val Int.nanometers: LengthQuantity get() = times(nanometer_unit)

  val Long.nanometers: LengthQuantity get() = times(nanometer_unit)

  val Float.nanometers: LengthQuantity get() = times(nanometer_unit)

  val Double.nanometers: LengthQuantity get() = times(nanometer_unit)

  // endregion
  // endregion
  // region Imperial
  // region miles

  val Number.miles: LengthQuantity get() = times(mile_unit)

  val Int.miles: LengthQuantity get() = times(mile_unit)

  val Long.miles: LengthQuantity get() = times(mile_unit)

  val Float.miles: LengthQuantity get() = times(mile_unit)

  val Double.miles: LengthQuantity get() = times(mile_unit)

  // endregion
  // region feet

  val Number.feet: LengthQuantity get() = times(feet_unit)

  val Int.feet: LengthQuantity get() = times(feet_unit)

  val Long.feet: LengthQuantity get() = times(feet_unit)

  val Float.feet: LengthQuantity get() = times(feet_unit)

  val Double.feet: LengthQuantity get() = times(feet_unit)

  // endregion
  // region inches

  val Number.inches: LengthQuantity get() = times(inch_unit)

  val Int.inches: LengthQuantity get() = times(inch_unit)

  val Long.inches: LengthQuantity get() = times(inch_unit)

  val Float.inches: LengthQuantity get() = times(inch_unit)

  val Double.inches: LengthQuantity get() = times(inch_unit)

  // endregion
  // endregion
}

