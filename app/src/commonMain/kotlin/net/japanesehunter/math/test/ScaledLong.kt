package net.japanesehunter.math.test

/**
 * Provides overflow-safe scaling utilities for [Long].
 */
object ScaledLong {
  private const val DOUBLE_SIGNED_LONG_UPPER_EXCLUSIVE: Double =
    9.223372036854776E18

  /**
   * Scales [value] by [factor] and returns the result as a [Long].
   *
   * ## Description
   *
   * The result is computed as `value * factor` and truncated toward zero.
   *
   * @throws IllegalArgumentException
   * - If [factor] is NaN or infinite.
   * @throws ArithmeticException
   * - If the scaled value does not fit into signed 64 bits.
   */
  fun scaleToLong(
    value: Long,
    factor: Double,
  ): Long {
    require(factor.isFinite()) {
      "The factor must be finite, but was $factor."
    }
    if (value == 0L || factor == 0.0) {
      return 0L
    }

    val scaled = value.toDouble() * factor
    if (!scaled.isFinite()) {
      throw ArithmeticException("Overflow while scaling $value by $factor.")
    }

    val longMinInclusive =
      Long.MIN_VALUE
        .toDouble()
    if (scaled < longMinInclusive ||
      scaled >= DOUBLE_SIGNED_LONG_UPPER_EXCLUSIVE
    ) {
      throw ArithmeticException("Overflow while scaling $value by $factor.")
    }

    return scaled.toLong()
  }
}


