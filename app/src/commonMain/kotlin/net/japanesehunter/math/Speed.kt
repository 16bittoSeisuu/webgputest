package net.japanesehunter.math

import net.japanesehunter.math.Speed.Companion.from
import kotlin.math.abs
import kotlin.math.roundToLong
import kotlin.time.Duration

private const val NANOMETERS_PER_SECOND_PER_METER_PER_SECOND: Long =
  1_000_000_000L
private const val NANOMETERS_PER_SECOND_PER_KILOMETER_PER_HOUR: Long =
  277_777_778L

/**
 * Represents the units supported by [Speed].
 * Each unit defines how many nanometers per second it contains.
 *
 * @property nanometersPerSecondPerUnit The number of nanometers per second in a single unit.
 * @property symbol The short unit suffix used by [Speed.toString].
 */
enum class SpeedUnit(
  internal val nanometersPerSecondPerUnit: Long,
  internal val symbol: String,
) {
  /**
   * The nanometer per second unit.
   */
  NanometerPerSecond(1L, "nm/s"),

  /**
   * The meter per second unit.
   */
  MeterPerSecond(NANOMETERS_PER_SECOND_PER_METER_PER_SECOND, "m/s"),

  /**
   * The kilometer per hour unit.
   */
  KilometerPerHour(NANOMETERS_PER_SECOND_PER_KILOMETER_PER_HOUR, "km/h"),
}

/**
 * Immutable speed value backed by a signed nanometers-per-second count in a [Long].
 * Precision is fixed to nanometers per second; [from] with [Double] rounds to the
 * nearest nanometer per second and rejects non-finite values.
 * Use [SpeedUnit] to view or construct values in meters per second or kilometers per hour.
 * Arithmetic methods signal overflow with [ArithmeticException].
 *
 * @author Int16
 */
