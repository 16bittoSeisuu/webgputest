package net.japanesehunter.math

import net.japanesehunter.math.Area.Companion.from
import kotlin.math.abs
import kotlin.math.roundToLong

private const val NANOMETERS_PER_MICROMETER: Long = 1_000L
private const val NANOMETERS_PER_MILLIMETER: Long = 1_000_000L
private const val NANOMETERS_PER_METER: Long = 1_000_000_000L
private const val NANOMETERS_PER_KILOMETER: Long = 1_000_000_000_000L

private val SQUARE_NANOMETERS_PER_MICROMETER: Double = NANOMETERS_PER_MICROMETER.toDouble() * NANOMETERS_PER_MICROMETER.toDouble()
private val SQUARE_NANOMETERS_PER_MILLIMETER: Double = NANOMETERS_PER_MILLIMETER.toDouble() * NANOMETERS_PER_MILLIMETER.toDouble()
private val SQUARE_NANOMETERS_PER_METER: Double = NANOMETERS_PER_METER.toDouble() * NANOMETERS_PER_METER.toDouble()
private val SQUARE_NANOMETERS_PER_KILOMETER: Double = NANOMETERS_PER_KILOMETER.toDouble() * NANOMETERS_PER_KILOMETER.toDouble()

/**
 * Represents the units supported by [Area].
 * Each unit defines how many square nanometers it contains.
 *
 * @property nanometersSquaredPerUnit The number of square nanometers in a single unit.
 * @property symbol The short unit suffix used by [Area.toString].
 */
enum class AreaUnit(
  internal val nanometersSquaredPerUnit: Double,
  internal val symbol: String,
) {
  SQUARE_NANOMETER(1.0, "nm^2"),
  SQUARE_MICROMETER(SQUARE_NANOMETERS_PER_MICROMETER, "um^2"),
  SQUARE_MILLIMETER(SQUARE_NANOMETERS_PER_MILLIMETER, "mm^2"),
  SQUARE_METER(SQUARE_NANOMETERS_PER_METER, "m^2"),
  SQUARE_KILOMETER(SQUARE_NANOMETERS_PER_KILOMETER, "km^2"),
  ;

  companion object {
    /**
     * Returns the [AreaUnit] corresponding to the given [DistanceUnit] squared.
     */
    fun from(distanceUnit: DistanceUnit): AreaUnit =
      when (distanceUnit) {
        DistanceUnit.NANOMETER -> SQUARE_NANOMETER
        DistanceUnit.MICROMETER -> SQUARE_MICROMETER
        DistanceUnit.MILLIMETER -> SQUARE_MILLIMETER
        DistanceUnit.METER -> SQUARE_METER
        DistanceUnit.KILOMETER -> SQUARE_KILOMETER
      }
  }
}

/**
 * Immutable area value backed by a signed square-nanometer count in a [Long].
 * Precision is fixed to square nanometers; [from] with [Double] rounds to the
 * nearest square nanometer and rejects non-finite values.
 * Use [AreaUnit] to view or construct values in square nanometers,
 * square micrometers, square millimeters, square meters, or square kilometers.
 * Arithmetic methods signal overflow with [ArithmeticException].
 *
 * @author Int16
 */
