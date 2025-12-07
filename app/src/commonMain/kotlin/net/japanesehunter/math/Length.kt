package net.japanesehunter.math

import net.japanesehunter.math.Length.Companion.from
import kotlin.math.abs
import kotlin.math.roundToLong

private const val NANOMETERS_PER_MICROMETER: Long = 1_000L
private const val NANOMETERS_PER_MILLIMETER: Long = 1_000_000L
private const val NANOMETERS_PER_CENTIMETER: Long = 10_000_000L
private const val NANOMETERS_PER_METER: Long = 1_000_000_000L
private const val NANOMETERS_PER_KILOMETER: Long = 1_000_000_000_000L

/**
 * Represents the units supported by [Length].
 * Each unit defines how many nanometers it contains.
 *
 * @property nanometersPerUnit The number of nanometers in a single unit.
 * @property symbol The short unit suffix used by [Length.toString].
 */
enum class LengthUnit(
  internal val nanometersPerUnit: Long,
  internal val symbol: String,
) {
  /**
   * The nanometer unit (1e-9 meters).
   */
  NANOMETER(1L, "nm"),

  /**
   * The micrometer unit (1e-6 meters).
   */
  MICROMETER(NANOMETERS_PER_MICROMETER, "um"),

  /**
   * The millimeter unit (1e-3 meters).
   */
  MILLIMETER(NANOMETERS_PER_MILLIMETER, "mm"),

  /**
   * The centimeter unit (1e-2 meters).
   */
  CENTIMETER(NANOMETERS_PER_CENTIMETER, "cm"),

  /**
   * The meter unit.
   */
  METER(NANOMETERS_PER_METER, "m"),

  /**
   * The kilometer unit.
   */
  KILOMETER(NANOMETERS_PER_KILOMETER, "km"),
}

/**
 * Immutable distance value backed by a signed nanometer count in a [Long].
 * Precision is fixed to nanometers; [from] with [Double] rounds to the
 * nearest nanometer and rejects non-finite values.
 * Use [LengthUnit] to view or construct values in nanometers, micrometers,
 * millimeters, centimeters, meters, or kilometers.
 * Arithmetic methods signal overflow with [ArithmeticException].
 *
 * @author Int16
 */
