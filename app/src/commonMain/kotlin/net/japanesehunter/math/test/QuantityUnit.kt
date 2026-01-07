package net.japanesehunter.math.test

import net.japanesehunter.math.test.QuantityUnit.Companion.base
import kotlin.math.abs
import kotlin.math.roundToLong
import kotlin.math.ulp


/**
 * Represents a unit of a [Dimension].
 *
 * ## Description
 *
 * A [QuantityUnit] defines a scale relative to the dimension's canonical unit.
 * Conversion to the canonical unit is performed by multiplying a value in this unit
 * by [thisToCanonicalFactor].
 *
 * Use [base] to create a unit with a factor of 1.0.
 * Use [derive] to create derived units from an existing unit.
 *
 * @param D The dimension of the unit.
 * @property dimension The dimension this unit belongs to.
 * @property thisToCanonicalFactor The factor to convert a value in this unit to the canonical unit.
 * @property name The human-readable unit name.
 * @property symbol The unit symbol used for formatting.
 */
@ConsistentCopyVisibility
data class QuantityUnit<D : Dimension<D>> private constructor(
  val dimension: D,
  val thisToCanonicalFactor: Double,
  val name: String,
  val symbol: String,
) {
  companion object {
    /**
     * Creates a base unit for [dimension].
     *
     * ## Description
     *
     * The returned unit has a [QuantityUnit.thisToCanonicalFactor] of 1.0.
     * This is typically used as the canonical unit of the dimension.
     *
     * @param dimension The dimension the unit belongs to.
     * @param name The unit name.
     * @param symbol The unit symbol.
     * @return The created base unit.
     */
    fun <D : Dimension<D>> base(
      dimension: D,
      name: String,
      symbol: String,
    ): QuantityUnit<D> =
      QuantityUnit(dimension, 1.0, name, symbol)
  }

  /**
   * Creates a unit derived from this unit.
   *
   * ## Description
   *
   * [newToThisFactor] specifies the factor to convert a value in the new unit into this unit.
   * The derived unit's canonical conversion factor becomes
   * `thisToCanonicalFactor * newToThisFactor`.
   *
   * ## Example
   * - When this unit is meters, passing 1e3 creates kilometers.
   * - When this unit is seconds, passing 60.0 creates minutes.
   *
   * @param newToThisFactor The factor to convert the new unit to this unit.
   * - Must be a positive finite number (> 0.0)
   * @param name The new unit name.
   * @param symbol The new unit symbol.
   * @return The derived unit.
   * @throws IllegalArgumentException
   * - If [newToThisFactor] is NaN or infinite.
   * - If [newToThisFactor] is zero or negative.
   */
  fun derive(
    newToThisFactor: Double,
    name: String,
    symbol: String,
  ): QuantityUnit<D> {
    require(newToThisFactor.isFinite() && newToThisFactor > 0.0) {
      "The conversion factor must be a positive finite number, but was $newToThisFactor."
    }
    return copy(
      name = name,
      symbol = symbol,
      thisToCanonicalFactor = thisToCanonicalFactor * newToThisFactor,
    )
  }

  // TODO: KDoc
  infix fun per(
    other: QuantityUnit<D>,
  ): Double {
    val ret = other.thisToCanonicalFactor / thisToCanonicalFactor
    if (ret.isInfinite()) {
      throw ArithmeticException(
        "The derived unit's canonical conversion factor overflows Double.",
      )
    }
    val nearest =
      ret
        .roundToLong()
        .toDouble()
    if (abs(ret - nearest) <= nearest.ulp * 2.0) {
      return nearest
    }
    return ret
  }

  override fun toString(): String =
    "QuantityUnit(1$symbol($name)=$thisToCanonicalFactor$dimension)"
}
