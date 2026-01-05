package net.japanesehunter.math.test

interface Displacement1<D : Dimension, U : QuantityUnit<D>> {
  val dx: Quantity<D, U>

  operator fun component1(): Quantity<D, U> =
    dx

  operator fun plus(
    other: Displacement1<D, U>,
  ): Displacement1<D, U>

  operator fun minus(
    other: Displacement1<D, U>,
  ): Displacement1<D, U> =
    plus(-other)

  operator fun times(
    scalar: Double,
  ): Displacement1<D, U>

  operator fun div(
    scalar: Double,
  ): Displacement1<D, U> =
    times(1.0 / scalar)

  operator fun unaryPlus(): Displacement1<D, U> =
    this

  operator fun unaryMinus(): Displacement1<D, U> =
    times(-1.0)
}
