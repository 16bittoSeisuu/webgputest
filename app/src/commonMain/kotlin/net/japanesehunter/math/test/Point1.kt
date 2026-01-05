package net.japanesehunter.math.test

interface Point1<D : Dimension, U : QuantityUnit<D>> {
  operator fun plus(
    other: Displacement1<D, U>,
  ): Point1<D, U>

  operator fun minus(
    other: Displacement1<D, U>,
  ): Point1<D, U>

  operator fun minus(
    other: Point1<D, U>,
  ): Displacement1<D, U>

  infix fun relativeTo(
    other: Point1<D, U>,
  ): Displacement1<D, U> =
    this - other
}
