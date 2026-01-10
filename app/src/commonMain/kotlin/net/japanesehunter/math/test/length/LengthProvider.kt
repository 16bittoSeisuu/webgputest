package net.japanesehunter.math.test.length

import net.japanesehunter.math.test.length.centimeter as centimeter_unit
import net.japanesehunter.math.test.length.foot as feet_unit
import net.japanesehunter.math.test.length.inch as inch_unit
import net.japanesehunter.math.test.length.kilometer as kilometer_unit
import net.japanesehunter.math.test.length.meter as meter_unit
import net.japanesehunter.math.test.length.micrometer as micrometer_unit
import net.japanesehunter.math.test.length.mile as mile_unit
import net.japanesehunter.math.test.length.millimeter as millimeter_unit
import net.japanesehunter.math.test.length.nanometer as nanometer_unit

/**
 * Provides constructors and unit accessors for [LengthQuantity].
 *
 * ## Description
 *
 * This interface defines:
 * - Multiplication operators to construct a length from primitive numbers and a [LengthUnit].
 * - Convenience extension properties such as [Int.meters].
 *
 * The sign of the receiver is preserved.
 */
interface LengthProvider {
  // region operators

  /**
   * Creates a [LengthQuantity] representing this numeric value in [unit].
   *
   * ## Description
   *
   * The sign of the receiver is preserved.
   * The returned quantity may be rounded internally.
   * Converting it back with `toInt(unit)` is not guaranteed to reproduce the receiver.
   *
   * @param unit The unit of the constructed value.
   * @return A length quantity representing `this` expressed in [unit].
   */
  operator fun Int.times(
    unit: LengthUnit,
  ): LengthQuantity =
    toLong() * unit

  /**
   * Creates a [LengthQuantity] representing this numeric value in [unit].
   *
   * ## Description
   *
   * The sign of the receiver is preserved.
   * The returned quantity may be rounded internally.
   * Converting it back with `toLong(unit)` is not guaranteed to reproduce the receiver.
   *
   * @param unit The unit of the constructed value.
   * @return A length quantity representing `this` expressed in [unit].
   */
  operator fun Long.times(
    unit: LengthUnit,
  ): LengthQuantity

  /**
   * Creates a [LengthQuantity] representing this numeric value in [unit].
   *
   * ## Description
   *
   * The sign of the receiver is preserved.
   * The returned quantity may be rounded internally.
   * Converting it back with `toDouble(unit)` is not guaranteed to reproduce the receiver.
   *
   * @param unit The unit of the constructed value.
   * @return A length quantity representing `this` expressed in [unit].
   * @throws IllegalArgumentException
   * - If the receiver is NaN.
   * - If the receiver is positive infinity or negative infinity.
   */
  operator fun Float.times(
    unit: LengthUnit,
  ): LengthQuantity =
    toDouble() * unit

  /**
   * Creates a [LengthQuantity] representing this numeric value in [unit].
   *
   * ## Description
   *
   * The sign of the receiver is preserved.
   * The returned quantity may be rounded internally.
   * Converting it back with `toDouble(unit)` is not guaranteed to reproduce the receiver.
   *
   * @param unit The unit of the constructed value.
   * @return A length quantity representing `this` expressed in [unit].
   * @throws IllegalArgumentException
   * - If the receiver is NaN.
   * - If the receiver is positive infinity or negative infinity.
   */
  operator fun Double.times(
    unit: LengthUnit,
  ): LengthQuantity

  // endregion

  /**
   * Returns the additive identity for length quantities.
   *
   * @return A length quantity equal to `0.meters`.
   */
  val zero: LengthQuantity get() = 0.meters
  // region SI
  // region meters

  /**
   * Creates a [LengthQuantity] representing this numeric value in meters.
   *
   * ## Description
   *
   * The sign of the receiver is preserved.
   * The returned quantity may be rounded internally.
   * Converting it back with `toInt(meters)` is not guaranteed to reproduce the receiver.
   *
   * @return A length quantity equal to `this * meters`.
   */
  val Int.meters: LengthQuantity get() = times(meter_unit)

  /**
   * Creates a [LengthQuantity] representing this numeric value in meters.
   *
   * ## Description
   *
   * The sign of the receiver is preserved.
   * The returned quantity may be rounded internally.
   * Converting it back with `toLong(meters)` is not guaranteed to reproduce the receiver.
   *
   * @return A length quantity equal to `this * meters`.
   */
  val Long.meters: LengthQuantity get() = times(meter_unit)

