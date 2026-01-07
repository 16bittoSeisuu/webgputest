package net.japanesehunter.math.test

/**
 * Provides arithmetic operations that throw on overflow.
 */
object ExactMath {
  /**
   * Returns the sum of [a] and [b].
   *
   * @throws ArithmeticException
   * - If the sum overflows signed 64 bits.
   */
  fun addExact(
    a: Long,
    b: Long,
  ): Long {
    if (b > 0 && a > Long.MAX_VALUE - b) {
      throw ArithmeticException("Overflow while adding $a and $b.")
    }
    if (b < 0 && a < Long.MIN_VALUE - b) {
      throw ArithmeticException("Overflow while adding $a and $b.")
    }
    return a + b
  }

  /**
   * Returns the product of [a] and [b].
   *
   * @throws ArithmeticException
   * - If the product overflows signed 64 bits.
   */
  fun multiplyExact(
    a: Long,
    b: Long,
  ): Long {
    if (a == 0L || b == 0L) {
      return 0L
    }
    val r = a * b
    if (r / b != a) {
      throw ArithmeticException("Overflow while multiplying $a and $b.")
    }
    return r
  }
}


