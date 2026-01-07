package net.japanesehunter.math.test.length

import net.japanesehunter.math.test.Quantity
import net.japanesehunter.math.test.QuantityUnit

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
  override fun toDouble(
    unit: QuantityUnit<Length>,
  ): Double {
    TODO("Not yet implemented")
  }

  override fun roundToLong(
    unit: QuantityUnit<Length>,
  ): Long {
    TODO("Not yet implemented")
  }

  override fun toLong(
    unit: QuantityUnit<Length>,
  ): Long {
    TODO("Not yet implemented")
  }

  override fun plus(
    other: Quantity<Length>,
  ): LengthQuantity {
    TODO("Not yet implemented")
  }

  override fun times(
    scalar: Double,
  ): LengthQuantity {
    TODO("Not yet implemented")
  }
}


