package net.japanesehunter.math.test.length

import net.japanesehunter.math.test.Dimension
import net.japanesehunter.math.test.Quantity
import net.japanesehunter.math.test.QuantityUnit
import net.japanesehunter.math.test.length.meters as meters_unit

data object Length : Dimension<Length>, LengthProvider {
  override val canonicalUnit: QuantityUnit<Length> by lazy {
    meters_unit
  }

  override val zero: LengthQuantity = super.zero

  override fun Long.times(
    unit: LengthUnit,
  ): LengthQuantity {
    TODO("Not yet implemented")
  }

  override fun Double.times(
    unit: LengthUnit,
  ): LengthQuantity {
    TODO("Not yet implemented")
  }
}

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

  override fun div(
    scalar: Double,
  ): LengthQuantity =
    times(1.0 / scalar)

  override fun unaryPlus(): LengthQuantity =
    this

  override fun unaryMinus(): LengthQuantity =
    times(-1.0)
}
