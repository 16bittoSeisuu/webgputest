package net.japanesehunter.math.test

sealed interface Length : Dimension

interface LengthQuantity<U : QuantityUnit<Length>> : Quantity<Length, U> {
  override fun toDouble(
    unit: U,
  ): Double

  override fun roundToLong(
    unit: U,
  ): Long

  override fun toLong(
    unit: U,
  ): Long

  override fun plus(
    other: Quantity<Length, U>,
  ): LengthQuantity<U>

  override fun minus(
    other: Quantity<Length, U>,
  ): LengthQuantity<U> =
    plus(-other)

  override fun times(
    scalar: Double,
  ): LengthQuantity<U>

  override fun div(
    scalar: Double,
  ): LengthQuantity<U> =
    times(1.0 / scalar)

  override fun unaryPlus(): LengthQuantity<U> =
    this

  override fun unaryMinus(): LengthQuantity<U> =
    times(-1.0)
}