value class Area internal constructor(
  private val squareNanometers: Long,
) : Comparable<Area> {
  /**
   * Returns this area as a whole number of square nanometers.
   */
  val inWholeSquareNanometers: Long
    get() = squareNanometers

  /**
   * Returns this area as a whole number of square micrometers,
   * truncated toward zero.
   */
  val inWholeSquareMicrometers: Long
    get() = toLong(AreaUnit.SQUARE_MICROMETER)

  /**
   * Returns this area as a whole number of square millimeters,
   * truncated toward zero.
   */
  val inWholeSquareMillimeters: Long
    get() = toLong(AreaUnit.SQUARE_MILLIMETER)

  /**
   * Returns this area as a whole number of square meters,
   * truncated toward zero.
   */
  val inWholeSquareMeters: Long
    get() = toLong(AreaUnit.SQUARE_METER)

  /**
   * Returns this area as a whole number of square kilometers,
   * truncated toward zero.
   */
  val inWholeSquareKilometers: Long
    get() = toLong(AreaUnit.SQUARE_KILOMETER)

  /**
   * Converts this [Area] to a [Long] value using the specified [unit].
   * The result is truncated toward zero.
   *
   * @param unit The [AreaUnit] to convert to.
   * @return The truncated [Long] representation in [unit].
   */
  fun toLong(unit: AreaUnit): Long = (squareNanometers.toDouble() / unit.nanometersSquaredPerUnit).toLong()

  /**
   * Converts this [Area] to a [Double] value using the specified [unit].
   *
   * @param unit The [AreaUnit] to convert to.
   * @return The [Double] representation in [unit].
   */
  fun toDouble(unit: AreaUnit): Double = squareNanometers.toDouble() / unit.nanometersSquaredPerUnit

  /**
   * Returns the absolute value of this area.
   */
  val absoluteValue: Area
    get() =
      if (squareNanometers < 0) {
        Area(safeNegate(squareNanometers))
      } else {
        this
      }

  /**
   * Returns `true` if this area is exactly zero.
   */
  val isZero: Boolean
    get() = squareNanometers == 0L

  /**
   * Returns `true` if this area is strictly positive.
   */
  val isPositive: Boolean
    get() = squareNanometers > 0L

  /**
   * Returns `true` if this area is strictly negative.
   */
  val isNegative: Boolean
    get() = squareNanometers < 0L

  /**
   * Returns the negated area.
   *
   * @throws ArithmeticException If negation overflows [Long].
   */
  operator fun unaryMinus(): Area = Area(safeNegate(squareNanometers))

  /**
   * Adds another [Area].
   *
   * @param other The area to add.
   * @return The sum of the areas.
   * @throws ArithmeticException If the sum overflows [Long].
   */
  operator fun plus(other: Area): Area = Area(safeAdd(squareNanometers, other.squareNanometers))

  /**
   * Subtracts another [Area].
   *
   * @param other The area to subtract.
   * @return The difference of the areas.
   * @throws ArithmeticException If the subtraction overflows [Long].
   */
  operator fun minus(other: Area): Area = Area(safeAdd(squareNanometers, safeNegate(other.squareNanometers)))

  /**
   * Multiplies this area by a [Long] factor.
   *
   * @param factor The scaling factor.
   * @return The scaled [Area].
   * @throws ArithmeticException If the multiplication overflows [Long].
   */
  operator fun times(factor: Long): Area = Area(safeMultiply(squareNanometers, factor))

  /**
   * Multiplies this area by a [Double] factor.
   * The result is rounded to the nearest square nanometer.
   *
   * @param factor The scaling factor. Must be finite.
   * @return The scaled [Area].
   * @throws IllegalArgumentException If [factor] is not finite.
   */
  operator fun times(factor: Double): Area = Area(scaleDouble(squareNanometers, factor))

  /**
   * Divides this area by a [Long] divisor.
   *
   * @param divisor The divisor. Must not be zero.
   * @return The scaled [Area].
   * @throws IllegalArgumentException If [divisor] is zero.
   */
  operator fun div(divisor: Long): Area {
    require(divisor != 0L) { "Cannot divide an area by zero." }
    return Area(squareNanometers / divisor)
  }

  /**
   * Divides this area by a [Double] divisor.
   * The result is rounded to the nearest square nanometer.
   *
   * @param divisor The divisor. Must be finite and non-zero.
   * @return The scaled [Area].
   * @throws IllegalArgumentException If [divisor] is not finite or zero.
   */
  operator fun div(divisor: Double): Area {
    require(divisor.isFinite() && divisor != 0.0) {
      "Divisor must be finite and non-zero: $divisor"
    }
    return Area(scaleDouble(squareNanometers, 1.0 / divisor))
  }

  /**
   * Divides this area by another [Area], returning the ratio.
   *
   * @param other The divisor. Must not be zero.
   * @return The ratio as a [Double].
   * @throws IllegalArgumentException If [other] is zero.
   */
  operator fun div(other: Area): Double {
    require(other.squareNanometers != 0L) { "Cannot divide by a zero area." }
    return squareNanometers.toDouble() / other.squareNanometers.toDouble()
  }

  override fun compareTo(other: Area): Int = squareNanometers.compareTo(other.squareNanometers)

  override fun toString(): String {
    val isNegative = squareNanometers < 0
    val absSquareNanometers = absoluteSquareNanometers(squareNanometers)
    val (value, unit) =
      when {
        absSquareNanometers >= SQUARE_NANOMETERS_PER_KILOMETER && SQUARE_NANOMETERS_PER_KILOMETER.isFinite() ->
          absSquareNanometers / SQUARE_NANOMETERS_PER_KILOMETER to AreaUnit.SQUARE_KILOMETER
        absSquareNanometers >= SQUARE_NANOMETERS_PER_METER ->
          absSquareNanometers / SQUARE_NANOMETERS_PER_METER to AreaUnit.SQUARE_METER
        absSquareNanometers >= SQUARE_NANOMETERS_PER_MILLIMETER ->
          absSquareNanometers / SQUARE_NANOMETERS_PER_MILLIMETER to AreaUnit.SQUARE_MILLIMETER
        absSquareNanometers >= SQUARE_NANOMETERS_PER_MICROMETER ->
          absSquareNanometers / SQUARE_NANOMETERS_PER_MICROMETER to AreaUnit.SQUARE_MICROMETER
        else -> absSquareNanometers to AreaUnit.SQUARE_NANOMETER
      }
    val formatted = formatAreaValue(value)
    return if (isNegative) {
      "-$formatted${unit.symbol}"
    } else {
      "$formatted${unit.symbol}"
    }
  }

  companion object {
    /**
     * An area of zero square nanometers.
     */
    val ZERO: Area = Area(0L)

    /**
    * Creates an [Area] from a [Long] value with the specified [unit].
    *
    * @param value The magnitude expressed in [unit].
    * @param unit The [AreaUnit] describing the input value.
    * @return The created [Area].
    * @throws ArithmeticException If the conversion overflows [Long].
    */
    fun from(
      value: Long,
      unit: AreaUnit = AreaUnit.SQUARE_NANOMETER,
    ): Area = Area(unit.toSquareNanometers(value))

    /**
     * Creates an [Area] from a [Double] value with the specified [unit].
     * The value is rounded to the nearest square nanometer.
     *
     * @param value The magnitude expressed in [unit]. Must be finite.
     * @param unit The [AreaUnit] describing the input value.
     * @return The created [Area].
     * @throws IllegalArgumentException If [value] is not finite.
     */
    fun from(
      value: Double,
      unit: AreaUnit = AreaUnit.SQUARE_NANOMETER,
    ): Area = Area(unit.toSquareNanometers(value))
  }
}

