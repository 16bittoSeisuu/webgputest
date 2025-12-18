package net.japanesehunter.math

import net.japanesehunter.math.Acceleration.Companion.from
import kotlin.math.abs
import kotlin.math.roundToLong
import kotlin.time.Duration

private const val NANOMETERS_PER_SECOND_SQUARED_PER_METER_PER_SECOND_SQUARED: Long = 1_000_000_000L

/**
 * Represents the units supported by [Acceleration].
 * Each unit defines how many nanometers per second squared it contains.
 *
 * @property nanometersPerSecondSquaredPerUnit The number of nanometers per second squared in a single unit.
 * @property symbol The short unit suffix used by [Acceleration.toString].
 */
enum class AccelerationUnit(
  internal val nanometersPerSecondSquaredPerUnit: Long,
  internal val symbol: String,
) {
  /**
   * The nanometer per second squared unit.
   */
  NANOMETER_PER_SECOND_SQUARED(1L, "nm/s²"),

  /**
   * The meter per second squared unit.
   */
  METER_PER_SECOND_SQUARED(NANOMETERS_PER_SECOND_SQUARED_PER_METER_PER_SECOND_SQUARED, "m/s²"),
}

/**
 * Immutable acceleration value backed by a signed nanometers-per-second-squared count in a [Long].
 * Precision is fixed to nanometers per second squared; [from] with [Double] rounds to the
 * nearest nanometer per second squared and rejects non-finite values.
 * Use [AccelerationUnit] to view or construct values in meters per second squared.
 * Arithmetic methods signal overflow with [ArithmeticException].
 *
 * @author Int16
 */
