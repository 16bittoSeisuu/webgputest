package net.japanesehunter.math.test.speed

import net.japanesehunter.math.Proportion
import net.japanesehunter.math.test.Dimension
import net.japanesehunter.math.test.ExactMath.reciprocalExact
import net.japanesehunter.math.test.ExactMath.scaleExact
import net.japanesehunter.math.test.Quantity
import net.japanesehunter.math.test.QuantityUnit
import net.japanesehunter.math.test.acceleration.AccelerationQuantity
import net.japanesehunter.math.test.length.LengthQuantity
import net.japanesehunter.math.test.length.meter
import net.japanesehunter.math.test.length.nanometer
import net.japanesehunter.math.test.time.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.toDuration

/**
 * Defines the speed dimension.
 *
 * ## Description
 *
 * The canonical unit of this dimension is [metersPerSecond].
 */
data object Speed : Dimension<Speed> {
  override val canonicalUnit: QuantityUnit<Speed> by lazy {
    metersPerSecond
  }
}

/**
 * Represents a quantity whose dimension is [Speed].
 *
 * ## Description
 *
 * This is a concrete wrapper that delegates its logic to a [LengthQuantity]
 * representing "distance traveled per second".
 *
 * @property amountPerSecond The length traveled in one second.
 */
class SpeedQuantity internal constructor(
  internal val amountPerSecond: LengthQuantity,
) : Quantity<Speed> {
  override val resolution: QuantityUnit<Speed>
    get() = amountPerSecond.resolution / 1.seconds

  override fun toDouble(
    unit: QuantityUnit<Speed>,
  ): Double {
    val canonicalValue =
      amountPerSecond.toDouble(meter)
    return canonicalValue * (Speed.canonicalUnit per unit)
  }

  override fun toLong(
    unit: QuantityUnit<Speed>,
  ): Long {
    val canonicalValue =
      amountPerSecond.toLong(meter)
    return canonicalValue scaleExact (Speed.canonicalUnit per unit)
  }

  override fun roundToLong(
    unit: QuantityUnit<Speed>,
  ): Long {
    val canonicalValue =
      amountPerSecond.roundToLong(meter)
    return canonicalValue scaleExact (Speed.canonicalUnit per unit)
  }

  override fun isPositive(): Boolean =
    amountPerSecond.isPositive()

  override fun isNegative(): Boolean =
    amountPerSecond.isNegative()

  override fun isZero(): Boolean =
    amountPerSecond.isZero()

  override val absoluteValue: SpeedQuantity by lazy {
    if (isNegative()) -this else this
  }

  override fun plus(
    other: Quantity<Speed>,
  ): SpeedQuantity =
    SpeedQuantity(
      amountPerSecond +
        (
          (other as? SpeedQuantity)?.amountPerSecond
            ?: run {
              val _ = other.toLong(nanometer / 1.seconds)
              TODO()
            }
        ),
    )

  override fun minus(
    other: Quantity<Speed>,
  ): SpeedQuantity =
    plus(-other)

  override fun times(
    scalar: Double,
  ): SpeedQuantity =
    SpeedQuantity(amountPerSecond * scalar)

  override fun times(
    scalar: Long,
  ): SpeedQuantity =
    SpeedQuantity(amountPerSecond * scalar)

  override fun times(
    scalar: Float,
  ): SpeedQuantity =
    SpeedQuantity(amountPerSecond * scalar)

  override fun times(
    scalar: Int,
  ): SpeedQuantity =
    SpeedQuantity(amountPerSecond * scalar)

  override fun times(
    scalar: Short,
  ): SpeedQuantity =
    SpeedQuantity(amountPerSecond * scalar)

  override fun times(
    scalar: Byte,
  ): SpeedQuantity =
    SpeedQuantity(amountPerSecond * scalar)

  override fun times(
    proportion: Proportion,
  ): SpeedQuantity =
    times(proportion.toDouble())

  override fun div(
    scalar: Double,
  ): SpeedQuantity =
    SpeedQuantity(amountPerSecond / scalar)

  override fun div(
    scalar: Long,
  ): SpeedQuantity =
    SpeedQuantity(amountPerSecond / scalar)

  override fun div(
    scalar: Float,
  ): SpeedQuantity =
    SpeedQuantity(amountPerSecond / scalar)

  override fun div(
    scalar: Int,
  ): SpeedQuantity =
    SpeedQuantity(amountPerSecond / scalar)

  override fun div(
    scalar: Short,
  ): SpeedQuantity =
    SpeedQuantity(amountPerSecond / scalar)

  override fun div(
    scalar: Byte,
  ): SpeedQuantity =
    SpeedQuantity(amountPerSecond / scalar)

  override fun unaryPlus(): SpeedQuantity =
    this

  override fun unaryMinus(): SpeedQuantity =
    SpeedQuantity(-amountPerSecond)

  /**
   * Calculates the distance traveled over the given [duration] at this speed.
   *
   * @param duration The time period over which to calculate distance.
   * @return The distance traveled as a [LengthQuantity].
   */
  operator fun times(
    duration: Duration,
  ): LengthQuantity =
    amountPerSecond * duration.toDouble(DurationUnit.SECONDS)

  /**
   * Calculates the acceleration resulting from a speed change of this magnitude over [duration].
   *
   * @param duration The time period over which the speed change occurs.
   * @return The resulting acceleration as an [AccelerationQuantity].
   * @throws IllegalArgumentException
   * - If [duration] is zero.
   * - If [duration] is infinite.
   * @throws ArithmeticException
   * - If the result's internal representation overflows.
   */
  operator fun div(
    duration: Duration,
  ): AccelerationQuantity =
    AccelerationQuantity(
      this * (1.toDuration(DurationUnit.SECONDS) / duration),
    )

  /**
   * Calculates the acceleration resulting from a speed change of this magnitude over [duration].
   *
   * @param duration The time period over which the speed change occurs.
   * @return The resulting acceleration as an [AccelerationQuantity].
   * @throws IllegalArgumentException
   * - If [duration] is zero.
   * - If [duration] is infinite.
   * @throws ArithmeticException
   * - If the result's internal representation overflows.
   */
  infix fun per(
    duration: Duration,
  ): AccelerationQuantity =
    div(duration)

  /**
   * Calculates the acceleration resulting from a speed change of this magnitude over the given [timeUnit].
   * @param timeUnit The time unit over which the speed change occurs.
   * @return The resulting acceleration as an [AccelerationQuantity].
   * @throws ArithmeticException
   * - If the result's internal representation overflows.
   */
  infix fun per(
    timeUnit: TimeUnit,
  ): AccelerationQuantity =
    div(
      timeUnit.thisToCanonicalFactor
        .reciprocalExact()
        .toDuration(DurationUnit.SECONDS),
    )

  override fun toString(): String =
    "$amountPerSecond/s"

  override fun equals(
    other: Any?,
  ): Boolean =
    Speed.areEqual(this, other)

  override fun hashCode(): Int =
    Speed.calculateHashCode(this)
}
