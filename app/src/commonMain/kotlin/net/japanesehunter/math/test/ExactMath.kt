@file:Suppress("NOTHING_TO_INLINE")

package net.japanesehunter.math.test

/**
 * Provides arithmetic operations that throw on overflow.
 */
object ExactMath {
  /**
   * Adds two `Long` values and returns the result. Throws an `ArithmeticException`
   * if the result overflows a `Long`.
   *
   * @param other The addend to be added to this `Long` value.
   * @return The sum of this `Long` value and [other].
   * @throws ArithmeticException If the result overflows a `Long`.
   */
  inline infix fun Long.plusExact(
    other: Long,
  ): Long {
    if (other > 0 && this > Long.MAX_VALUE - other) {
      throw ArithmeticException("Overflow while adding $this and $other.")
    }
    if (other < 0 && this < Long.MIN_VALUE - other) {
      throw ArithmeticException("Overflow while adding $this and $other.")
    }
    return this + other
  }

  /**
   * Subtracts the specified `Long` value from this `Long` value and returns the result.
   * Throws an `ArithmeticException` if the result overflows a `Long`.
   *
   * @param other The subtrahend to be subtracted from this `Long` value.
   * @return The difference of this `Long` value and [other].
   * @throws ArithmeticException If the result overflows a `Long`.
   */
  infix fun Long.minusExact(
    other: Long,
  ): Long =
    plusExact(
      -other,
    )

  /**
   * Multiplies this [Long] value by the given [other] [Long] value exactly,
   * throwing an exception if an overflow occurs.
   *
   * @param other The other [Long] value to be multiplied with this value.
   * @return The result of the multiplication as a [Long].
   * @throws ArithmeticException If the result overflows the range of [Long].
   */
  inline infix fun Long.timesExact(
    other: Long,
  ): Long {
    TODO()
  }

  /**
   * Scales this [Long] value by the specified [Double] scale factor, returning the result as a [Long].
   *
   * The scaling operation is performed exactly, without rounding, and the result is truncated toward zero.
   * Throws an [ArithmeticException] if the scaled result overflows the range of a [Long].
   *
   * @param other The scale factor by which to scale the [Long] value. Must be a finite [Double].
   * @return The scaled [Long] value. The result is truncated toward zero.
   * @throws ArithmeticException If the scaled result overflows the range of a [Long].
   * @throws IllegalArgumentException If the scale factor is not finite.
   */
  inline infix fun Long.scaleExact(
    other: Double,
  ): Long {
    require(other.isFinite()) { "Scale must be finite but was: $other" }
    if (this == 0L || other == 0.0) {
      return 0L
    }
    TODO()
  }
}
