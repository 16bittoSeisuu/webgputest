package net.japanesehunter.math

import net.japanesehunter.math.Angle.Companion.from
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToLong
import kotlin.math.cos as cosDouble
import kotlin.math.sin as sinDouble
import kotlin.math.tan as tanDouble

private const val NANORADIANS_PER_MICRORADIAN: Long = 1_000L
private const val NANORADIANS_PER_MILLIRADIAN: Long = 1_000_000L
private const val NANORADIANS_PER_RADIAN: Long = 1_000_000_000L
private const val NANORADIANS_PER_TAU: Long = (2.0 * PI * NANORADIANS_PER_RADIAN).toLong()

/**
 * Represents the units supported by [Angle].
 * Each unit defines how many nanoradians it contains.
 *
 * @property nanoradiansPerUnit The number of nanoradians in a single unit.
 * @property symbol The short unit suffix used by [Angle.toString].
 */
enum class AngleUnit(
  internal val nanoradiansPerUnit: Double,
  internal val symbol: String,
) {
  /**
   * The nanoradian unit (1e-9 radians).
   */
  NANORADIAN(1.0, "nrad"),

  /**
   * The microradian unit (1e-6 radians).
   */
  MICRORADIAN(NANORADIANS_PER_MICRORADIAN.toDouble(), "urad"),

  /**
   * The milliradian unit (1e-3 radians).
   */
  MILLIRADIAN(NANORADIANS_PER_MILLIRADIAN.toDouble(), "mrad"),

  /**
   * The radian unit.
   */
  RADIAN(NANORADIANS_PER_RADIAN.toDouble(), "rad"),

  /**
   * The degree unit.
   */
  DEGREE(PI * NANORADIANS_PER_RADIAN / 180.0, "deg"),
}

/**
 * Immutable angle value backed by a signed nanoradian count in a [Long].
 * Precision is fixed to nanoradians; [from] with [Double] rounds to the nearest nanoradian and rejects non-finite values.
 *
 * @author Int16
 */