value class Speed internal constructor(
  private val nanometersPerSecond: Long,
) : Comparable<Speed> {
  /**
   * Returns this speed as a whole number of nanometers per second.
   */
  val inWholeNanometersPerSecond: Long
    get() = nanometersPerSecond

  /**
   * Returns this speed as a whole number of meters per second,
   * truncated toward zero.
   */
  val inWholeMetersPerSecond: Long
    get() = nanometersPerSecond / NANOMETERS_PER_SECOND_PER_METER_PER_SECOND

  /**
   * Converts this [Speed] to a [Long] value using the specified [unit].
   * The result is truncated toward zero.
   *
   * @param unit The [SpeedUnit] to convert to.
   * @return The truncated [Long] representation in [unit].
   */
  fun toLong(
    unit: SpeedUnit,
  ): Long =
    when (unit) {
      SpeedUnit.NanometerPerSecond -> {
        nanometersPerSecond
      }

      SpeedUnit.MeterPerSecond -> {
        inWholeMetersPerSecond
      }

      SpeedUnit.KilometerPerHour -> {
        nanometersPerSecond /
          NANOMETERS_PER_SECOND_PER_KILOMETER_PER_HOUR
      }
    }

  /**
   * Converts this [Speed] to a [Double] value using the specified [unit].
   *
   * @param unit The [SpeedUnit] to convert to.
   * @return The [Double] representation in [unit].
   */
  fun toDouble(
    unit: SpeedUnit,
  ): Double =
    nanometersPerSecond.toDouble() /
      unit.nanometersPerSecondPerUnit
        .toDouble()

  /**
   * Returns the absolute value of this speed.
   */
  val absoluteValue: Speed
    get() =
      if (nanometersPerSecond < 0) {
        Speed(safeNegate(nanometersPerSecond))
      } else {
        this
      }

  /**
   * Returns `true` if this speed is exactly zero.
   */
  val isZero: Boolean
    get() = nanometersPerSecond == 0L

  /**
   * Returns `true` if this speed is strictly positive.
   */
  val isPositive: Boolean
    get() = nanometersPerSecond > 0L

  /**
   * Returns `true` if this speed is strictly negative.
   */
  val isNegative: Boolean
    get() = nanometersPerSecond < 0L

  /**
   * Returns the negated speed.
   *
   * @throws ArithmeticException If negation overflows [Long].
   */
  operator fun unaryMinus(): Speed =
    Speed(safeNegate(nanometersPerSecond))

  /**
   * Adds another [Speed].
   *
   * @param other The speed to add.
   * @return The sum of the speeds.
   * @throws ArithmeticException If the sum overflows [Long].
   */
  operator fun plus(
    other: Speed,
  ): Speed =
    Speed(safeAdd(nanometersPerSecond, other.nanometersPerSecond))

  /**
   * Subtracts another [Speed].
   *
   * @param other The speed to subtract.
   * @return The difference of the speeds.
   * @throws ArithmeticException If the subtraction overflows [Long].
   */
  operator fun minus(
    other: Speed,
  ): Speed =
    Speed(safeAdd(nanometersPerSecond, safeNegate(other.nanometersPerSecond)))

  /**
   * Multiplies this speed by a [Long] factor.
   *
   * @param factor The scaling factor.
   * @return The scaled [Speed].
   * @throws ArithmeticException If the multiplication overflows [Long].
   */
  operator fun times(
    factor: Long,
  ): Speed =
    Speed(safeMultiply(nanometersPerSecond, factor))

  /**
   * Multiplies this speed by a [Double] factor.
   * The result is rounded to the nearest nanometer per second.
   *
   * @param factor The scaling factor.
   * @return The scaled [Speed].
   * @throws ArithmeticException If the result overflows [Long].
   * @throws IllegalArgumentException If [factor] is not finite.
   */
  operator fun times(
    factor: Double,
  ): Speed {
    require(factor.isFinite()) { "factor must be finite: $factor" }
    val result = (nanometersPerSecond.toDouble() * factor).roundToLong()
    return Speed(result)
  }

  /**
   * Multiplies this speed by a [Duration] to produce a [Length].
   *
   * @param duration The time duration.
   * @return The resulting distance.
   * @throws ArithmeticException If the multiplication overflows [Long].
   */
  operator fun times(
    duration: Duration,
  ): Length {
    val seconds = duration.inWholeSeconds
    val nanos = (duration.inWholeNanoseconds % 1_000_000_000L)
    val wholeSecondDistance = safeMultiply(nanometersPerSecond, seconds)
    val nanoSecondDistance =
      (
        nanometersPerSecond.toDouble() *
          nanos.toDouble() /
          1_000_000_000.0
      ).roundToLong()
    return Length.from(
      safeAdd(wholeSecondDistance, nanoSecondDistance),
      LengthUnit.NANOMETER,
    )
  }

  /**
   * Divides this speed by a [Long] divisor.
   *
   * @param divisor The divisor.
   * @return The divided [Speed].
   * @throws ArithmeticException If [divisor] is zero.
   */
  operator fun div(
    divisor: Long,
  ): Speed {
    require(divisor != 0L) { "divisor must not be zero" }
    return Speed(nanometersPerSecond / divisor)
  }

  /**
   * Divides this speed by a [Double] divisor.
   * The result is rounded to the nearest nanometer per second.
   *
   * @param divisor The divisor.
   * @return The divided [Speed].
   * @throws ArithmeticException If the result overflows [Long].
   * @throws IllegalArgumentException If [divisor] is not finite or is zero.
   */
  operator fun div(
    divisor: Double,
  ): Speed {
    require(divisor.isFinite()) { "divisor must be finite: $divisor" }
    require(divisor != 0.0) { "divisor must not be zero" }
    val result = (nanometersPerSecond.toDouble() / divisor).roundToLong()
    return Speed(result)
  }

  /**
   * Divides this speed by another [Speed] to produce a dimensionless ratio.
   *
   * @param other The divisor speed.
   * @return The ratio as a [Double].
   * @throws ArithmeticException If [other] is zero.
   */
  operator fun div(
    other: Speed,
  ): Double {
    require(other.nanometersPerSecond != 0L) { "other must not be zero" }
    return nanometersPerSecond.toDouble() /
      other.nanometersPerSecond
        .toDouble()
  }

  override fun compareTo(
    other: Speed,
  ): Int =
    nanometersPerSecond.compareTo(other.nanometersPerSecond)

  override fun toString(): String {
    val value = toDouble(SpeedUnit.MeterPerSecond)
    return "$value ${SpeedUnit.MeterPerSecond.symbol}"
  }

  /**
   * Formats this speed as a string with the specified unit and decimal places.
   *
   * @param unit The unit to display the value in.
   *
   *   null: uses meters per second
   * @param decimals The number of decimal places.
   *
   *   null: uses unlimited precision
   *   range: decimals >= 0
   * @param signMode The sign display mode.
   * @return A formatted string representation.
   */
  fun toString(
    unit: SpeedUnit?,
    decimals: Int? = 2,
    signMode: SignMode = SignMode.Always,
  ): String {
    require(decimals == null || decimals >= 0) {
      "decimals must be non-negative: $decimals"
    }
    val resolvedUnit = unit ?: SpeedUnit.MeterPerSecond
    val isNegative = nanometersPerSecond < 0
    val absValue = abs(toDouble(resolvedUnit))
    val formatted =
      if (decimals !=
        null
      ) {
        formatDecimals(absValue, decimals)
      } else {
        absValue.toString()
      }
    return "${signMode.prefix(isNegative)}$formatted ${resolvedUnit.symbol}"
  }

  companion object {
    /**
     * The zero speed.
     */
    val ZERO: Speed = Speed(0L)

    /**
     * Creates a [Speed] from a [Long] value and a [SpeedUnit].
     *
     * @param value The numeric value in the specified unit.
     * @param unit The unit of [value].
     * @return A new [Speed] instance.
     * @throws ArithmeticException If the conversion overflows [Long].
     */
    fun from(
      value: Long,
      unit: SpeedUnit,
    ): Speed =
      Speed(safeMultiply(value, unit.nanometersPerSecondPerUnit))

    /**
     * Creates a [Speed] from a [Double] value and a [SpeedUnit].
     * The value is rounded to the nearest nanometer per second.
     *
     * @param value The numeric value in the specified unit.
     * @param unit The unit of [value].
     * @return A new [Speed] instance.
     * @throws ArithmeticException If the conversion overflows [Long].
     * @throws IllegalArgumentException If [value] is not finite.
     */
    fun from(
      value: Double,
      unit: SpeedUnit,
    ): Speed {
      require(value.isFinite()) { "value must be finite: $value" }
      val nanometersPerSecond =
        (
          value *
            unit.nanometersPerSecondPerUnit
              .toDouble()
        ).roundToLong()
      return Speed(nanometersPerSecond)
    }
  }
}