value class Length internal constructor(
  private val nanometers: Long,
) : Comparable<Length> {
  /**
   * Returns this distance as a whole number of nanometers.
   */
  val inWholeNanometers: Long
    get() = nanometers

  /**
   * Returns this distance as a whole number of micrometers,
   * truncated toward zero.
   */
  val inWholeMicrometers: Long
    get() = nanometers / NANOMETERS_PER_MICROMETER

  /**
   * Returns this distance as a whole number of millimeters,
   * truncated toward zero.
   */
  val inWholeMillimeters: Long
    get() = nanometers / NANOMETERS_PER_MILLIMETER

  /**
   * Returns this distance as a whole number of centimeters,
   * truncated toward zero.
   */
  val inWholeCentimeters: Long
    get() = nanometers / NANOMETERS_PER_CENTIMETER

  /**
   * Returns this distance as a whole number of meters,
   * truncated toward zero.
   */
  val inWholeMeters: Long
    get() = nanometers / NANOMETERS_PER_METER

  /**
   * Returns this distance as a whole number of kilometers,
   * truncated toward zero.
   */
  val inWholeKilometers: Long
    get() = nanometers / NANOMETERS_PER_KILOMETER

  /**
   * Converts this [Length] to a [Long] value using the specified [unit].
   * The result is truncated toward zero.
   *
   * @param unit The [LengthUnit] to convert to.
   * @return The truncated [Long] representation in [unit].
   */
  fun toLong(unit: LengthUnit): Long =
    when (unit) {
      LengthUnit.NANOMETER -> inWholeNanometers
      LengthUnit.MICROMETER -> inWholeMicrometers
      LengthUnit.MILLIMETER -> inWholeMillimeters
      LengthUnit.CENTIMETER -> inWholeCentimeters
      LengthUnit.METER -> inWholeMeters
      LengthUnit.KILOMETER -> inWholeKilometers
    }

  /**
   * Converts this [Length] to a [Double] value using the specified [unit].
   *
   * @param unit The [LengthUnit] to convert to.
   * @return The [Double] representation in [unit].
   */
  fun toDouble(unit: LengthUnit): Double = nanometers.toDouble() / unit.nanometersPerUnit.toDouble()

  /**
   * Returns the absolute value of this distance.
   */
  val absoluteValue: Length
    get() =
      if (nanometers < 0) {
        Length(safeNegate(nanometers))
      } else {
        this
      }

  /**
   * Returns `true` if this distance is exactly zero.
   */
  val isZero: Boolean
    get() = nanometers == 0L

  /**
   * Returns `true` if this distance is strictly positive.
   */
  val isPositive: Boolean
    get() = nanometers > 0L

  /**
   * Returns `true` if this distance is strictly negative.
   */
  val isNegative: Boolean
    get() = nanometers < 0L

  /**
   * Returns the negated distance.
   *
   * @throws ArithmeticException If negation overflows [Long].
   */
  operator fun unaryMinus(): Length = Length(safeNegate(nanometers))

  /**
   * Adds another [Length].
   *
   * @param other The distance to add.
   * @return The sum of the distances.
   * @throws ArithmeticException If the sum overflows [Long].
   */
  operator fun plus(other: Length): Length = Length(safeAdd(nanometers, other.nanometers))

  /**
   * Subtracts another [Length].
   *
   * @param other The distance to subtract.
   * @return The difference of the distances.
   * @throws ArithmeticException If the subtraction overflows [Long].
   */
  operator fun minus(other: Length): Length = Length(safeAdd(nanometers, safeNegate(other.nanometers)))

  /**
   * Multiplies this distance by a [Long] factor.
   *
   * @param factor The scaling factor.
   * @return The scaled [Length].
   * @throws ArithmeticException If the multiplication overflows [Long].
   */
  operator fun times(factor: Long): Length = Length(safeMultiply(nanometers, factor))

  /**
   * Multiplies this distance by a [Double] factor.
   * The result is rounded to the nearest nanometer.
   *
   * @param factor The scaling factor. Must be finite.
   * @return The scaled [Length].
   * @throws IllegalArgumentException If [factor] is not finite.
   */
  operator fun times(factor: Double): Length = Length(scaleDouble(nanometers, factor))

  /**
   * Divides this distance by a [Long] divisor.
   *
   * @param divisor The divisor. Must not be zero.
   * @return The scaled [Length].
   * @throws IllegalArgumentException If [divisor] is zero.
   */
  operator fun div(divisor: Long): Length {
    require(divisor != 0L) { "Cannot divide a distance by zero." }
    return Length(nanometers / divisor)
  }

  /**
   * Divides this distance by a [Double] divisor.
   * The result is rounded to the nearest nanometer.
   *
   * @param divisor The divisor. Must be finite and non-zero.
   * @return The scaled [Length].
   * @throws IllegalArgumentException If [divisor] is not finite or zero.
   */
  operator fun div(divisor: Double): Length {
    require(divisor.isFinite() && divisor != 0.0) {
      "Divisor must be finite and non-zero: $divisor"
    }
    return Length(scaleDouble(nanometers, 1.0 / divisor))
  }

  /**
   * Divides this distance by another [Length], returning the ratio.
   *
   * @param other The divisor. Must not be zero.
   * @return The ratio as a [Double].
   * @throws IllegalArgumentException If [other] is zero.
   */
  operator fun div(other: Length): Double {
    require(other.nanometers != 0L) { "Cannot divide by a zero distance." }
    return nanometers.toDouble() / other.nanometers.toDouble()
  }

  override fun compareTo(other: Length): Int = nanometers.compareTo(other.nanometers)

  override fun toString(): String {
    val isNegative = nanometers < 0
    val absNanometers = absoluteNanometers(nanometers)
    val (value, unit) =
      when {
        absNanometers >= NANOMETERS_PER_KILOMETER -> {
          absNanometers.toDouble() / NANOMETERS_PER_KILOMETER to LengthUnit.KILOMETER
        }

        absNanometers >= NANOMETERS_PER_METER -> {
          absNanometers.toDouble() / NANOMETERS_PER_METER to LengthUnit.METER
        }

        absNanometers >= NANOMETERS_PER_CENTIMETER -> {
          absNanometers.toDouble() / NANOMETERS_PER_CENTIMETER to LengthUnit.CENTIMETER
        }

        absNanometers >= NANOMETERS_PER_MILLIMETER -> {
          absNanometers.toDouble() / NANOMETERS_PER_MILLIMETER to LengthUnit.MILLIMETER
        }

        absNanometers >= NANOMETERS_PER_MICROMETER -> {
          absNanometers.toDouble() / NANOMETERS_PER_MICROMETER to LengthUnit.MICROMETER
        }

        else -> {
          absNanometers.toDouble() to LengthUnit.NANOMETER
        }
      }
    val formatted = formatLengthValue(value)
    return if (isNegative) {
      "-$formatted${unit.symbol}"
    } else {
      "$formatted${unit.symbol}"
    }
  }

  companion object {
    /**
     * A distance of zero nanometers.
     */
    val ZERO: Length = Length(0L)

    /**
     * Creates a [Length] from a [Long] value with the specified [unit].
     *
     * @param value The magnitude expressed in [unit].
     * @param unit The [LengthUnit] describing the input value.
     * @return The created [Length].
     * @throws ArithmeticException If the conversion overflows [Long].
     */
    fun from(
      value: Long,
      unit: LengthUnit = LengthUnit.NANOMETER,
    ): Length = Length(unit.toNanometers(value))

    /**
     * Creates a [Length] from a [Double] value with the specified [unit].
     * The value is rounded to the nearest nanometer.
     *
     * @param value The magnitude expressed in [unit]. Must be finite.
     * @param unit The [LengthUnit] describing the input value.
     * @return The created [Length].
     * @throws IllegalArgumentException If [value] is not finite.
     */
    fun from(
      value: Double,
      unit: LengthUnit = LengthUnit.NANOMETER,
    ): Length = Length(unit.toNanometers(value))
  }
}

