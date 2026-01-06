package net.japanesehunter.math.test

interface Dimension<D : Dimension<D>> {
  val canonicalUnit: QuantityUnit<D>
}
