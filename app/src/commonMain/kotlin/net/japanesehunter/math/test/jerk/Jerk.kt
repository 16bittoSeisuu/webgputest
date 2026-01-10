package net.japanesehunter.math.test.jerk

import net.japanesehunter.math.Proportion
import net.japanesehunter.math.test.Dimension
import net.japanesehunter.math.test.ExactMath.scaleExact
import net.japanesehunter.math.test.Quantity
import net.japanesehunter.math.test.QuantityUnit
import net.japanesehunter.math.test.acceleration.AccelerationQuantity
import net.japanesehunter.math.test.acceleration.div
import net.japanesehunter.math.test.acceleration.metersPerSecondSquared
import net.japanesehunter.math.test.length.nanometer
import net.japanesehunter.math.test.speed.div
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

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
  override val resolution: JerkUnit
    get() = amountPerSecond.resolution / 1.seconds

  override fun toDouble(
    unit: QuantityUnit<Jerk>,
  ): Double {
    val canonicalValue = amountPerSecond.toDouble(metersPerSecondSquared)
    return canonicalValue * (Jerk.canonicalUnit per unit)
  }

  override fun toLong(
    unit: QuantityUnit<Jerk>,
  ): Long {
    val canonicalValue = amountPerSecond.toLong(metersPerSecondSquared)
    return canonicalValue scaleExact (Jerk.canonicalUnit per unit)
  }

  override fun roundToLong(
    unit: QuantityUnit<Jerk>,
  ): Long {
    val canonicalValue = amountPerSecond.roundToLong(metersPerSecondSquared)
    return canonicalValue scaleExact (Jerk.canonicalUnit per unit)
  }

  override fun isPositive(): Boolean =
    amountPerSecond.isPositive()

  override fun isNegative(): Boolean =
    amountPerSecond.isNegative()

  override fun isZero(): Boolean =
    amountPerSecond.isZero()

  override val absoluteValue: JerkQuantity by lazy {
    if (isNegative()) -this else this
  }

  override fun plus(
    other: Quantity<Jerk>,
  ): JerkQuantity =
    JerkQuantity(
      amountPerSecond +
        (
          (other as? JerkQuantity)?.amountPerSecond ?: run {
            val _ = other.toLong(nanometer / 1.seconds / 1.seconds / 1.seconds)
            TODO()
          }
        ),
    )

  override fun minus(
    other: Quantity<Jerk>,
  ): JerkQuantity =
    plus(-other)

  override fun times(
    scalar: Double,
  ): JerkQuantity =
    JerkQuantity(amountPerSecond * scalar)

  override fun times(
    scalar: Long,
  ): JerkQuantity =
    JerkQuantity(amountPerSecond * scalar)

  override fun times(
    scalar: Float,
  ): JerkQuantity =
    JerkQuantity(amountPerSecond * scalar)

  override fun times(
    scalar: Int,
  ): JerkQuantity =
    JerkQuantity(amountPerSecond * scalar)

  override fun times(
    scalar: Short,
  ): JerkQuantity =
    JerkQuantity(amountPerSecond * scalar)

  override fun times(
    scalar: Byte,
  ): JerkQuantity =
    JerkQuantity(amountPerSecond * scalar)

  override fun times(
    proportion: Proportion,
  ): JerkQuantity =
    times(proportion.toDouble())

  override fun div(
    scalar: Double,
  ): JerkQuantity =
    JerkQuantity(amountPerSecond / scalar)

  override fun div(
    scalar: Long,
  ): JerkQuantity =
    JerkQuantity(amountPerSecond / scalar)

  override fun div(
    scalar: Float,
  ): JerkQuantity =
    JerkQuantity(amountPerSecond / scalar)

  override fun div(
    scalar: Int,
  ): JerkQuantity =
    JerkQuantity(amountPerSecond / scalar)

  override fun div(
    scalar: Short,
  ): JerkQuantity =
    JerkQuantity(amountPerSecond / scalar)

  override fun div(
    scalar: Byte,
  ): JerkQuantity =
    JerkQuantity(amountPerSecond / scalar)

  override fun unaryPlus(): JerkQuantity =
    this

  override fun unaryMinus(): JerkQuantity =
    JerkQuantity(-amountPerSecond)

  /**
   * Calculates the acceleration change over the given [duration] at this jerk.
   *
   * @param duration The time period over which to calculate acceleration change.
   * @return The acceleration change as an [AccelerationQuantity].
   */
  operator fun times(
    duration: Duration,
  ): AccelerationQuantity =
    amountPerSecond * duration.toDouble(DurationUnit.SECONDS)

  override fun toString(): String =
    "$amountPerSecond/s"

  override fun equals(
    other: Any?,
  ): Boolean =
    Jerk.areEqual(this, other)

  override fun hashCode(): Int =
    Jerk.calculateHashCode(this)
}