/**
 * Creates a [Length] from this [Long] value expressed in nanometers.
 */
val Long.nanometers: Length
  get() = Length.from(this, LengthUnit.NANOMETER)

/**
 * Creates a [Length] from this [Long] value expressed in micrometers.
 */
val Long.micrometers: Length
  get() = Length.from(this, LengthUnit.MICROMETER)

/**
 * Creates a [Length] from this [Long] value expressed in millimeters.
 */
val Long.millimeters: Length
  get() = Length.from(this, LengthUnit.MILLIMETER)

/**
 * Creates a [Length] from this [Long] value expressed in centimeters.
 */
val Long.centimeters: Length
  get() = Length.from(this, LengthUnit.CENTIMETER)

/**
 * Creates a [Length] from this [Long] value expressed in meters.
 */
val Long.meters: Length
  get() = Length.from(this, LengthUnit.METER)

/**
 * Creates a [Length] from this [Long] value expressed in kilometers.
 */
val Long.kilometers: Length
  get() = Length.from(this, LengthUnit.KILOMETER)

/**
 * Creates a [Length] from this [Double] value expressed in nanometers.
 * The value is rounded to the nearest nanometer.
 */
val Double.nanometers: Length
  get() = Length.from(this, LengthUnit.NANOMETER)

/**
 * Creates a [Length] from this [Double] value expressed in micrometers.
 * The value is rounded to the nearest nanometer.
 */
val Double.micrometers: Length
  get() = Length.from(this, LengthUnit.MICROMETER)

/**
 * Creates a [Length] from this [Double] value expressed in millimeters.
 * The value is rounded to the nearest nanometer.
 */
val Double.millimeters: Length
  get() = Length.from(this, LengthUnit.MILLIMETER)

/**
 * Creates a [Length] from this [Double] value expressed in centimeters.
 * The value is rounded to the nearest nanometer.
 */
val Double.centimeters: Length
  get() = Length.from(this, LengthUnit.CENTIMETER)

/**
 * Creates a [Length] from this [Double] value expressed in meters.
 * The value is rounded to the nearest nanometer.
 */
