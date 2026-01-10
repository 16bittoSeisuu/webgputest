package net.japanesehunter.math.test.acceleration

import net.japanesehunter.math.test.Dimension
import net.japanesehunter.math.test.Quantity
import net.japanesehunter.math.test.QuantityUnit
import net.japanesehunter.math.test.jerk.JerkQuantity
import net.japanesehunter.math.test.speed.SpeedQuantity
import kotlin.time.Duration

/**
 * Defines the acceleration dimension.
 *
 * ## Description
 *
 * The canonical unit of this dimension is [metersPerSecondSquared].
 */
data object Acceleration : Dimension<Acceleration> {
  override val canonicalUnit: QuantityUnit<Acceleration> by lazy {
    metersPerSecondSquared
  }
}

/**
 * Represents a quantity whose dimension is [Acceleration].
 *
 * ## Description
 *
 * This is a concrete wrapper that delegates its logic to a [SpeedQuantity]
 * representing "speed change per second".
 *
 * @property amountPerSecond The speed change in one second.
 */
class AccelerationQuantity internal constructor(
  val amountPerSecond: SpeedQuantity,
) : Quantity<Acceleration> {
  override val resolution: QuantityUnit<Acceleration>
    get() = TODO()

  override fun toDouble(
    unit: QuantityUnit<Acceleration>,
  ): Double =
    TODO()

  override fun toLong(
    unit: QuantityUnit<Acceleration>,
  ): Long =
    TODO()

  override fun roundToLong(
    unit: QuantityUnit<Acceleration>,
  ): Long =
    TODO()

  override fun isPositive(): Boolean =
    amountPerSecond.isPositive()

  override fun isNegative(): Boolean =
    amountPerSecond.isNegative()

  override fun isZero(): Boolean =
    amountPerSecond.isZero()

  override val absoluteValue: AccelerationQuantity
    get() = AccelerationQuantity(amountPerSecond.absoluteValue)

  override fun plus(
    other: Quantity<Acceleration>,
  ): AccelerationQuantity =
    TODO()

  override fun minus(
    other: Quantity<Acceleration>,
  ): AccelerationQuantity =
    TODO()

  override fun times(
    scalar: Double,
  ): AccelerationQuantity =
    AccelerationQuantity(amountPerSecond * scalar)

  override fun times(
    scalar: Long,
  ): AccelerationQuantity =
    AccelerationQuantity(amountPerSecond * scalar)

  /**
   * Calculates the speed change over the given [duration] at this acceleration.
   *
   * @param duration The time period over which to calculate speed change.
   * @return The speed change as a [SpeedQuantity].
   */
  operator fun times(
    duration: Duration,
  ): SpeedQuantity =
    TODO()

  /**
   * Calculates the jerk resulting from an acceleration change of this magnitude over [duration].
   *
   * @param duration The time period over which the acceleration change occurs.
   * @return The resulting jerk as a [JerkQuantity].
   */
  operator fun div(
    duration: Duration,
  ): JerkQuantity =
    TODO()

  override fun toString(): String =
    TODO()

  override fun equals(
    other: Any?,
  ): Boolean =
    Acceleration.areEqual(this, other)

  override fun hashCode(): Int =
    Acceleration.calculateHashCode(this)
}
