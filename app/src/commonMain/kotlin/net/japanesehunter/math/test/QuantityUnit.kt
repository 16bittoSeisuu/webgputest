package net.japanesehunter.math.test

@ConsistentCopyVisibility
data class QuantityUnit<D : Dimension<D>> private constructor(
  val dimension: D,
  // TODO: remove this comment
  // e.g., for nanometers, this is 1e-9
  val canonicalToThis: Double,
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
      canonicalToThis = canonicalToThis * factorToThis,
    )

  override fun toString(): String =
    "QuantityUnit(1$symbol($name)=$canonicalToThis$dimension)"
}