val Double.meters: Length
  get() = Length.from(this, LengthUnit.METER)

/**
 * Creates a [Length] from this [Double] value expressed in kilometers.
 * The value is rounded to the nearest nanometer.
 */
val Double.kilometers: Length
  get() = Length.from(this, LengthUnit.KILOMETER)

/**
 * Multiplies a [Double] by a [Length].
 *
 * @receiver The scaling factor. Must be finite.
 * @param distance The distance to scale.
 * @return The scaled [Length].
 */
operator fun Double.times(distance: Length): Length = distance * this

/**
 * Creates a [Length] from this [Int] value expressed in nanometers.
 */
val Int.nanometers: Length
  get() = toLong().nanometers

/**
 * Creates a [Length] from this [Int] value expressed in micrometers.
 */
val Int.micrometers: Length
  get() = toLong().micrometers

/**
 * Creates a [Length] from this [Int] value expressed in millimeters.
 */
val Int.millimeters: Length
  get() = toLong().millimeters

/**
 * Creates a [Length] from this [Int] value expressed in centimeters.
 */
val Int.centimeters: Length
  get() = toLong().centimeters

/**
 * Creates a [Length] from this [Int] value expressed in meters.
 */
val Int.meters: Length
  get() = toLong().meters

/**
 * Creates a [Length] from this [Int] value expressed in kilometers.
 */
val Int.kilometers: Length
  get() = toLong().kilometers

private fun LengthUnit.toNanometers(value: Long): Long =
  when (this) {
    LengthUnit.NANOMETER -> value
    LengthUnit.MICROMETER -> safeMultiply(value, NANOMETERS_PER_MICROMETER)
    LengthUnit.MILLIMETER -> safeMultiply(value, NANOMETERS_PER_MILLIMETER)
    LengthUnit.CENTIMETER -> safeMultiply(value, NANOMETERS_PER_CENTIMETER)
    LengthUnit.METER -> safeMultiply(value, NANOMETERS_PER_METER)
    LengthUnit.KILOMETER -> safeMultiply(value, NANOMETERS_PER_KILOMETER)
  }

private fun LengthUnit.toNanometers(value: Double): Long {
  require(value.isFinite()) { "Length must be finite: $value $this" }
  val scaled = value * nanometersPerUnit.toDouble()
  require(scaled.isFinite()) { "Converted distance is not finite: $scaled" }
  require(scaled <= Long.MAX_VALUE && scaled >= Long.MIN_VALUE) {
    "Length $value $this cannot be represented as nanometers."
  }
  return scaled.roundToLong()
}

private fun safeAdd(
  a: Long,
  b: Long,
): Long {
  val result = a + b
  if ((a xor result) and (b xor result) < 0) {
    throw ArithmeticException("Long overflow adding $a and $b.")
  }
  return result
}

private fun safeMultiply(
  a: Long,
  b: Long,
): Long {
  if (a == 0L || b == 0L) return 0L
  val result = a * b
  if (result / b != a) {
    throw ArithmeticException("Long overflow multiplying $a and $b.")
  }
  return result
}

private fun safeNegate(value: Long): Long =
  if (value == Long.MIN_VALUE) {
    throw ArithmeticException("Long overflow negating $value.")
  } else {
    -value
  }

private fun scaleDouble(
  value: Long,
  factor: Double,
): Long {
  require(factor.isFinite()) { "Scale must be finite: $factor" }
  val scaled = value.toDouble() * factor
  require(scaled.isFinite()) { "Scale produced a non-finite distance: $scaled" }
  require(scaled <= Long.MAX_VALUE && scaled >= Long.MIN_VALUE) {
    "Scaled distance does not fit in Long: $scaled"
  }
  return scaled.roundToLong()
}

private fun absoluteNanometers(value: Long): Long =
  if (value == Long.MIN_VALUE) {
    Long.MAX_VALUE
  } else {
    abs(value)
  }

private fun formatLengthValue(value: Double): String {
  val rounded = value.roundToLong()
  return if (rounded.toDouble() == value) {
    rounded.toString()
  } else {
    value.toString()
  }
}
