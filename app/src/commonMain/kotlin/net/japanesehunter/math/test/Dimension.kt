package net.japanesehunter.math.test

/**
 * Defines a physical dimension.
 *
 * ## Description
 *
 * A dimension provides a canonical unit used as the normalization target for conversions.
 *
 * @param D The concrete self type of the dimension.
 */
interface Dimension<D : Dimension<D>> {
  /**
   * Returns the canonical unit of this dimension.
   *
   * @return The canonical unit for conversions.
   */
  val canonicalUnit: QuantityUnit<D>
}
