package net.japanesehunter.math.test.jerk

import net.japanesehunter.math.test.Dimension
import net.japanesehunter.math.test.Quantity
import net.japanesehunter.math.test.QuantityUnit
import net.japanesehunter.math.test.acceleration.AccelerationQuantity
import kotlin.time.Duration

/**
 * Defines the jerk dimension.
 *
 * ## Description
 *
 * The canonical unit of this dimension is [metersPerSecondCubed].
 */
data object Jerk : Dimension<Jerk> {
  override val canonicalUnit: QuantityUnit<Jerk> by lazy {
    metersPerSecondCubed
  }
}

/**
 * Represents a quantity whose dimension is [Jerk].
 *
 * ## Description
 *
 * This is a concrete wrapper that delegates its logic to an [AccelerationQuantity]
 * representing "acceleration change per second".
 *
 * @property amountPerSecond The acceleration change in one second.
 */
class JerkQuantity internal constructor(
  val amountPerSecond: AccelerationQuantity,
) : Quantity<Jerk> {
  override val resolution: QuantityUnit<Jerk>
    get() = TODO()

  override fun toDouble(
    unit: QuantityUnit<Jerk>,
  ): Double =
    TODO()

  override fun toLong(
    unit: QuantityUnit<Jerk>,
  ): Long =
    TODO()

  override fun roundToLong(
    unit: QuantityUnit<Jerk>,
  ): Long =
    TODO()

  override fun isPositive(): Boolean =
    amountPerSecond.isPositive()

  override fun isNegative(): Boolean =
    amountPerSecond.isNegative()

  override fun isZero(): Boolean =
    amountPerSecond.isZero()

  override val absoluteValue: JerkQuantity
    get() = JerkQuantity(amountPerSecond.absoluteValue)

  override fun plus(
    other: Quantity<Jerk>,
  ): JerkQuantity =
    TODO()

  override fun minus(
    other: Quantity<Jerk>,
  ): JerkQuantity =
    TODO()

  override fun times(
    scalar: Double,
  ): JerkQuantity =
    JerkQuantity(amountPerSecond * scalar)

  override fun times(
    scalar: Long,
  ): JerkQuantity =
    JerkQuantity(amountPerSecond * scalar)

  /**
   * Calculates the acceleration change over the given [duration] at this jerk.
   *
   * @param duration The time period over which to calculate acceleration change.
   * @return The acceleration change as an [AccelerationQuantity].
   */
  operator fun times(
    duration: Duration,
  ): AccelerationQuantity =
    TODO()

  override fun toString(): String =
    TODO()

  override fun equals(
    other: Any?,
  ): Boolean =
    Jerk.areEqual(this, other)

  override fun hashCode(): Int =
    Jerk.calculateHashCode(this)
}
