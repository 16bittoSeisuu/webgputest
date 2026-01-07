package net.japanesehunter.math.test

interface Quantity<D : Dimension<D>> {
  fun toDouble(
    unit: QuantityUnit<D>,
  ): Double

  fun toFloat(
    unit: QuantityUnit<D>,
  ): Float =
    toDouble(unit).toFloat()

  fun toLong(
    unit: QuantityUnit<D>,
  ): Long

  fun roundToLong(
    unit: QuantityUnit<D>,
  ): Long

  fun toInt(
    unit: QuantityUnit<D>,
  ): Int =
    toLong(unit).toInt()

  fun roundToInt(
    unit: QuantityUnit<D>,
  ): Int =
    roundToLong(unit).toInt()

  fun toShort(
    unit: QuantityUnit<D>,
  ): Short =
    toLong(unit).toShort()

  fun toByte(
    unit: QuantityUnit<D>,
  ): Byte =
    toLong(unit).toByte()

  operator fun plus(
    other: Quantity<D>,
  ): Quantity<D>

  operator fun minus(
    other: Quantity<D>,
  ): Quantity<D> =
    plus(-other)

  operator fun times(
    scalar: Double,
  ): Quantity<D>

  operator fun div(
    scalar: Double,
  ): Quantity<D> =
    times(1.0 / scalar)

  operator fun unaryPlus(): Quantity<D> =
    this

  operator fun unaryMinus(): Quantity<D> =
    times(-1.0)
}