/**
 * Creates an [Area] from this [Long] value expressed in square nanometers.
 */
val Long.squareNanometers: Area
  get() = Area.from(this, AreaUnit.SQUARE_NANOMETER)

/**
 * Creates an [Area] from this [Long] value expressed in square micrometers.
 */
val Long.squareMicrometers: Area
  get() = Area.from(this, AreaUnit.SQUARE_MICROMETER)

/**
 * Creates an [Area] from this [Long] value expressed in square millimeters.
 */
val Long.squareMillimeters: Area
  get() = Area.from(this, AreaUnit.SQUARE_MILLIMETER)

/**
 * Creates an [Area] from this [Long] value expressed in square meters.
 */
val Long.squareMeters: Area
  get() = Area.from(this, AreaUnit.SQUARE_METER)

/**
 * Creates an [Area] from this [Long] value expressed in square kilometers.
 */
val Long.squareKilometers: Area
  get() = Area.from(this, AreaUnit.SQUARE_KILOMETER)

/**
 * Creates an [Area] from this [Double] value expressed in square nanometers.
 * The value is rounded to the nearest square nanometer.
 */
val Double.squareNanometers: Area
  get() = Area.from(this, AreaUnit.SQUARE_NANOMETER)

/**
 * Creates an [Area] from this [Double] value expressed in square micrometers.
 * The value is rounded to the nearest square nanometer.
 */
val Double.squareMicrometers: Area
  get() = Area.from(this, AreaUnit.SQUARE_MICROMETER)

/**
 * Creates an [Area] from this [Double] value expressed in square millimeters.
 * The value is rounded to the nearest square nanometer.
 */
val Double.squareMillimeters: Area
  get() = Area.from(this, AreaUnit.SQUARE_MILLIMETER)

/**
 * Creates an [Area] from this [Double] value expressed in square meters.
 * The value is rounded to the nearest square nanometer.
 */
val Double.squareMeters: Area
  get() = Area.from(this, AreaUnit.SQUARE_METER)

/**
 * Creates an [Area] from this [Double] value expressed in square kilometers.
 * The value is rounded to the nearest square nanometer.
 */
val Double.squareKilometers: Area
  get() = Area.from(this, AreaUnit.SQUARE_KILOMETER)

/**
 * Multiplies a [Double] by an [Area].
 *
 * @receiver The scaling factor. Must be finite.
 * @param area The area to scale.
 * @return The scaled [Area].
 */
operator fun Double.times(area: Area): Area = area * this

private fun AreaUnit.toSquareNanometers(value: Long): Long =
  when (this) {
    AreaUnit.SQUARE_NANOMETER -> value
    else -> {
      val scaled = value.toDouble() * nanometersSquaredPerUnit
      ensureInLongRange(scaled, "Area $value $this cannot be represented as square nanometers.")
      scaled.roundToLong()
    }
  }

private fun AreaUnit.toSquareNanometers(value: Double): Long {
  require(value.isFinite()) { "Area must be finite: $value $this" }
  val scaled = value * nanometersSquaredPerUnit
  ensureInLongRange(scaled, "Area $value $this cannot be represented as square nanometers.")
  return scaled.roundToLong()
}

private fun ensureInLongRange(
  scaled: Double,
  message: String,
) {
  require(scaled.isFinite()) { "Converted area is not finite: $scaled" }
  require(scaled <= Long.MAX_VALUE && scaled >= Long.MIN_VALUE) { message }
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
  ensureInLongRange(scaled, "Scaled area does not fit in Long: $scaled")
  return scaled.roundToLong()
}

private fun absoluteSquareNanometers(value: Long): Double =
  if (value == Long.MIN_VALUE) {
    Long.MAX_VALUE.toDouble()
  } else {
    abs(value.toDouble())
  }

private fun formatAreaValue(value: Double): String {
  val rounded = value.roundToLong()
  return if (rounded.toDouble() == value) {
    rounded.toString()
  } else {
    value.toString()
  }
}

