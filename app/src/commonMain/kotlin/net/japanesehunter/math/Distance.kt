package net.japanesehunter.math

import net.japanesehunter.math.Distance.Companion.from
import kotlin.math.abs
import kotlin.math.roundToLong

private const val NANOMETERS_PER_MICROMETER: Long = 1_000L
private const val NANOMETERS_PER_MILLIMETER: Long = 1_000_000L
private const val NANOMETERS_PER_METER: Long = 1_000_000_000L
private const val NANOMETERS_PER_KILOMETER: Long = 1_000_000_000_000L

/**
 * Represents the units supported by [Distance].
 * Each unit defines how many nanometers it contains.
 *
 * @property nanometersPerUnit The number of nanometers in a single unit.
 * @property symbol The short unit suffix used by [Distance.toString].
 */
enum class DistanceUnit(
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
 * Use [DistanceUnit] to view or construct values in nanometers, micrometers,
 * millimeters, meters, or kilometers.
 * Arithmetic methods signal overflow with [ArithmeticException].
 *
 * @author Int16
 */
value class Distance internal constructor(
  private val nanometers: Long,
) : Comparable<Distance> {
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
   * Converts this [Distance] to a [Long] value using the specified [unit].
   * The result is truncated toward zero.
   *
   * @param unit The [DistanceUnit] to convert to.
   * @return The truncated [Long] representation in [unit].
   */
  fun toLong(unit: DistanceUnit): Long =
    when (unit) {
      DistanceUnit.NANOMETER -> inWholeNanometers
      DistanceUnit.MICROMETER -> inWholeMicrometers
      DistanceUnit.MILLIMETER -> inWholeMillimeters
      DistanceUnit.METER -> inWholeMeters
      DistanceUnit.KILOMETER -> inWholeKilometers
    }

  /**
   * Converts this [Distance] to a [Double] value using the specified [unit].
   *
   * @param unit The [DistanceUnit] to convert to.
   * @return The [Double] representation in [unit].
   */
  fun toDouble(unit: DistanceUnit): Double = nanometers.toDouble() / unit.nanometersPerUnit.toDouble()

  /**
   * Returns the absolute value of this distance.
   */
  val absoluteValue: Distance
    get() =
      if (nanometers < 0) {
        Distance(safeNegate(nanometers))
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
  operator fun unaryMinus(): Distance = Distance(safeNegate(nanometers))

  /**
   * Adds another [Distance].
   *
   * @param other The distance to add.
   * @return The sum of the distances.
   * @throws ArithmeticException If the sum overflows [Long].
   */
  operator fun plus(other: Distance): Distance = Distance(safeAdd(nanometers, other.nanometers))

  /**
   * Subtracts another [Distance].
   *
   * @param other The distance to subtract.
   * @return The difference of the distances.
   * @throws ArithmeticException If the subtraction overflows [Long].
   */
  operator fun minus(other: Distance): Distance = Distance(safeAdd(nanometers, safeNegate(other.nanometers)))

  /**
   * Multiplies this distance by a [Long] factor.
   *
   * @param factor The scaling factor.
   * @return The scaled [Distance].
   * @throws ArithmeticException If the multiplication overflows [Long].
   */
  operator fun times(factor: Long): Distance = Distance(safeMultiply(nanometers, factor))

  /**
   * Multiplies this distance by a [Double] factor.
   * The result is rounded to the nearest nanometer.
   *
   * @param factor The scaling factor. Must be finite.
   * @return The scaled [Distance].
   * @throws IllegalArgumentException If [factor] is not finite.
   */
  operator fun times(factor: Double): Distance = Distance(scaleDouble(nanometers, factor))

  /**
   * Divides this distance by a [Long] divisor.
   *
   * @param divisor The divisor. Must not be zero.
   * @return The scaled [Distance].
   * @throws IllegalArgumentException If [divisor] is zero.
   */
  operator fun div(divisor: Long): Distance {
    require(divisor != 0L) { "Cannot divide a distance by zero." }
    return Distance(nanometers / divisor)
  }

  /**
   * Divides this distance by a [Double] divisor.
   * The result is rounded to the nearest nanometer.
   *
   * @param divisor The divisor. Must be finite and non-zero.
   * @return The scaled [Distance].
   * @throws IllegalArgumentException If [divisor] is not finite or zero.
   */
  operator fun div(divisor: Double): Distance {
    require(divisor.isFinite() && divisor != 0.0) {
      "Divisor must be finite and non-zero: $divisor"
    }
    return Distance(scaleDouble(nanometers, 1.0 / divisor))
  }

  /**
   * Divides this distance by another [Distance], returning the ratio.
   *
   * @param other The divisor. Must not be zero.
   * @return The ratio as a [Double].
   * @throws IllegalArgumentException If [other] is zero.
   */
  operator fun div(other: Distance): Double {
    require(other.nanometers != 0L) { "Cannot divide by a zero distance." }
    return nanometers.toDouble() / other.nanometers.toDouble()
  }

  override fun compareTo(other: Distance): Int = nanometers.compareTo(other.nanometers)

  override fun toString(): String {
    val isNegative = nanometers < 0
    val absNanometers = absoluteNanometers(nanometers)
    val (value, unit) =
      when {
        absNanometers >= NANOMETERS_PER_KILOMETER -> {
          absNanometers.toDouble() / NANOMETERS_PER_KILOMETER to DistanceUnit.KILOMETER
        }

        absNanometers >= NANOMETERS_PER_METER -> {
          absNanometers.toDouble() / NANOMETERS_PER_METER to DistanceUnit.METER
        }

        absNanometers >= NANOMETERS_PER_MILLIMETER -> {
          absNanometers.toDouble() / NANOMETERS_PER_MILLIMETER to DistanceUnit.MILLIMETER
        }

        absNanometers >= NANOMETERS_PER_MICROMETER -> {
          absNanometers.toDouble() / NANOMETERS_PER_MICROMETER to DistanceUnit.MICROMETER
        }

        else -> {
          absNanometers.toDouble() to DistanceUnit.NANOMETER
        }
      }
    val formatted = formatDistanceValue(value)
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
    val ZERO: Distance = Distance(0L)

    /**
     * Creates a [Distance] from a [Long] value with the specified [unit].
     *
     * @param value The magnitude expressed in [unit].
     * @param unit The [DistanceUnit] describing the input value.
     * @return The created [Distance].
     * @throws ArithmeticException If the conversion overflows [Long].
     */
    fun from(
      value: Long,
      unit: DistanceUnit = DistanceUnit.NANOMETER,
    ): Distance = Distance(unit.toNanometers(value))

    /**
     * Creates a [Distance] from a [Double] value with the specified [unit].
     * The value is rounded to the nearest nanometer.
     *
     * @param value The magnitude expressed in [unit]. Must be finite.
     * @param unit The [DistanceUnit] describing the input value.
     * @return The created [Distance].
     * @throws IllegalArgumentException If [value] is not finite.
     */
    fun from(
      value: Double,
      unit: DistanceUnit = DistanceUnit.NANOMETER,
    ): Distance = Distance(unit.toNanometers(value))
  }
}

/**
 * Creates a [Distance] from this [Long] value expressed in nanometers.
 */
val Long.nanometers: Distance
  get() = Distance.from(this, DistanceUnit.NANOMETER)

/**
 * Creates a [Distance] from this [Long] value expressed in micrometers.
 */
val Long.micrometers: Distance
  get() = Distance.from(this, DistanceUnit.MICROMETER)

/**
 * Creates a [Distance] from this [Long] value expressed in millimeters.
 */
val Long.millimeters: Distance
  get() = Distance.from(this, DistanceUnit.MILLIMETER)

/**
 * Creates a [Distance] from this [Long] value expressed in meters.
 */
val Long.meters: Distance
  get() = Distance.from(this, DistanceUnit.METER)

/**
 * Creates a [Distance] from this [Long] value expressed in kilometers.
 */
val Long.kilometers: Distance
  get() = Distance.from(this, DistanceUnit.KILOMETER)

/**
 * Creates a [Distance] from this [Double] value expressed in nanometers.
 * The value is rounded to the nearest nanometer.
 */
val Double.nanometers: Distance
  get() = Distance.from(this, DistanceUnit.NANOMETER)

/**
 * Creates a [Distance] from this [Double] value expressed in micrometers.
 * The value is rounded to the nearest nanometer.
 */
val Double.micrometers: Distance
  get() = Distance.from(this, DistanceUnit.MICROMETER)

/**
 * Creates a [Distance] from this [Double] value expressed in millimeters.
 * The value is rounded to the nearest nanometer.
 */
val Double.millimeters: Distance
  get() = Distance.from(this, DistanceUnit.MILLIMETER)

/**
 * Creates a [Distance] from this [Double] value expressed in meters.
 * The value is rounded to the nearest nanometer.
 */
val Double.meters: Distance
  get() = Distance.from(this, DistanceUnit.METER)

/**
 * Creates a [Distance] from this [Double] value expressed in kilometers.
 * The value is rounded to the nearest nanometer.
 */
val Double.kilometers: Distance
  get() = Distance.from(this, DistanceUnit.KILOMETER)

/**
 * Multiplies a [Double] by a [Distance].
 *
 * @receiver The scaling factor. Must be finite.
 * @param distance The distance to scale.
 * @return The scaled [Distance].
 */
operator fun Double.times(distance: Distance): Distance = distance * this

private fun DistanceUnit.toNanometers(value: Long): Long =
  when (this) {
    DistanceUnit.NANOMETER -> value
    DistanceUnit.MICROMETER -> safeMultiply(value, NANOMETERS_PER_MICROMETER)
    DistanceUnit.MILLIMETER -> safeMultiply(value, NANOMETERS_PER_MILLIMETER)
    DistanceUnit.METER -> safeMultiply(value, NANOMETERS_PER_METER)
    DistanceUnit.KILOMETER -> safeMultiply(value, NANOMETERS_PER_KILOMETER)
  }

private fun DistanceUnit.toNanometers(value: Double): Long {
  require(value.isFinite()) { "Distance must be finite: $value $this" }
  val scaled = value * nanometersPerUnit.toDouble()
  require(scaled.isFinite()) { "Converted distance is not finite: $scaled" }
  require(scaled <= Long.MAX_VALUE && scaled >= Long.MIN_VALUE) {
    "Distance $value $this cannot be represented as nanometers."
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

private fun formatDistanceValue(value: Double): String {
  val rounded = value.roundToLong()
  return if (rounded.toDouble() == value) {
    rounded.toString()
  } else {
    value.toString()
  }
}