  /**
   * Creates a [LengthQuantity] representing this numeric value in meters.
   *
   * ## Description
   *
   * The sign of the receiver is preserved.
   * The returned quantity may be rounded internally.
   * Converting it back with `toDouble(meters)` is not guaranteed to reproduce the receiver.
   *
   * @return A length quantity equal to `this * meters`.
   * @throws IllegalArgumentException
   * - If the receiver is NaN.
   * - If the receiver is positive infinity or negative infinity.
   */
  val Float.meters: LengthQuantity get() = times(meter_unit)

  /**
   * Creates a [LengthQuantity] representing this numeric value in meters.
   *
   * ## Description
   *
   * The sign of the receiver is preserved.
   * The returned quantity may be rounded internally.
   * Converting it back with `toDouble(meters)` is not guaranteed to reproduce the receiver.
   *
   * @return A length quantity equal to `this * meters`.
   * @throws IllegalArgumentException
   * - If the receiver is NaN.
   * - If the receiver is positive infinity or negative infinity.
   */
  val Double.meters: LengthQuantity get() = times(meter_unit)

  // endregion
  // region kilometers

  /**
   * Creates a [LengthQuantity] representing this numeric value in kilometers.
   *
   * ## Description
   *
   * The sign of the receiver is preserved.
   * The returned quantity may be rounded internally.
   * Converting it back with `toInt(kilometers)` is not guaranteed to reproduce the receiver.
   *
   * @return A length quantity equal to `this * kilometers`.
   */
  val Int.kilometers: LengthQuantity get() = times(kilometer_unit)

  /**
   * Creates a [LengthQuantity] representing this numeric value in kilometers.
   *
   * ## Description
   *
   * The sign of the receiver is preserved.
   * The returned quantity may be rounded internally.
   * Converting it back with `toLong(kilometers)` is not guaranteed to reproduce the receiver.
   *
   * @return A length quantity equal to `this * kilometers`.
   */
  val Long.kilometers: LengthQuantity get() = times(kilometer_unit)

  /**
   * Creates a [LengthQuantity] representing this numeric value in kilometers.
   *
   * ## Description
   *
   * The sign of the receiver is preserved.
   * The returned quantity may be rounded internally.
   * Converting it back with `toDouble(kilometers)` is not guaranteed to reproduce the receiver.
   *
   * @return A length quantity equal to `this * kilometers`.
   * @throws IllegalArgumentException
   * - If the receiver is NaN.
   * - If the receiver is positive infinity or negative infinity.
   */
  val Float.kilometers: LengthQuantity get() = times(kilometer_unit)

  /**
   * Creates a [LengthQuantity] representing this numeric value in kilometers.
   *
   * ## Description
   *
   * The sign of the receiver is preserved.
   * The returned quantity may be rounded internally.
   * Converting it back with `toDouble(kilometers)` is not guaranteed to reproduce the receiver.
   *
   * @return A length quantity equal to `this * kilometers`.
   * @throws IllegalArgumentException
   * - If the receiver is NaN.
   * - If the receiver is positive infinity or negative infinity.
   */
  val Double.kilometers: LengthQuantity get() = times(kilometer_unit)

  // endregion
  // region centimeters

  /**
   * Creates a [LengthQuantity] representing this numeric value in centimeters.
   *
   * ## Description
   *
   * The sign of the receiver is preserved.
   * The returned quantity may be rounded internally.
   * Converting it back with `toInt(centimeters)` is not guaranteed to reproduce the receiver.
   *
   * @return A length quantity equal to `this * centimeters`.
   */
  val Int.centimeters: LengthQuantity get() = times(centimeter_unit)

  /**
   * Creates a [LengthQuantity] representing this numeric value in centimeters.
   *
   * ## Description
   *
   * The sign of the receiver is preserved.
   * The returned quantity may be rounded internally.
   * Converting it back with `toLong(centimeters)` is not guaranteed to reproduce the receiver.
   *
   * @return A length quantity equal to `this * centimeters`.
   */
  val Long.centimeters: LengthQuantity get() = times(centimeter_unit)