/**
 * Creates a [Speed] from this [Double] value in meters per second.
 *
 * @return A new [Speed] instance.
 * @throws ArithmeticException If the conversion overflows [Long].
 * @throws IllegalArgumentException If this value is not finite.
 */
val Double.metersPerSecond: Speed
  get() = from(this, SpeedUnit.MeterPerSecond)

/**
 * Creates a [Speed] from this [Long] value in meters per second.
 *
 * @return A new [Speed] instance.
 * @throws ArithmeticException If the conversion overflows [Long].
 */
val Long.metersPerSecond: Speed
  get() = from(this, SpeedUnit.MeterPerSecond)

/**
 * Creates a [Speed] from this [Int] value in meters per second.
 *
 * @return A new [Speed] instance.
 * @throws ArithmeticException If the conversion overflows [Long].
 */
val Int.metersPerSecond: Speed
  get() = from(this.toLong(), SpeedUnit.MeterPerSecond)

/**
 * Creates a [Speed] from this [Double] value in kilometers per hour.
 *
 * @return A new [Speed] instance.
 * @throws ArithmeticException If the conversion overflows [Long].
 * @throws IllegalArgumentException If this value is not finite.
 */
val Double.kilometersPerHour: Speed
  get() = from(this, SpeedUnit.KilometerPerHour)

/**
 * Creates a [Speed] from this [Long] value in kilometers per hour.
 *
 * @return A new [Speed] instance.
 * @throws ArithmeticException If the conversion overflows [Long].
 */
val Long.kilometersPerHour: Speed
  get() = from(this, SpeedUnit.KilometerPerHour)

/**
 * Creates a [Speed] from this [Int] value in kilometers per hour.
 *
 * @return A new [Speed] instance.
 * @throws ArithmeticException If the conversion overflows [Long].
 */
val Int.kilometersPerHour: Speed
  get() = from(this.toLong(), SpeedUnit.KilometerPerHour)

/**
 * Divides this [Length] by a [Duration] to produce a [Speed].
 *
 * @param duration The time duration.
 * @return The resulting speed.
 * @throws ArithmeticException If [duration] is zero or if the result overflows [Long].
 */
operator fun Length.div(
  duration: Duration,
): Speed {
  val totalNanos = duration.inWholeNanoseconds
  require(totalNanos != 0L) { "duration must not be zero" }
  val nanometersPerSecond =
    (
      this.inWholeNanometers
        .toDouble() *
        1_000_000_000.0 /
        totalNanos.toDouble()
    ).roundToLong()
  return Speed.from(nanometersPerSecond, SpeedUnit.MeterPerSecond)
}

/**
 * Multiplies this [Duration] by a [Speed] to produce a [Length].
 *
 * @param speed The speed value.
 * @return The resulting distance.
 * @throws ArithmeticException If the multiplication overflows [Long].
 */
operator fun Duration.times(
  speed: Speed,
): Length =
  speed * this

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

private fun safeNegate(
  value: Long,
): Long =
  if (value == Long.MIN_VALUE) {
    throw ArithmeticException("Long overflow negating $value.")
  } else {
    -value
  }
