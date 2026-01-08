package net.japanesehunter.math.test.length

import net.japanesehunter.math.test.Dimension
import net.japanesehunter.math.test.ExactMath.reciprocalExact
import net.japanesehunter.math.test.Quantity
import net.japanesehunter.math.test.QuantityUnit
import net.japanesehunter.math.test.length.meters
import net.japanesehunter.math.test.length.meters as meters_unit

/**
 * Defines the length dimension.
 *
 * ## Description
 *
 * The canonical unit of this dimension is [meters].
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
interface LengthQuantity : Quantity<Length> {
  override fun toDouble(
    unit: QuantityUnit<Length>,
  ): Double

  override fun roundToLong(
    unit: QuantityUnit<Length>,
  ): Long

  override fun toLong(
    unit: QuantityUnit<Length>,
  ): Long

  override fun plus(
    other: Quantity<Length>,
  ): LengthQuantity

  override fun minus(
    other: Quantity<Length>,
  ): LengthQuantity =
    plus(-other)

  override fun times(
    scalar: Double,
  ): LengthQuantity

  override fun times(
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

  override fun div(
    scalar: Long,
  ): LengthQuantity =
    TODO()

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

  override fun toString(): String

  override fun equals(
    other: Any?,
  ): Boolean

  override fun hashCode(): Int
}
