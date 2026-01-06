package net.japanesehunter.math.test

@ConsistentCopyVisibility
data class QuantityUnit<D : Dimension<D>> private constructor(
  val dimension: D,
  val fromCanonical: Double,
  val name: String,
  val symbol: String,
) {
  companion object {
    fun <D : Dimension<D>> base(
      dimension: D,
      name: String,
      symbol: String,
    ): QuantityUnit<D> =
      QuantityUnit(dimension, 1.0, name, symbol)
  }

  fun derive(
    factorToThis: Double,
    name: String,
    symbol: String,
  ): QuantityUnit<D> =
    copy(
      name = name,
      symbol = symbol,
      fromCanonical = fromCanonical * factorToThis,
    )

  override fun toString(): String =
    "QuantityUnit(1$symbol($name)=$fromCanonical$dimension)"
}
