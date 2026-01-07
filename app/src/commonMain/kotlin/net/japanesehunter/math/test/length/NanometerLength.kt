package net.japanesehunter.math.test.length

import net.japanesehunter.math.test.Quantity
import net.japanesehunter.math.test.QuantityUnit
import kotlin.jvm.JvmInline

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
 * @property nanometers The length value expressed in nanometers.
 */
value class NanometerLength internal constructor(
  private val nanometers: Long,
) : LengthQuantity {
  companion object : LengthProvider {
    private const val NANOMETERS_PER_METER: Long = 1_000_000_000L

    private fun requireFinite(
      value: Double,
    ) {
      require(value.isFinite()) {
        "The receiver must be finite."
      }
    }

    private fun nanometersPerUnit(
      unit: QuantityUnit<Length>,
    ): Long =
      roundToLongAwayFromZero(
        unit.thisToCanonicalFactor * NANOMETERS_PER_METER.toDouble(),
      )

    private fun checkedAdd(
      a: Long,
      b: Long,
    ): Long {
      if (b > 0 && a > Long.MAX_VALUE - b) {
        throw ArithmeticException("Overflow.")
      }
      if (b < 0 && a < Long.MIN_VALUE - b) {
        throw ArithmeticException("Overflow.")
      }
      return a + b
    }

    private fun checkedMultiply(
      a: Long,
      b: Long,
    ): Long {
      if (a == 0L || b == 0L) {
        return 0L
      }
      val r = a * b
      if (r / b != a) {
        throw ArithmeticException("Overflow.")
      }
      return r
    }

    private fun roundToLongAwayFromZero(
      value: Double,
    ): Long {
      if (!value.isFinite()) {
        throw IllegalArgumentException("The value must be finite.")
      }
      if (value >
        Long.MAX_VALUE
          .toDouble() ||
        value <
        Long.MIN_VALUE
          .toDouble()
      ) {
        throw ArithmeticException("Overflow.")
      }
      val truncated = value.toLong()
      val frac = value - truncated.toDouble()
      if (abs(frac) < 0.5) {
        return truncated
      }
      if (abs(frac) > 0.5) {
        return if (value > 0.0) truncated + 1 else truncated - 1
      }
      return if (value > 0.0) truncated + 1 else truncated - 1
    }

    override fun Long.times(
      unit: LengthUnit,
    ): LengthQuantity {
      val perUnit = nanometersPerUnit(unit)
      val nm = checkedMultiply(this, perUnit)
      return NanometerLength(nm)
    }

    override fun Double.times(
      unit: LengthUnit,
    ): LengthQuantity {
      requireFinite(this)
      val perUnit = nanometersPerUnit(unit).toDouble()
      val nm = roundToLongAwayFromZero(this * perUnit)
      return NanometerLength(nm)
    }
  }

  override fun toDouble(
    unit: QuantityUnit<Length>,
  ): Double {
    val metersValue = nanometers.toDouble() / NANOMETERS_PER_METER.toDouble()
    return metersValue / unit.thisToCanonicalFactor
  }

  override fun roundToLong(
    unit: QuantityUnit<Length>,
  ): Long {
    val perUnit = nanometersPerUnit(unit)
    val q = nanometers / perUnit
    val r = nanometers % perUnit
    if (r == 0L) {
      return q
    }
    val twiceAbsR = abs(r) * 2L
    if (twiceAbsR < perUnit) {
      return q
    }
    return if (nanometers > 0L) q + 1L else q - 1L
  }

  override fun toLong(
    unit: QuantityUnit<Length>,
  ): Long {
    val perUnit = nanometersPerUnit(unit)
    return nanometers / perUnit
  }

  override fun plus(
    other: Quantity<Length>,
  ): LengthQuantity {
    val otherNm =
      when (other) {
        is NanometerLength -> {
          other.nanometers
        }

        else -> {
          roundToLongAwayFromZero(
            other.toDouble(meters) * NANOMETERS_PER_METER.toDouble(),
          )
        }
      }
    return NanometerLength(checkedAdd(nanometers, otherNm))
  }

  override fun times(
    scalar: Double,
  ): LengthQuantity {
    require(scalar.isFinite()) {
      "The scalar must be finite."
    }
    val scaled = nanometers.toDouble() * scalar
    return NanometerLength(roundToLongAwayFromZero(scaled))
  }
}