value class Acceleration internal constructor(
  private val nanometersPerSecondSquared: Long,
) : Comparable<Acceleration> {
  /**
   * Returns this acceleration as a whole number of nanometers per second squared.
   */
  val inWholeNanometersPerSecondSquared: Long
    get() = nanometersPerSecondSquared

  /**
   * Returns this acceleration as a whole number of meters per second squared,
   * truncated toward zero.
   */
  val inWholeMetersPerSecondSquared: Long
    get() = nanometersPerSecondSquared / NANOMETERS_PER_SECOND_SQUARED_PER_METER_PER_SECOND_SQUARED

  /**
   * Converts this [Acceleration] to a [Long] value using the specified [unit].
   * The result is truncated toward zero.
   *
   * @param unit The [AccelerationUnit] to convert to.
   * @return The truncated [Long] representation in [unit].
   */
  fun toLong(unit: AccelerationUnit): Long =
    when (unit) {
      AccelerationUnit.NANOMETER_PER_SECOND_SQUARED -> inWholeNanometersPerSecondSquared
      AccelerationUnit.METER_PER_SECOND_SQUARED -> inWholeMetersPerSecondSquared
    }

  /**
   * Converts this [Acceleration] to a [Double] value using the specified [unit].
   *
   * @param unit The [AccelerationUnit] to convert to.
   * @return The [Double] representation in [unit].
   */
  fun toDouble(unit: AccelerationUnit): Double = nanometersPerSecondSquared.toDouble() / unit.nanometersPerSecondSquaredPerUnit.toDouble()

  /**
   * Returns the absolute value of this acceleration.
   */
  val absoluteValue: Acceleration
    get() =
      if (nanometersPerSecondSquared < 0) {
        Acceleration(safeNegate(nanometersPerSecondSquared))
      } else {
        this
      }

  /**
   * Returns `true` if this acceleration is exactly zero.
   */
  val isZero: Boolean
    get() = nanometersPerSecondSquared == 0L

  /**
   * Returns `true` if this acceleration is strictly positive.
   */
  val isPositive: Boolean
    get() = nanometersPerSecondSquared > 0L

  /**
   * Returns `true` if this acceleration is strictly negative.
   */
  val isNegative: Boolean
    get() = nanometersPerSecondSquared < 0L

  /**
   * Returns the negated acceleration.
   *
   * @throws ArithmeticException If negation overflows [Long].
   */
  operator fun unaryMinus(): Acceleration = Acceleration(safeNegate(nanometersPerSecondSquared))

  /**
   * Adds another [Acceleration].
   *
   * @param other The acceleration to add.
   * @return The sum of the accelerations.
   * @throws ArithmeticException If the sum overflows [Long].
   */
  operator fun plus(other: Acceleration): Acceleration = Acceleration(safeAdd(nanometersPerSecondSquared, other.nanometersPerSecondSquared))

  /**
   * Subtracts another [Acceleration].
   *
   * @param other The acceleration to subtract.
   * @return The difference of the accelerations.
   * @throws ArithmeticException If the subtraction overflows [Long].
   */
  operator fun minus(other: Acceleration): Acceleration =
    Acceleration(safeAdd(nanometersPerSecondSquared, safeNegate(other.nanometersPerSecondSquared)))

  /**
   * Multiplies this acceleration by a [Long] factor.
   *
   * @param factor The scaling factor.
   * @return The scaled [Acceleration].
   * @throws ArithmeticException If the multiplication overflows [Long].
   */
  operator fun times(factor: Long): Acceleration = Acceleration(safeMultiply(nanometersPerSecondSquared, factor))

  /**
   * Multiplies this acceleration by a [Double] factor.
   * The result is rounded to the nearest nanometer per second squared.
   *
   * @param factor The scaling factor.
   * @return The scaled [Acceleration].
   * @throws ArithmeticException If the result overflows [Long].
   * @throws IllegalArgumentException If [factor] is not finite.
   */
  operator fun times(factor: Double): Acceleration {
    require(factor.isFinite()) { "factor must be finite: $factor" }
    val result = (nanometersPerSecondSquared.toDouble() * factor).roundToLong()
    return Acceleration(result)
  }

  /**
   * Multiplies this acceleration by a [Duration] to produce a [Speed].
   *
   * @param duration The time duration.
   * @return The resulting speed.
   * @throws ArithmeticException If the multiplication overflows [Long].
   */
  operator fun times(duration: Duration): Speed {
    val seconds = duration.inWholeSeconds
    val nanos = (duration.inWholeNanoseconds % 1_000_000_000L)
    val wholeSecondSpeed = safeMultiply(nanometersPerSecondSquared, seconds)
    val nanoSecondSpeed = (nanometersPerSecondSquared.toDouble() * nanos.toDouble() / 1_000_000_000.0).roundToLong()
    return Speed.from(safeAdd(wholeSecondSpeed, nanoSecondSpeed), SpeedUnit.NANOMETER_PER_SECOND)
  }

  /**
   * Divides this acceleration by a [Long] divisor.
   *
   * @param divisor The divisor.
   * @return The divided [Acceleration].
   * @throws ArithmeticException If [divisor] is zero.
   */
  operator fun div(divisor: Long): Acceleration {
    require(divisor != 0L) { "divisor must not be zero" }
    return Acceleration(nanometersPerSecondSquared / divisor)
  }

  /**
   * Divides this acceleration by a [Double] divisor.
   * The result is rounded to the nearest nanometer per second squared.
   *
   * @param divisor The divisor.
   * @return The divided [Acceleration].
   * @throws ArithmeticException If the result overflows [Long].
   * @throws IllegalArgumentException If [divisor] is not finite or is zero.
   */
  operator fun div(divisor: Double): Acceleration {
    require(divisor.isFinite()) { "divisor must be finite: $divisor" }
    require(divisor != 0.0) { "divisor must not be zero" }
    val result = (nanometersPerSecondSquared.toDouble() / divisor).roundToLong()
    return Acceleration(result)
  }

  /**
   * Divides this acceleration by another [Acceleration] to produce a dimensionless ratio.
   *
   * @param other The divisor acceleration.
   * @return The ratio as a [Double].
   * @throws ArithmeticException If [other] is zero.
   */
  operator fun div(other: Acceleration): Double {
    require(other.nanometersPerSecondSquared != 0L) { "other must not be zero" }
    return nanometersPerSecondSquared.toDouble() / other.nanometersPerSecondSquared.toDouble()
  }

  override fun compareTo(other: Acceleration): Int = nanometersPerSecondSquared.compareTo(other.nanometersPerSecondSquared)

  override fun toString(): String {
    val value = toDouble(AccelerationUnit.METER_PER_SECOND_SQUARED)
    return "$value ${AccelerationUnit.METER_PER_SECOND_SQUARED.symbol}"
  }

  /**
   * Formats this acceleration as a string with the specified unit and decimal places.
   *
   * @param unit The unit to display the value in.
   *
   *   null: uses meters per second squared
   * @param decimals The number of decimal places.
   *
   *   null: uses unlimited precision
   *   range: decimals >= 0
   * @param signMode The sign display mode.
   * @return A formatted string representation.
   */
  fun toString(
    unit: AccelerationUnit?,
    decimals: Int? = 2,
    signMode: SignMode = SignMode.Always,
  ): String {
    require(decimals == null || decimals >= 0) { "decimals must be non-negative: $decimals" }
    val resolvedUnit = unit ?: AccelerationUnit.METER_PER_SECOND_SQUARED
    val isNegative = nanometersPerSecondSquared < 0
    val absValue = abs(toDouble(resolvedUnit))
    val formatted = if (decimals != null) formatDecimals(absValue, decimals) else absValue.toString()
    return "${signMode.prefix(isNegative)}$formatted ${resolvedUnit.symbol}"
  }

  companion object {
    /**
     * The zero acceleration.
     */
    val ZERO: Acceleration = Acceleration(0L)

    /**
     * Creates an [Acceleration] from a [Long] value and an [AccelerationUnit].
     *
     * @param value The numeric value in the specified unit.
     * @param unit The unit of [value].
     * @return A new [Acceleration] instance.
     * @throws ArithmeticException If the conversion overflows [Long].
     */
    fun from(
      value: Long,
      unit: AccelerationUnit,
    ): Acceleration = Acceleration(safeMultiply(value, unit.nanometersPerSecondSquaredPerUnit))

    /**
     * Creates an [Acceleration] from a [Double] value and an [AccelerationUnit].
     * The value is rounded to the nearest nanometer per second squared.
     *
     * @param value The numeric value in the specified unit.
     * @param unit The unit of [value].
     * @return A new [Acceleration] instance.
     * @throws ArithmeticException If the conversion overflows [Long].
     * @throws IllegalArgumentException If [value] is not finite.
     */
    fun from(
      value: Double,
      unit: AccelerationUnit,
    ): Acceleration {
      require(value.isFinite()) { "value must be finite: $value" }
      val nanometersPerSecondSquared = (value * unit.nanometersPerSecondSquaredPerUnit.toDouble()).roundToLong()
      return Acceleration(nanometersPerSecondSquared)
    }
  }
}

