package net.japanesehunter.math.test

interface Quantity<D : Dimension, U : QuantityUnit<D>> {
  fun toDouble(
    unit: U,
  ): Double

  fun toFloat(
    unit: U,
  ): Float =
    toDouble(unit).toFloat()

  fun toLong(
    unit: U,
  ): Long

  fun roundToLong(
    unit: U,
  ): Long

  fun toInt(
    unit: U,
  ): Int =
    toLong(unit).toInt()

  fun toShort(
    unit: U,
  ): Short =
    toLong(unit).toShort()

  fun toByte(
    unit: U,
  ): Byte =
    toLong(unit).toByte()

  operator fun plus(
    other: Quantity<D, U>,
  ): Quantity<D, U>

  operator fun minus(
    other: Quantity<D, U>,
  ): Quantity<D, U> =
    plus(-other)

  operator fun times(
    scalar: Double,
  ): Quantity<D, U>

  operator fun div(
    scalar: Double,
  ): Quantity<D, U> =
    times(1.0 / scalar)

  operator fun unaryPlus(): Quantity<D, U> =
    this

  operator fun unaryMinus(): Quantity<D, U> =
    times(-1.0)
}
