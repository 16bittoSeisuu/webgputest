package net.japanesehunter.math.test.acceleration

import net.japanesehunter.math.Proportion
import net.japanesehunter.math.test.Dimension
import net.japanesehunter.math.test.ExactMath.reciprocalExact
import net.japanesehunter.math.test.ExactMath.scaleExact
import net.japanesehunter.math.test.Quantity
import net.japanesehunter.math.test.QuantityUnit
import net.japanesehunter.math.test.jerk.JerkQuantity
import net.japanesehunter.math.test.length.nanometer
import net.japanesehunter.math.test.speed.SpeedQuantity
import net.japanesehunter.math.test.speed.div
import net.japanesehunter.math.test.speed.metersPerSecond
import net.japanesehunter.math.test.time.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

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
  override val resolution: AccelerationUnit
    get() = amountPerSecond.resolution / 1.seconds

  override fun toDouble(
    unit: QuantityUnit<Acceleration>,
  ): Double {
    val canonicalValue =
      amountPerSecond.toDouble(metersPerSecond)
    return canonicalValue * (Acceleration.canonicalUnit per unit)
  }

  override fun toLong(
    unit: QuantityUnit<Acceleration>,
  ): Long {
    val canonicalValue =
      amountPerSecond.toLong(metersPerSecond)
    return canonicalValue scaleExact (Acceleration.canonicalUnit per unit)
  }

  override fun roundToLong(
    unit: QuantityUnit<Acceleration>,
  ): Long {
    val canonicalValue =
      amountPerSecond.roundToLong(metersPerSecond)
    return canonicalValue scaleExact (Acceleration.canonicalUnit per unit)
  }

  override fun isPositive(): Boolean =
    amountPerSecond.isPositive()

  override fun isNegative(): Boolean =
    amountPerSecond.isNegative()

  override fun isZero(): Boolean =
    amountPerSecond.isZero()

  override val absoluteValue: AccelerationQuantity by lazy {
    if (isNegative()) -this else this
  }

  override fun plus(
    other: Quantity<Acceleration>,
  ): AccelerationQuantity =
    AccelerationQuantity(
      amountPerSecond +
        (
          (other as? AccelerationQuantity)
            ?.amountPerSecond
            ?: run {
              val _ = other.toLong(nanometer / 1.seconds / 1.seconds)
              TODO()
            }
        ),
    )

  override fun minus(
    other: Quantity<Acceleration>,
  ): AccelerationQuantity =
    plus(-other)

  override fun times(
    scalar: Double,
  ): AccelerationQuantity =
    AccelerationQuantity(amountPerSecond * scalar)

  override fun times(
    scalar: Long,
  ): AccelerationQuantity =
    AccelerationQuantity(amountPerSecond * scalar)

  override fun times(
    scalar: Float,
  ): AccelerationQuantity =
    AccelerationQuantity(amountPerSecond * scalar)

  override fun times(
    scalar: Int,
  ): AccelerationQuantity =
    AccelerationQuantity(amountPerSecond * scalar)

  override fun times(
    scalar: Short,
  ): AccelerationQuantity =
    AccelerationQuantity(amountPerSecond * scalar)

  override fun times(
    scalar: Byte,
  ): AccelerationQuantity =
    AccelerationQuantity(amountPerSecond * scalar)

  override fun times(
    proportion: Proportion,
  ): AccelerationQuantity =
    times(proportion.toDouble())

  override fun div(
    scalar: Double,
  ): AccelerationQuantity =
    AccelerationQuantity(amountPerSecond / scalar)

  override fun div(
    scalar: Long,
  ): AccelerationQuantity =
    AccelerationQuantity(amountPerSecond / scalar)

  override fun div(
    scalar: Float,
  ): AccelerationQuantity =
    AccelerationQuantity(amountPerSecond / scalar)

  override fun div(
    scalar: Int,
  ): AccelerationQuantity =
    AccelerationQuantity(amountPerSecond / scalar)

  override fun div(
    scalar: Short,
  ): AccelerationQuantity =
    AccelerationQuantity(amountPerSecond / scalar)

  override fun div(
    scalar: Byte,
  ): AccelerationQuantity =
    AccelerationQuantity(amountPerSecond / scalar)

  override fun unaryPlus(): AccelerationQuantity =
    this

  override fun unaryMinus(): AccelerationQuantity =
    AccelerationQuantity(-amountPerSecond)

  /**
   * Calculates the speed change over the given [duration] at this acceleration.
   *
   * @param duration The time period over which to calculate speed change.
   * @return The speed change as a [SpeedQuantity].
   */
  operator fun times(
    duration: Duration,
  ): SpeedQuantity =
    amountPerSecond * duration.toDouble(DurationUnit.SECONDS)

  /**
   * Calculates the jerk resulting from an acceleration change of this magnitude over [duration].
   *
   * @param duration The time period over which the acceleration change occurs.
   * @return The resulting jerk as a [JerkQuantity].
   */
  operator fun div(
    duration: Duration,
  ): JerkQuantity =
    JerkQuantity(this * (1.seconds / duration))

  infix fun per(
    duration: Duration,
  ): JerkQuantity =
    div(duration)

  infix fun per(
    timeUnit: TimeUnit,
  ): JerkQuantity =
    div(
      timeUnit.thisToCanonicalFactor
        .reciprocalExact()
        .seconds,
    )

  override fun toString(): String =
    "$amountPerSecond/s"

  override fun equals(
    other: Any?,
  ): Boolean =
    Acceleration.areEqual(this, other)

  override fun hashCode(): Int =
    Acceleration.calculateHashCode(this)
}
