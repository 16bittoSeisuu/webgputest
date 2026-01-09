package net.japanesehunter.math.test

/**
 * Defines a physical dimension.
 *
 * ## Description
 *
 * A dimension provides a canonical unit used as the normalization target for conversions.
 *
 * Implementations must ensure that two [Dimension] objects representing the same physical
 * dimension are considered equal via [equals] and have the same [hashCode].
 *
 * @param D The concrete self-type of the dimension.
 */
interface Dimension<D : Dimension<D>> {
  /**
   * Returns the canonical unit of this dimension.
   *
   * @return The canonical unit for conversions.
   */
  val canonicalUnit: QuantityUnit<D>

  /**
   * Determines whether two quantities are equal.
   *
   * @param thisQuantity The first quantity to compare.
   * @param other The object to compare with.
   * @return `true` if they represent the same physical amount.
   */
  fun areEqual(
    thisQuantity: Quantity<D>,
    other: Any?,
  ): Boolean {
    if (thisQuantity === other) return true
    if (other !is Quantity<*>) return false

    if (other.resolution.dimension != this) return false

    @Suppress("UNCHECKED_CAST")
    val otherQuantity = other as Quantity<D>

    return thisQuantity
      .toDouble(canonicalUnit)
      .compareTo(otherQuantity.toDouble(canonicalUnit)) == 0
  }

  /**
   * Calculates the hash code for a quantity.
   *
   * @param quantity The quantity to calculate hash code for.
   * @return The calculated hash code.
   */
  fun calculateHashCode(
    quantity: Quantity<D>,
  ): Int {
    val d = quantity.toDouble(canonicalUnit)
    val normalized =
      when {
        d == 0.0 -> 0.0
        d.isNaN() -> Double.NaN
        else -> d
      }
    return normalized.hashCode()
  }
}