  /**
   * Creates a [LengthQuantity] representing this numeric value in centimeters.
   *
   * ## Description
   *
   * The sign of the receiver is preserved.
   * The returned quantity may be rounded internally.
   * Converting it back with `toDouble(centimeters)` is not guaranteed to reproduce the receiver.
   *
   * @return A length quantity equal to `this * centimeters`.
   * @throws IllegalArgumentException
   * - If the receiver is NaN.
   * - If the receiver is positive infinity or negative infinity.
   */
  val Float.centimeters: LengthQuantity get() = times(centimeter_unit)

  /**
   * Creates a [LengthQuantity] representing this numeric value in centimeters.
   *
   * ## Description
   *
   * The sign of the receiver is preserved.
   * The returned quantity may be rounded internally.
   * Converting it back with `toDouble(centimeters)` is not guaranteed to reproduce the receiver.
   *
   * @return A length quantity equal to `this * centimeters`.
   * @throws IllegalArgumentException
   * - If the receiver is NaN.
   * - If the receiver is positive infinity or negative infinity.
   */
  val Double.centimeters: LengthQuantity get() = times(centimeter_unit)

  // endregion
  // region millimeters

  /**
   * Creates a [LengthQuantity] representing this numeric value in millimeters.
   *
   * ## Description
   *
   * The sign of the receiver is preserved.
   * The returned quantity may be rounded internally.
   * Converting it back with `toInt(millimeters)` is not guaranteed to reproduce the receiver.
   *
   * @return A length quantity equal to `this * millimeters`.
   */
  val Int.millimeters: LengthQuantity get() = times(millimeter_unit)

  /**
   * Creates a [LengthQuantity] representing this numeric value in millimeters.
   *
   * ## Description
   *
   * The sign of the receiver is preserved.
   * The returned quantity may be rounded internally.
   * Converting it back with `toLong(millimeters)` is not guaranteed to reproduce the receiver.
   *
   * @return A length quantity equal to `this * millimeters`.
   */
  val Long.millimeters: LengthQuantity get() = times(millimeter_unit)

  /**
   * Creates a [LengthQuantity] representing this numeric value in millimeters.
   *
   * ## Description
   *
   * The sign of the receiver is preserved.
   * The returned quantity may be rounded internally.
   * Converting it back with `toDouble(millimeters)` is not guaranteed to reproduce the receiver.
   *
   * @return A length quantity equal to `this * millimeters`.
   * @throws IllegalArgumentException
   * - If the receiver is NaN.
   * - If the receiver is positive infinity or negative infinity.
   */
  val Float.millimeters: LengthQuantity get() = times(millimeter_unit)

  /**
   * Creates a [LengthQuantity] representing this numeric value in millimeters.
   *
   * ## Description
   *
   * The sign of the receiver is preserved.
   * The returned quantity may be rounded internally.
   * Converting it back with `toDouble(millimeters)` is not guaranteed to reproduce the receiver.
   *
   * @return A length quantity equal to `this * millimeters`.
   * @throws IllegalArgumentException
   * - If the receiver is NaN.
   * - If the receiver is positive infinity or negative infinity.
   */
  val Double.millimeters: LengthQuantity get() = times(millimeter_unit)

  // endregion
  // region micrometers

  /**
   * Creates a [LengthQuantity] representing this numeric value in micrometers.
   *
   * ## Description
   *
   * The sign of the receiver is preserved.
   * The returned quantity may be rounded internally.
   * Converting it back with `toInt(micrometers)` is not guaranteed to reproduce the receiver.
   *
   * @return A length quantity equal to `this * micrometers`.
   */
  val Int.micrometers: LengthQuantity get() = times(micrometer_unit)

  /**
   * Creates a [LengthQuantity] representing this numeric value in micrometers.
   *
   * ## Description
   *
   * The sign of the receiver is preserved.
   * The returned quantity may be rounded internally.
   * Converting it back with `toLong(micrometers)` is not guaranteed to reproduce the receiver.
   *
   * @return A length quantity equal to `this * micrometers`.
   */
  val Long.micrometers: LengthQuantity get() = times(micrometer_unit)

  /**
   * Creates a [LengthQuantity] representing this numeric value in micrometers.
   *
   * ## Description
   *
   * The sign of the receiver is preserved.
   * The returned quantity may be rounded internally.
   * Converting it back with `toDouble(micrometers)` is not guaranteed to reproduce the receiver.
   *
   * @return A length quantity equal to `this * micrometers`.
   * @throws IllegalArgumentException
   * - If the receiver is NaN.
   * - If the receiver is positive infinity or negative infinity.
   */
  val Float.micrometers: LengthQuantity get() = times(micrometer_unit)

