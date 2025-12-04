package net.japanesehunter.math

import kotlin.math.roundToLong

private const val FRACTION_ZERO: Double = 0.0
private const val FRACTION_QUARTER: Double = 0.25
private const val FRACTION_HALF: Double = 0.5
private const val FRACTION_THREE_QUARTERS: Double = 0.75
private const val FRACTION_ONE: Double = 1.0

/**
 * Immutable dimensionless value representing a fraction between 0.0 and 1.0 inclusive.
 * Input values are clamped into this range; only non-finite inputs result in
 * [IllegalArgumentException]. Arithmetic methods also clamp their results.
 *
 * @author Int16
 */
value class Proportion internal constructor(
  private val fraction: Double,
) : Comparable<Proportion> {
  /**
   * Returns this proportion as a fraction in [0.0, 1.0].
   */
  fun toDouble(): Double = fraction

  /**
   * Returns this proportion as a percentage in [0.0, 100.0].
   */
  fun toPercent(): Double = fraction * 100.0

  /**
   * Returns the complementary proportion (1 - this).
   * The result is clamped to [0.0, 1.0].
   */
  val complement: Proportion
    get() = createProportion(ensureFraction(FRACTION_ONE - fraction))

  /**
   * Returns `true` if this proportion is exactly zero.
   */
  val isZero: Boolean
    get() = fraction == FRACTION_ZERO

  /**
   * Returns `true` if this proportion is exactly one.
   */
  val isFull: Boolean
    get() = fraction == FRACTION_ONE

  /**
   * Returns `true` if this proportion is strictly between zero and one.
   */
  val isPartial: Boolean
    get() = fraction > 0.0 && fraction < 1.0

  /**
   * Adds another [Proportion], clamping the sum to [0.0, 1.0].
   *
   * @param other The proportion to add.
   * @return The clamped sum of the proportions.
   */
  operator fun plus(other: Proportion): Proportion = createProportion(ensureFraction(fraction + other.fraction))

  /**
   * Subtracts another [Proportion], clamping the result to [0.0, 1.0].
   *
   * @param other The proportion to subtract.
   * @return The clamped difference of the proportions.
   */
  operator fun minus(other: Proportion): Proportion = createProportion(ensureFraction(fraction - other.fraction))

  /**
   * Multiplies this proportion by another [Proportion].
   *
   * @param other The proportion to multiply by.
   * @return The product of the proportions.
   */
  operator fun times(other: Proportion): Proportion = createProportion(ensureFraction(fraction * other.fraction))

  /**
   * Applies this proportion to a [Double] value.
   *
   * @param value The magnitude to scale. Must be finite.
   * @return The scaled value.
   * @throws IllegalArgumentException If [value] is not finite.
   */
  operator fun times(value: Double): Double = applyTo(value)

  /**
   * Applies this proportion to a [Double] value.
   *
   * @param value The magnitude to scale. Must be finite.
   * @return The scaled value.
   * @throws IllegalArgumentException If [value] is not finite.
   */
  fun applyTo(value: Double): Double {
    require(value.isFinite()) { "Scaled value must be finite: $value" }
    return value * fraction
  }

  /**
   * Divides this proportion by another [Proportion], returning the ratio.
   *
   * @param other The divisor. The result must be finite.
   * @return The ratio as a [Double].
   * @throws IllegalArgumentException If the result is not finite (for example, dividing by zero).
   */
  operator fun div(other: Proportion): Double {
    val ratio = fraction / other.fraction
    require(ratio.isFinite()) { "Ratio must be finite: $ratio" }
    return ratio
  }

  override fun compareTo(other: Proportion): Int = fraction.compareTo(other.fraction)

  override fun toString(): String {
    val formattedPercent = formatProportionValue(toPercent())
    return "$formattedPercent%"
  }

  companion object {
    /**
     * A proportion of 0.0.
     */
    val ZERO: Proportion = Proportion(FRACTION_ZERO)

    /**
     * A proportion of 0.25.
     */
    val QUARTER: Proportion = Proportion(FRACTION_QUARTER)

    /**
     * A proportion of 0.5.
     */
    val HALF: Proportion = Proportion(FRACTION_HALF)

    /**
     * A proportion of 0.75.
     */
    val THREE_QUARTERS: Proportion = Proportion(FRACTION_THREE_QUARTERS)

    /**
     * A proportion of 1.0.
     */
    val ONE: Proportion = Proportion(FRACTION_ONE)

    /**
     * Creates a [Proportion] from a fractional value between 0.0 and 1.0.
     *
     * @param value The fraction to wrap. Must be finite; values are clamped to [0.0, 1.0].
     * @return The created [Proportion].
     * @throws IllegalArgumentException If [value] is not finite.
     */
    fun fromFraction(value: Double): Proportion = createProportion(ensureFraction(value))

    /**
     * Creates a [Proportion] from a percentage between 0.0 and 100.0.
     *
     * @param percent The percentage to wrap. Must be finite; values are clamped to [0.0, 100.0].
     * @return The created [Proportion].
     * @throws IllegalArgumentException If [percent] is not finite.
     */
    fun fromPercent(percent: Double): Proportion = createProportion(percentToFraction(percent))
  }
}

/**
 * Creates a [Proportion] from this [Double] fraction in [0.0, 1.0].
 * Out-of-range values are clamped; non-finite values throw [IllegalArgumentException].
 */
val Double.proportion: Proportion
  get() = Proportion.fromFraction(this)

/**
 * Creates a [Proportion] from this [Double] percentage in [0.0, 100.0].
 * Out-of-range values are clamped; non-finite values throw [IllegalArgumentException].
 */
val Double.percent: Proportion
  get() = Proportion.fromPercent(this)

/**
 * Creates a [Proportion] from this [Int] percentage in [0, 100].
 * Out-of-range values are clamped.
 */
val Int.percent: Proportion
  get() = toDouble().percent

/**
 * Applies this [Proportion] to a [Double] value.
 *
 * @receiver The value to scale. Must be finite.
 * @return The scaled value.
 * @throws IllegalArgumentException If the receiver is not finite.
 */
operator fun Double.times(proportion: Proportion): Double = proportion * this

private fun percentToFraction(percent: Double): Double {
  require(percent.isFinite()) { "Proportion percent must be finite: $percent" }
  val clampedPercent = percent.coerceIn(0.0, 100.0)
  return clampedPercent / 100.0
}

private fun ensureFraction(fraction: Double): Double {
  require(fraction.isFinite()) { "Proportion must be finite: $fraction" }
  return fraction.coerceIn(0.0, 1.0)
}

private fun createProportion(fraction: Double): Proportion =
  when (fraction) {
    FRACTION_ZERO -> Proportion.ZERO
    FRACTION_QUARTER -> Proportion.QUARTER
    FRACTION_HALF -> Proportion.HALF
    FRACTION_THREE_QUARTERS -> Proportion.THREE_QUARTERS
    FRACTION_ONE -> Proportion.ONE
    else -> Proportion(fraction)
  }

private fun formatProportionValue(percentValue: Double): String {
  val rounded = percentValue.roundToLong()
  return if (rounded.toDouble() == percentValue) {
    rounded.toString()
  } else {
    percentValue.toString()
  }
}