value class Angle internal constructor(
  private val nanoradians: Long,
) : Comparable<Angle> {
  /**
   * Returns this angle as a whole number of radians, truncated toward zero.
   */
  val inWholeRadians: Long
    get() = nanoradians / NANORADIANS_PER_RADIAN

  /**
   * Converts this [Angle] to a [Long] value using the specified [unit]. The result is truncated toward zero.
   */
  fun toLong(unit: AngleUnit): Long =
    when (unit) {
      AngleUnit.NANORADIAN -> nanoradians
      AngleUnit.MICRORADIAN -> nanoradians / NANORADIANS_PER_MICRORADIAN
      AngleUnit.MILLIRADIAN -> nanoradians / NANORADIANS_PER_MILLIRADIAN
      AngleUnit.RADIAN -> nanoradians / NANORADIANS_PER_RADIAN
      AngleUnit.DEGREE -> (nanoradians.toDouble() / unit.nanoradiansPerUnit).toLong()
    }

  /**
   * Converts this [Angle] to a [Double] value using the specified [unit].
   */
  fun toDouble(unit: AngleUnit): Double = nanoradians.toDouble() / unit.nanoradiansPerUnit

  /**
   * Returns the sine of this angle.
   */
  fun sin(): Double = sinDouble(toDouble(AngleUnit.RADIAN))

  /**
   * Returns the cosine of this angle.
   */
  fun cos(): Double = cosDouble(toDouble(AngleUnit.RADIAN))

  /**
   * Returns the tangent of this angle.
   */
  fun tan(): Double = tanDouble(toDouble(AngleUnit.RADIAN))

  /**
   * Returns the absolute value of this angle.
   */
  val absoluteValue: Angle
    get() =
      if (nanoradians < 0) {
        Angle(safeNegate(nanoradians))
      } else {
        this
      }

  /**
   * Returns `true` if this angle is exactly zero.
   */
  val isZero: Boolean
    get() = nanoradians == 0L

  /**
   * Returns `true` if this angle is strictly positive.
   */
  val isPositive: Boolean
    get() = nanoradians > 0L

  /**
   * Returns `true` if this angle is strictly negative.
   */
  val isNegative: Boolean
    get() = nanoradians < 0L

  /**
   * Returns the negated angle.
   *
   * @throws ArithmeticException If negation overflows [Long].
   */
  operator fun unaryMinus(): Angle = Angle(safeNegate(nanoradians))

  /**
   * Adds another [Angle].
   *
   * @param other The angle to add.
   * @return The sum of the angles.
   * @throws ArithmeticException If the sum overflows [Long].
   */
  operator fun plus(other: Angle): Angle = Angle(safeAdd(nanoradians, other.nanoradians))

  /**
   * Subtracts another [Angle].
   *
   * @param other The angle to subtract.
   * @return The difference of the angles.
   * @throws ArithmeticException If the subtraction overflows [Long].
   */
  operator fun minus(other: Angle): Angle = Angle(safeAdd(nanoradians, safeNegate(other.nanoradians)))

  /**
   * Multiplies this angle by a [Long] factor.
   *
   * @param factor The scaling factor.
   * @return The scaled [Angle].
   * @throws ArithmeticException If the multiplication overflows [Long].
   */
  operator fun times(factor: Long): Angle = Angle(safeMultiply(nanoradians, factor))

  /**
   * Multiplies this angle by a [Double] factor. The result is rounded to the nearest nanoradian.
   *
   * @param factor The scaling factor. Must be finite.
   * @return The scaled [Angle].
   * @throws IllegalArgumentException If [factor] is not finite.
   */
  operator fun times(factor: Double): Angle = Angle(scaleDouble(nanoradians, factor))

  /**
   * Multiplies this angle by an [Int] factor.
   *
   * @param factor The scaling factor.
   * @return The scaled [Angle].
   * @throws ArithmeticException If the multiplication overflows [Long].
   */
  operator fun times(factor: Int): Angle = times(factor.toLong())

  /**
   * Divides this angle by a [Long] divisor.
   *
   * @param divisor The divisor. Must not be zero.
   * @return The scaled [Angle].
   * @throws IllegalArgumentException If [divisor] is zero.
   */
  operator fun div(divisor: Long): Angle {
    require(divisor != 0L) { "Cannot divide an angle by zero." }
    return Angle(nanoradians / divisor)
  }

  /**
   * Divides this angle by a [Double] divisor. The result is rounded to the nearest nanoradian.
   *
   * @param divisor The divisor. Must be finite and non-zero.
   * @return The scaled [Angle].
   * @throws IllegalArgumentException If [divisor] is not finite or zero.
   */
  operator fun div(divisor: Double): Angle {
    require(divisor.isFinite() && divisor != 0.0) { "Divisor must be finite and non-zero: $divisor" }
    return Angle(scaleDouble(nanoradians, 1.0 / divisor))
  }

  /**
   * Divides this angle by an [Int] divisor.
   *
   * @param divisor The divisor. Must not be zero.
   * @return The scaled [Angle].
   * @throws IllegalArgumentException If [divisor] is zero.
   */
  operator fun div(divisor: Int): Angle = div(divisor.toLong())

  /**
   * Divides this angle by another [Angle], returning the ratio.
   *
   * @param other The divisor. Must not be zero.
   * @return The ratio as a [Double].
   * @throws IllegalArgumentException If [other] is zero.
   */
  operator fun div(other: Angle): Double {
    require(other.nanoradians != 0L) { "Cannot divide by a zero angle." }
    return nanoradians.toDouble() / other.nanoradians.toDouble()
  }

  /**
   * Returns the remainder of dividing this angle by [other].
   *
   * @param other The divisor. Must not be zero.
   * @return The remainder angle with the same sign as this angle.
   * @throws IllegalArgumentException If [other] is zero.
   */
  operator fun rem(other: Angle): Angle {
    require(other.nanoradians != 0L) { "Cannot take the remainder by a zero angle." }
    return Angle(nanoradians % other.nanoradians)
  }

  override fun compareTo(other: Angle): Int = nanoradians.compareTo(other.nanoradians)

  override fun toString(): String {
    val absValue = abs(nanoradians)
    val (value, unit) =
      when {
        absValue >= NANORADIANS_PER_RADIAN -> nanoradians.toDouble() / NANORADIANS_PER_RADIAN to AngleUnit.RADIAN
        absValue >= NANORADIANS_PER_MILLIRADIAN -> nanoradians.toDouble() / NANORADIANS_PER_MILLIRADIAN to AngleUnit.MILLIRADIAN
        absValue >= NANORADIANS_PER_MICRORADIAN -> nanoradians.toDouble() / NANORADIANS_PER_MICRORADIAN to AngleUnit.MICRORADIAN
        else -> nanoradians.toDouble() to AngleUnit.NANORADIAN
      }
    val formatted = formatAngleValue(value)
    return "$formatted ${unit.symbol}"
  }

  companion object {
    /**
     * An angle of zero radians.
     */
    val ZERO: Angle = Angle(0L)

    /**
     * 180 degrees (π radians).
     */
    val PI: Angle = Angle((kotlin.math.PI * NANORADIANS_PER_RADIAN).roundToLong())

    /**
     * 90 degrees (π/2 radians).
     */
    val HALF_PI: Angle = Angle((0.5 * kotlin.math.PI * NANORADIANS_PER_RADIAN).roundToLong())

    /**
     * 360 degrees (2π radians).
     */
    val TAU: Angle = Angle(NANORADIANS_PER_TAU)

    /**
     * Creates an [Angle] from the given [value] expressed in [unit].
     */
    fun from(
      value: Long,
      unit: AngleUnit,
    ): Angle = Angle(unit.toNanoradians(value))

    /**
     * Creates an [Angle] from the given [value] expressed in [unit].
     *
     * @throws IllegalArgumentException If [value] is not finite.
     */
    fun from(
      value: Double,
      unit: AngleUnit,
    ): Angle = Angle(unit.toNanoradians(value))
  }
}