/**
 * Creates an [Acceleration] from this [Double] value in meters per second squared.
 *
 * @return A new [Acceleration] instance.
 * @throws ArithmeticException If the conversion overflows [Long].
 * @throws IllegalArgumentException If this value is not finite.
 */
val Double.metersPerSecondSquared: Acceleration
  get() = from(this, AccelerationUnit.METER_PER_SECOND_SQUARED)

/**
 * Creates an [Acceleration] from this [Long] value in meters per second squared.
 *
 * @return A new [Acceleration] instance.
 * @throws ArithmeticException If the conversion overflows [Long].
 */
val Long.metersPerSecondSquared: Acceleration
  get() = from(this, AccelerationUnit.METER_PER_SECOND_SQUARED)

/**
 * Creates an [Acceleration] from this [Int] value in meters per second squared.
 *
 * @return A new [Acceleration] instance.
 * @throws ArithmeticException If the conversion overflows [Long].
 */
val Int.metersPerSecondSquared: Acceleration
  get() = from(this.toLong(), AccelerationUnit.METER_PER_SECOND_SQUARED)

/**
 * Divides this [Speed] by a [Duration] to produce an [Acceleration].
 *
 * @param duration The time duration.
 * @return The resulting acceleration.
 * @throws ArithmeticException If [duration] is zero or if the result overflows [Long].
 */
operator fun Speed.div(duration: Duration): Acceleration {
  val totalNanos = duration.inWholeNanoseconds
  require(totalNanos != 0L) { "duration must not be zero" }
  val nanometersPerSecondSquared =
    (this.inWholeNanometersPerSecond.toDouble() * 1_000_000_000.0 / totalNanos.toDouble()).roundToLong()
  return Acceleration.from(nanometersPerSecondSquared, AccelerationUnit.NANOMETER_PER_SECOND_SQUARED)
}

/**
 * Multiplies this [Duration] by an [Acceleration] to produce a [Speed].
 *
 * @param acceleration The acceleration value.
 * @return The resulting speed.
 * @throws ArithmeticException If the multiplication overflows [Long].
 */
operator fun Duration.times(acceleration: Acceleration): Speed = acceleration * this

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
