package net.japanesehunter.math.test

interface Point1<D : Dimension<D>> {
  operator fun plus(
    other: Displacement1<D>,
  ): Point1<D>

  operator fun minus(
    other: Displacement1<D>,
  ): Point1<D>

  operator fun minus(
    other: Point1<D>,
  ): Displacement1<D>

  infix fun relativeTo(
    other: Point1<D>,
  ): Displacement1<D> =
    this - other
}
