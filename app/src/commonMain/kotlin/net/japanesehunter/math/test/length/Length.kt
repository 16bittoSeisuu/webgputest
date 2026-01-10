package net.japanesehunter.math.test.length

import net.japanesehunter.math.test.Dimension
import net.japanesehunter.math.test.ExactMath.reciprocalExact
import net.japanesehunter.math.test.Quantity
import net.japanesehunter.math.test.QuantityUnit
import net.japanesehunter.math.test.length.meter
import net.japanesehunter.math.test.speed.SpeedQuantity
import net.japanesehunter.math.test.time.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import net.japanesehunter.math.test.length.meter as meters_unit

/**
 * Defines the length dimension.
 *
 * ## Description
 *
 * The canonical unit of this dimension is [meter].
 */
data object Length : Dimension<Length> {
  override val canonicalUnit: QuantityUnit<Length> by lazy {
    meters_unit
  }
}

/**
 * Represents a quantity whose dimension is [Length].
 *
 * ## Description
 *
 * Implementations must satisfy the contract of [Quantity].
 * Equality must be based on the represented physical amount, not on the source numeric type
 * or the unit used at construction.
 */
abstract class LengthQuantity : Quantity<Length> {
  abstract override val resolution: QuantityUnit<Length>

  abstract override fun toDouble(
    unit: QuantityUnit<Length>,
  ): Double

  abstract override fun roundToLong(
    unit: QuantityUnit<Length>,
  ): Long

  abstract override fun toLong(
    unit: QuantityUnit<Length>,
  ): Long

  abstract override fun isPositive(): Boolean

  abstract override fun isNegative(): Boolean

  abstract override fun isZero(): Boolean

  abstract override val absoluteValue: LengthQuantity

  /**
   * Calculates the speed from traveling this distance over the given [duration].
   *
   * @param duration The time period over which the distance is traveled.
   * - Must not be zero.
   * - Must be finite.
   * @return The resulting speed as a [SpeedQuantity].
   * @throws IllegalArgumentException
   * - If [duration] is zero.
   * - If [duration] is infinite.
   * @throws ArithmeticException
   * - If the result's internal representation overflows.
   */
  abstract operator fun div(
    duration: Duration,
  ): SpeedQuantity

  /**
   * Calculates the speed from traveling this distance over the given [duration].
   *
   * @param duration The time period over which the distance is traveled.
   * - Must not be zero.
   * - Must be finite.
   * @return The resulting speed as a [SpeedQuantity].
   * @throws IllegalArgumentException
   * - If [duration] is zero.
   * - If [duration] is infinite.
   * @throws ArithmeticException
   * - If the result's internal representation overflows.
   */
  open infix fun per(
    duration: Duration,
  ): SpeedQuantity =
    this / duration

  /**
   * Calculates the speed from traveling this distance over the given 1-[timeUnit].
   *
   * @param timeUnit The time unit over which the distance is traveled.
   * @return The resulting speed as a [SpeedQuantity].
   * @throws ArithmeticException
   * - If the result's internal representation overflows.
   */
  open infix fun per(
    timeUnit: TimeUnit,
  ): SpeedQuantity =
    this /
      timeUnit.thisToCanonicalFactor
        .reciprocalExact()
        .seconds

  abstract override fun plus(
    other: Quantity<Length>,
  ): LengthQuantity

  override fun minus(
    other: Quantity<Length>,
  ): LengthQuantity =
    plus(-other)

  abstract override fun times(
    scalar: Double,
  ): LengthQuantity

  abstract override fun times(
    scalar: Long,
  ): LengthQuantity

  override fun times(
    scalar: Float,
  ): LengthQuantity =
    times(scalar.toDouble())

  override fun times(
    scalar: Int,
  ): LengthQuantity =
    times(scalar.toLong())

  override fun times(
    scalar: Short,
  ): LengthQuantity =
    times(scalar.toLong())

  override fun times(
    scalar: Byte,
  ): LengthQuantity =
    times(scalar.toLong())

  override fun div(
    scalar: Double,
  ): LengthQuantity =
    times(scalar.reciprocalExact())

  abstract override fun div(
    scalar: Long,
  ): LengthQuantity

  override fun div(
    scalar: Float,
  ): LengthQuantity =
    div(scalar.toDouble())

  override fun div(
    scalar: Int,
  ): LengthQuantity =
    div(scalar.toLong())

  override fun div(
    scalar: Short,
  ): LengthQuantity =
    div(scalar.toLong())

  override fun div(
    scalar: Byte,
  ): LengthQuantity =
    div(scalar.toLong())

  override fun unaryPlus(): LengthQuantity =
    this

  override fun unaryMinus(): LengthQuantity =
    times(-1.0)

  abstract override fun toString(): String

  final override fun equals(
    other: Any?,
  ): Boolean =
    Length.areEqual(this, other)

  final override fun hashCode(): Int =
    Length.calculateHashCode(this)
}
