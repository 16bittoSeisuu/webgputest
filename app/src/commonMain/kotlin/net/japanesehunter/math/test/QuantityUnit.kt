package net.japanesehunter.math.test

interface QuantityUnit<D : Dimension> {
  val scale: Double
  val name: String
  val symbol: String
}