/**
 * Creates an [Angle] from this [Int] value expressed in radians.
 */
val Int.radians: Angle
  get() = from(this.toLong(), AngleUnit.RADIAN)

/**
 * Creates an [Angle] from this [Int] value expressed in degrees.
 */
val Int.degrees: Angle
  get() = from(this.toLong(), AngleUnit.DEGREE)

/**
 * Creates an [Angle] from this [Long] value expressed in radians.
 */
val Long.radians: Angle
  get() = from(this, AngleUnit.RADIAN)

/**
 * Creates an [Angle] from this [Long] value expressed in degrees.
 */
val Long.degrees: Angle
  get() = from(this, AngleUnit.DEGREE)

/**
 * Creates an [Angle] from this [Double] value expressed in radians.
 */
val Double.radians: Angle
  get() = from(this, AngleUnit.RADIAN)

/**
 * Creates an [Angle] from this [Double] value expressed in degrees.
 */
val Double.degrees: Angle
  get() = from(this, AngleUnit.DEGREE)

private fun AngleUnit.toNanoradians(value: Long): Long =
  when (this) {
    AngleUnit.NANORADIAN -> {
      value
    }

    AngleUnit.MICRORADIAN -> {
      safeMultiply(value, NANORADIANS_PER_MICRORADIAN)
    }

    AngleUnit.MILLIRADIAN -> {
      safeMultiply(value, NANORADIANS_PER_MILLIRADIAN)
    }

    AngleUnit.RADIAN -> {
      safeMultiply(value, NANORADIANS_PER_RADIAN)
    }

    AngleUnit.DEGREE -> {
      val scaled = value * nanoradiansPerUnit
      ensureNanoradianRange(scaled, "Angle $value $this cannot be represented as nanoradians.")
      scaled.roundToLong()
    }
  }

private fun AngleUnit.toNanoradians(value: Double): Long {
  require(value.isFinite()) { "Angle must be finite: $value" }
  val scaled = value * nanoradiansPerUnit
  ensureNanoradianRange(scaled, "Angle $value $this cannot be represented as nanoradians.")
  return scaled.roundToLong()
}

private fun ensureNanoradianRange(
  scaled: Double,
  message: String,
) {
  require(scaled <= Long.MAX_VALUE && scaled >= Long.MIN_VALUE) { message }
}

private fun formatAngleValue(value: Double): String {
  val rounded = value.roundToLong()
  return if (rounded.toDouble() == value) {
    rounded.toString()
  } else {
    value.toString()
  }
}

private fun safeAdd(
  a: Long,
  b: Long,
): Long {
  val result = a + b
  if ((a xor result) and (b xor result) < 0) {
    throw ArithmeticException("Long overflow: $a + $b")
  }
  return result
}

private fun safeNegate(value: Long): Long {
  if (value == Long.MIN_VALUE) {
    throw ArithmeticException("Long overflow on negate: $value")
  }
  return -value
}

private fun safeMultiply(
  a: Long,
  b: Long,
): Long {
  val result = a * b
  if (a != 0L && result / a != b) {
    throw ArithmeticException("Long overflow: $a * $b")
  }
  return result
}

private fun scaleDouble(
  base: Long,
  factor: Double,
): Long {
  require(factor.isFinite()) { "Scale factor must be finite: $factor" }
  val scaled = base.toDouble() * factor
  require(scaled.isFinite()) { "Scaled angle is not finite: $scaled" }
  ensureNanoradianRange(scaled, "Scaled angle does not fit in Long: $scaled")
  return scaled.roundToLong()
}
