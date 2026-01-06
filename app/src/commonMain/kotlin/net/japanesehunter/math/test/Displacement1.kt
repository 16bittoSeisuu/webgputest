package net.japanesehunter.math.test

interface Displacement1<D : Dimension<D>> {
  val dx: Quantity<D>

  operator fun component1(): Quantity<D> =
    dx

  operator fun plus(
    other: Displacement1<D>,
  ): Displacement1<D>

  operator fun minus(
    other: Displacement1<D>,
  ): Displacement1<D> =
    plus(-other)

  operator fun times(
    scalar: Double,
  ): Displacement1<D>

  operator fun div(
    scalar: Double,
  ): Displacement1<D> =
    times(1.0 / scalar)

  operator fun unaryPlus(): Displacement1<D> =
    this

  operator fun unaryMinus(): Displacement1<D> =
    times(-1.0)
}
