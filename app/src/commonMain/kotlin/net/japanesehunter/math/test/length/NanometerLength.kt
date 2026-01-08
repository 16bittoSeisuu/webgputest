package net.japanesehunter.math.test.length

import net.japanesehunter.math.test.ExactMath.descaleExact
import net.japanesehunter.math.test.ExactMath.minusExact
import net.japanesehunter.math.test.ExactMath.negateExact
import net.japanesehunter.math.test.ExactMath.plusExact
import net.japanesehunter.math.test.ExactMath.scaleExact
import net.japanesehunter.math.test.Quantity
import net.japanesehunter.math.test.QuantityUnit
import kotlin.math.abs
import kotlin.math.roundToLong
import net.japanesehunter.math.test.length.nanometers as nanometers_unit

/**
 * Fixed-point implementation of [LengthQuantity] backed by a signed nanometer count in a [Long].
 *
 * ## Description
 *
 * The internal representation stores the physical amount in nanometers.
 * All conversions and arithmetic are defined in terms of this fixed scale.
 *
 * Values constructed from floating point inputs may be rounded internally.
 * Converting such values back to the source unit is not guaranteed to reproduce the input.
 *
 * @property nanometerCount The length value expressed in nanometers.
 */
value class NanometerLength private constructor(
  private val nanometerCount: Long,
) : LengthQuantity {
  companion object : LengthProvider {
    override fun Long.times(
      unit: LengthUnit,
    ): LengthQuantity =
      NanometerLength(this scaleExact (nanometers_unit per unit))

    override fun Double.times(
      unit: LengthUnit,
    ): LengthQuantity {
      require(isFinite()) {
        "The receiver must be finite, but was $this."
      }
      return NanometerLength((this * (nanometers_unit per unit)).roundToLong())
    }
  }

  override fun toDouble(
    unit: QuantityUnit<Length>,
  ): Double =
    nanometerCount.toDouble() * (unit per nanometers_unit)

  override fun roundToLong(
    unit: QuantityUnit<Length>,
  ): Long {
    val perUnit =
      (nanometers_unit per unit)
    val q = nanometerCount scaleExact (unit per nanometers_unit)
    val r = nanometerCount % perUnit
    if (r == 0.0) {
      return q
    }
    val twiceAbsR = abs(r) * 2.0
    if (twiceAbsR < perUnit) {
      return q
    }
    return if (nanometerCount > 0L) q + 1L else q - 1L
  }

  override fun toLong(
    unit: QuantityUnit<Length>,
  ): Long {
    if (unit == nanometers_unit) {
      return nanometerCount
    }
    return nanometerCount scaleExact (unit per nanometers_unit)
  }

  override fun plus(
    other: Quantity<Length>,
  ): LengthQuantity =
    NanometerLength(nanometerCount plusExact other.toLong(nanometers_unit))

  override fun minus(
    other: Quantity<Length>,
  ): LengthQuantity =
    NanometerLength(nanometerCount minusExact other.toLong(nanometers_unit))

  override fun times(
    scalar: Double,
  ): LengthQuantity =
    NanometerLength(nanometerCount scaleExact scalar)

  override fun div(
    scalar: Double,
  ): LengthQuantity =
    NanometerLength(nanometerCount descaleExact scalar)

  override fun unaryMinus(): LengthQuantity =
    NanometerLength(nanometerCount.negateExact())
}
