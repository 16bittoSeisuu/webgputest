package net.japanesehunter.math.test.length

import net.japanesehunter.math.test.ExactMath
import net.japanesehunter.math.test.Quantity
import net.japanesehunter.math.test.QuantityUnit
import net.japanesehunter.math.test.ScaledLong
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
    private fun nanometersPerUnit(
      unit: QuantityUnit<Length>,
    ): Long =
      (unit.thisToCanonicalFactor / nanometers_unit.thisToCanonicalFactor)
        .also { factor ->
          require(factor.isFinite() && factor > 0.0) {
            "The unit factor must be a positive finite number, but was $factor."
          }
        }.roundToLong()

    private fun toNanometers(
      value: Double,
      unit: QuantityUnit<Length>,
    ): Long =
      ScaledLong.scaleToLong(
        value = nanometersPerUnit(unit),
        factor = value,
      )

    private fun toNanometers(
      value: Long,
      unit: QuantityUnit<Length>,
    ): Long =
      ExactMath.multiplyExact(value, nanometersPerUnit(unit))

    override fun Long.times(
      unit: LengthUnit,
    ): LengthQuantity =
      NanometerLength(toNanometers(this, unit))

    override fun Double.times(
      unit: LengthUnit,
    ): LengthQuantity {
      require(isFinite()) {
        "The receiver must be finite, but was $this."
      }
      return NanometerLength(toNanometers(this, unit))
    }
  }

  override fun toDouble(
    unit: QuantityUnit<Length>,
  ): Double {
    val canonicalValue =
      nanometerCount.toDouble() * nanometers_unit.thisToCanonicalFactor
    return canonicalValue / unit.thisToCanonicalFactor
  }

  override fun roundToLong(
    unit: QuantityUnit<Length>,
  ): Long {
    val perUnit = nanometersPerUnit(unit)
    val q = nanometerCount / perUnit
    val r = nanometerCount % perUnit
    if (r == 0L) {
      return q
    }
    val twiceAbsR = abs(r) * 2L
    if (twiceAbsR < perUnit) {
      return q
    }
    return if (nanometerCount > 0L) q + 1L else q - 1L
  }

  override fun toLong(
    unit: QuantityUnit<Length>,
  ): Long {
    val perUnit = nanometersPerUnit(unit)
    return nanometerCount / perUnit
  }

  override fun plus(
    other: Quantity<Length>,
  ): LengthQuantity {
    val otherNm =
      when (other) {
        is NanometerLength -> {
          other.nanometerCount
        }

        else -> {
          toNanometers(other.toDouble(meters), meters)
        }
      }
    return NanometerLength(ExactMath.addExact(nanometerCount, otherNm))
  }

  override fun times(
    scalar: Double,
  ): LengthQuantity {
    require(scalar.isFinite()) {
      "The scalar must be finite, but was $scalar."
    }
    return NanometerLength(ScaledLong.scaleToLong(nanometerCount, scalar))
  }
}
