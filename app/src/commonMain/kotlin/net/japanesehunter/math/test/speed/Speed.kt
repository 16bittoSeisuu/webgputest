package net.japanesehunter.math.test.speed

import net.japanesehunter.math.test.Dimension
import net.japanesehunter.math.test.Quantity
import net.japanesehunter.math.test.QuantityUnit
import net.japanesehunter.math.test.acceleration.AccelerationQuantity
import net.japanesehunter.math.test.length.LengthQuantity
import kotlin.time.Duration

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
class SpeedQuantity internal constructor(val amountPerSecond: LengthQuantity) :
  Quantity<Speed> {
    override val resolution: QuantityUnit<Speed>
      get() = TODO()

    override fun toDouble(
      unit: QuantityUnit<Speed>,
    ): Double =
      TODO()

    override fun toLong(
      unit: QuantityUnit<Speed>,
    ): Long =
      TODO()

    override fun roundToLong(
      unit: QuantityUnit<Speed>,
    ): Long =
      TODO()

    override fun isPositive(): Boolean =
      amountPerSecond.isPositive()

    override fun isNegative(): Boolean =
      amountPerSecond.isNegative()

    override fun isZero(): Boolean =
      amountPerSecond.isZero()

    override val absoluteValue: SpeedQuantity
      get() = SpeedQuantity(amountPerSecond.absoluteValue)

    override fun plus(
      other: Quantity<Speed>,
    ): SpeedQuantity =
      TODO()

    override fun minus(
      other: Quantity<Speed>,
    ): SpeedQuantity =
      TODO()

    override fun times(
      scalar: Double,
    ): SpeedQuantity =
      SpeedQuantity(amountPerSecond * scalar)

    override fun times(
      scalar: Long,
    ): SpeedQuantity =
      SpeedQuantity(amountPerSecond * scalar)

    /**
     * Calculates the distance traveled over the given [duration] at this speed.
     *
     * @param duration The time period over which to calculate distance.
     * @return The distance traveled as a [LengthQuantity].
     */
    operator fun times(
      duration: Duration,
    ): LengthQuantity =
      TODO()

    /**
     * Calculates the acceleration resulting from a speed change of this magnitude over [duration].
     *
     * @param duration The time period over which the speed change occurs.
     * @return The resulting acceleration as an [AccelerationQuantity].
     */
    operator fun div(
      duration: Duration,
    ): AccelerationQuantity =
      TODO()

    override fun toString(): String =
      TODO()

    override fun equals(
      other: Any?,
    ): Boolean =
      Speed.areEqual(this, other)

    override fun hashCode(): Int =
      Speed.calculateHashCode(this)
  }