  /**
   * Creates a [LengthQuantity] representing this numeric value in micrometers.
   *
   * ## Description
   *
   * The sign of the receiver is preserved.
   * The returned quantity may be rounded internally.
   * Converting it back with `toDouble(micrometers)` is not guaranteed to reproduce the receiver.
   *
   * @return A length quantity equal to `this * micrometers`.
   * @throws IllegalArgumentException
   * - If the receiver is NaN.
   * - If the receiver is positive infinity or negative infinity.
   */
  val Double.micrometers: LengthQuantity get() = times(micrometer_unit)

  // endregion
  // region nanometers

  /**
   * Creates a [LengthQuantity] representing this numeric value in nanometers.
   *
   * ## Description
   *
   * The sign of the receiver is preserved.
   * The returned quantity may be rounded internally.
   * Converting it back with `toInt(nanometers)` is not guaranteed to reproduce the receiver.
   *
   * @return A length quantity equal to `this * nanometers`.
   */
  val Int.nanometers: LengthQuantity get() = times(nanometer_unit)

  /**
   * Creates a [LengthQuantity] representing this numeric value in nanometers.
   *
   * ## Description
   *
   * The sign of the receiver is preserved.
   * The returned quantity may be rounded internally.
   * Converting it back with `toLong(nanometers)` is not guaranteed to reproduce the receiver.
   *
   * @return A length quantity equal to `this * nanometers`.
   */
  val Long.nanometers: LengthQuantity get() = times(nanometer_unit)

  /**
   * Creates a [LengthQuantity] representing this numeric value in nanometers.
   *
   * ## Description
   *
   * The sign of the receiver is preserved.
   * The returned quantity may be rounded internally.
   * Converting it back with `toDouble(nanometers)` is not guaranteed to reproduce the receiver.
   *
   * @return A length quantity equal to `this * nanometers`.
   * @throws IllegalArgumentException
   * - If the receiver is NaN.
   * - If the receiver is positive infinity or negative infinity.
   */
  val Float.nanometers: LengthQuantity get() = times(nanometer_unit)

  /**
   * Creates a [LengthQuantity] representing this numeric value in nanometers.
   *
   * ## Description
   *
   * The sign of the receiver is preserved.
   * The returned quantity may be rounded internally.
   * Converting it back with `toDouble(nanometers)` is not guaranteed to reproduce the receiver.
   *
   * @return A length quantity equal to `this * nanometers`.
   * @throws IllegalArgumentException
   * - If the receiver is NaN.
   * - If the receiver is positive infinity or negative infinity.
   */
  val Double.nanometers: LengthQuantity get() = times(nanometer_unit)

  // endregion
  // endregion
  // region Imperial
  // region miles

  /**
   * Creates a [LengthQuantity] representing this numeric value in miles.
   *
   * ## Description
   *
   * The sign of the receiver is preserved.
   * The returned quantity may be rounded internally.
   * Converting it back with `toInt(miles)` is not guaranteed to reproduce the receiver.
   *
   * @return A length quantity equal to `this * miles`.
   */
  val Int.miles: LengthQuantity get() = times(mile_unit)

  /**
   * Creates a [LengthQuantity] representing this numeric value in miles.
   *
   * ## Description
   *
   * The sign of the receiver is preserved.
   * The returned quantity may be rounded internally.
   * Converting it back with `toLong(miles)` is not guaranteed to reproduce the receiver.
   *
   * @return A length quantity equal to `this * miles`.
   */
  val Long.miles: LengthQuantity get() = times(mile_unit)

  /**
   * Creates a [LengthQuantity] representing this numeric value in miles.
   *
   * ## Description
   *
   * The sign of the receiver is preserved.
   * The returned quantity may be rounded internally.
   * Converting it back with `toDouble(miles)` is not guaranteed to reproduce the receiver.
   *
   * @return A length quantity equal to `this * miles`.
   * @throws IllegalArgumentException
   * - If the receiver is NaN.
   * - If the receiver is positive infinity or negative infinity.
   */
  val Float.miles: LengthQuantity get() = times(mile_unit)

  /**
   * Creates a [LengthQuantity] representing this numeric value in miles.
   *
   * ## Description
   *
   * The sign of the receiver is preserved.
   * The returned quantity may be rounded internally.
   * Converting it back with `toDouble(miles)` is not guaranteed to reproduce the receiver.
   *
   * @return A length quantity equal to `this * miles`.
   * @throws IllegalArgumentException
   * - If the receiver is NaN.
   * - If the receiver is positive infinity or negative infinity.
   */
  val Double.miles: LengthQuantity get() = times(mile_unit)

  // endregion
  // region feet

  /**
   * Creates a [LengthQuantity] representing this numeric value in feet.
   *
   * ## Description
   *
   * The sign of the receiver is preserved.
   * The returned quantity may be rounded internally.
   * Converting it back with `toInt(feet)` is not guaranteed to reproduce the receiver.
   *
   * @return A length quantity equal to `this * feet`.
   */
  val Int.feet: LengthQuantity get() = times(feet_unit)

  /**
   * Creates a [LengthQuantity] representing this numeric value in feet.
   *
   * ## Description
   *
   * The sign of the receiver is preserved.
   * The returned quantity may be rounded internally.
   * Converting it back with `toLong(feet)` is not guaranteed to reproduce the receiver.
   *
   * @return A length quantity equal to `this * feet`.
   */
  val Long.feet: LengthQuantity get() = times(feet_unit)

  /**
   * Creates a [LengthQuantity] representing this numeric value in feet.
   *
   * ## Description
   *
   * The sign of the receiver is preserved.
   * The returned quantity may be rounded internally.
   * Converting it back with `toDouble(feet)` is not guaranteed to reproduce the receiver.
   *
   * @return A length quantity equal to `this * feet`.
   * @throws IllegalArgumentException
   * - If the receiver is NaN.
   * - If the receiver is positive infinity or negative infinity.
   */
  val Float.feet: LengthQuantity get() = times(feet_unit)

  /**
   * Creates a [LengthQuantity] representing this numeric value in feet.
   *
   * ## Description
   *
   * The sign of the receiver is preserved.
   * The returned quantity may be rounded internally.
   * Converting it back with `toDouble(feet)` is not guaranteed to reproduce the receiver.
   *
   * @return A length quantity equal to `this * feet`.
   * @throws IllegalArgumentException
   * - If the receiver is NaN.
   * - If the receiver is positive infinity or negative infinity.
   */
  val Double.feet: LengthQuantity get() = times(feet_unit)

  // endregion
  // region inches

  /**
   * Creates a [LengthQuantity] representing this numeric value in inches.
   *
   * ## Description
   *
   * The sign of the receiver is preserved.
   * The returned quantity may be rounded internally.
   * Converting it back with `toInt(inches)` is not guaranteed to reproduce the receiver.
   *
   * @return A length quantity equal to `this * inches`.
   */
  val Int.inches: LengthQuantity get() = times(inch_unit)

  /**
   * Creates a [LengthQuantity] representing this numeric value in inches.
   *
   * ## Description
   *
   * The sign of the receiver is preserved.
   * The returned quantity may be rounded internally.
   * Converting it back with `toLong(inches)` is not guaranteed to reproduce the receiver.
   *
   * @return A length quantity equal to `this * inches`.
   */
  val Long.inches: LengthQuantity get() = times(inch_unit)

  /**
   * Creates a [LengthQuantity] representing this numeric value in inches.
   *
   * ## Description
   *
   * The sign of the receiver is preserved.
   * The returned quantity may be rounded internally.
   * Converting it back with `toDouble(inches)` is not guaranteed to reproduce the receiver.
   *
   * @return A length quantity equal to `this * inches`.
   * @throws IllegalArgumentException
   * - If the receiver is NaN.
   * - If the receiver is positive infinity or negative infinity.
   */
  val Float.inches: LengthQuantity get() = times(inch_unit)

  /**
   * Creates a [LengthQuantity] representing this numeric value in inches.
   *
   * ## Description
   *
   * The sign of the receiver is preserved.
   * The returned quantity may be rounded internally.
   * Converting it back with `toDouble(inches)` is not guaranteed to reproduce the receiver.
   *
   * @return A length quantity equal to `this * inches`.
   * @throws IllegalArgumentException
   * - If the receiver is NaN.
   * - If the receiver is positive infinity or negative infinity.
   */
  val Double.inches: LengthQuantity get() = times(inch_unit)

  // endregion
  // endregion
}
