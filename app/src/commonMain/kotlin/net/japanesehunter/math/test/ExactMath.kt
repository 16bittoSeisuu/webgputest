@file:Suppress("NOTHING_TO_INLINE")

package net.japanesehunter.math.test

import kotlin.math.sign

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
  ): Long {
    if (other == Long.MIN_VALUE) {
      if (0 <= this) {
        throw ArithmeticException(
          "Overflow while subtracting $this from $other.",
        )
      }
      return Long.MAX_VALUE + (this + 1L)
    }
    return this plusExact -other
  }

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
    if (this == 0L || other == 0L) {
      return 0L
    }
    if (this == -1L && other == Long.MIN_VALUE) {
      throw ArithmeticException("Overflow while multiplying $this and $other.")
    }
    if (other == -1L && this == Long.MIN_VALUE) {
      throw ArithmeticException("Overflow while multiplying $this and $other.")
    }
    val result = this * other
    if (result / other != this) {
      throw ArithmeticException("Overflow while multiplying $this and $other.")
    }
    return result
  }

  /**
   * Negates this [Long] value exactly, throwing an exception if the result overflows the range of [Long].
   *
   * @return The negated [Long] value.
   * @throws ArithmeticException If the negation results in overflow.
   */
  inline fun Long.negateExact(): Long {
    if (this == Long.MIN_VALUE) {
      throw ArithmeticException("Overflow while negating $this.")
    }
    return -this
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
    // Fast path for integer scales
    val longScale = other.toLong()
    if (longScale.toDouble() == other) {
      return this timesExact longScale
    }
    val parsed = ParsedDecimal.parse(other.toString())
    if (parsed.significand == 0UL) {
      return 0L
    }

    val resultSign = this.sign * parsed.sign
    val absReceiver = absAsULong(this)

    var value =
      UInt128
        .fromULong(absReceiver)
        .times(parsed.significand)
        ?: throw ArithmeticException("Overflow while scaling $this by $other.")

    if (0 < parsed.exponent10) {
      repeat(parsed.exponent10) {
        value =
          value
            .times10()
            ?: throw ArithmeticException(
              "Overflow while scaling $this by $other.",
            )
      }
    } else if (parsed.exponent10 < 0) {
      repeat(-parsed.exponent10) {
        value = value.div10()
      }
    }

    return toLongExact(
      absoluteValue = value,
      sign = resultSign,
      receiver = this,
      scale = other,
    )
  }

  @PublishedApi
  internal fun absAsULong(
    value: Long,
  ): ULong =
    if (0L <= value) {
      value.toULong()
    } else if (value == Long.MIN_VALUE) {
      1UL shl 63
    } else {
      (-value).toULong()
    }

  @PublishedApi
  internal fun toLongExact(
    absoluteValue: UInt128,
    sign: Int,
    receiver: Long,
    scale: Double,
  ): Long {
    if (0 < sign) {
      if (absoluteValue.isGreaterThanULong(
          Long.MAX_VALUE
            .toULong(),
        )
      ) {
        throw ArithmeticException(
          "Overflow while scaling $receiver by $scale.",
        )
      }
      return absoluteValue
        .toULong()
        .toLong()
    }

    val minValueAbs = (1UL shl 63)
    if (absoluteValue.isGreaterThanULong(minValueAbs)) {
      throw ArithmeticException("Overflow while scaling $receiver by $scale.")
    }
    if (absoluteValue.toULong() == minValueAbs) {
      return Long.MIN_VALUE
    }
    return -absoluteValue
      .toULong()
      .toLong()
  }

  @PublishedApi
  internal data class ParsedDecimal(
    val sign: Int,
    val significand: ULong,
    val exponent10: Int,
  ) {
    companion object {
      fun parse(
        value: String,
      ): ParsedDecimal {
        var text = value
        var sign = 1
        if (text.startsWith("-")) {
          sign = -1
          text = text.drop(1)
        } else if (text.startsWith("+")) {
          text = text.drop(1)
        }

        val eIndex =
          text
            .indexOf('e')
            .let { lower ->
              if (lower != -1) {
                lower
              } else {
                text.indexOf('E')
              }
            }

        val base =
          if (eIndex != -1) {
            text.take(eIndex)
          } else {
            text
          }
        val expFromE =
          if (eIndex != -1) {
            text
              .substring(eIndex + 1)
              .toInt()
          } else {
            0
          }

        val dotIndex = base.indexOf('.')
        val decimalPlaces =
          if (dotIndex != -1) {
            base.length - dotIndex - 1
          } else {
            0
          }

        val digits =
          if (dotIndex != -1) {
            base.removeRange(dotIndex, dotIndex + 1)
          } else {
            base
          }

        val significand =
          digits.fold(0UL) { acc, c ->
            val digit = c.code - '0'.code
            require(digit in 0..9) {
              "Invalid decimal digit in scale: $value"
            }
            acc * 10UL + digit.toULong()
          }

        return ParsedDecimal(
          sign = sign,
          significand = significand,
          exponent10 = expFromE - decimalPlaces,
        )
      }
    }
  }

  @PublishedApi
  internal data class UInt128(
    val w3: UInt,
    val w2: UInt,
    val w1: UInt,
    val w0: UInt,
  ) {
    fun isGreaterThanULong(
      limit: ULong,
    ): Boolean {
      if (w3 != 0u || w2 != 0u) {
        return true
      }
      val limitW1 = (limit shr 32).toUInt()
      val limitW0 = (limit and 0xffffffffUL).toUInt()
      if (limitW1 < w1) {
        return true
      }
      if (w1 < limitW1) {
        return false
      }
      return limitW0 < w0
    }

    fun toULong(): ULong {
      require(w3 == 0u && w2 == 0u) { "Value does not fit into ULong." }
      return (w1.toULong() shl 32) or w0.toULong()
    }

    fun times10(): UInt128? =
      timesSmall(10u)

    fun div10(): UInt128 =
      divSmall(10u)

    operator fun times(
      other: ULong,
    ): UInt128? {
      val otherW1 = (other shr 32).toUInt()
      val otherW0 = (other and 0xffffffffUL).toUInt()
      if (otherW1 == 0u) {
        return timesSmall(otherW0)
      }
      val left = timesSmall(otherW0) ?: return null
      val rightBase = timesSmall(otherW1) ?: return null
      val right = rightBase.shiftLeft32() ?: return null
      return left.plus(right)
    }

    fun plus(
      other: UInt128,
    ): UInt128? {
      val sum0 =
        w0.toULong() +
          other.w0
            .toULong()
      val carry0 = sum0 shr 32
      val sum1 =
        w1.toULong() +
          other.w1
            .toULong() +
          carry0
      val carry1 = sum1 shr 32
      val sum2 =
        w2.toULong() +
          other.w2
            .toULong() +
          carry1
      val carry2 = sum2 shr 32
      val sum3 =
        w3.toULong() +
          other.w3
            .toULong() +
          carry2
      if (0x1_0000_0000UL <= sum3) {
        return null
      }
      return UInt128(
        w3 = sum3.toUInt(),
        w2 = sum2.toUInt(),
        w1 = sum1.toUInt(),
        w0 = sum0.toUInt(),
      )
    }

    private fun shiftLeft32(): UInt128? {
      if (w3 != 0u) {
        return null
      }
      return UInt128(
        w3 = w2,
        w2 = w1,
        w1 = w0,
        w0 = 0u,
      )
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    private fun timesSmall(
      factor: UInt,
    ): UInt128? {
      var carry = 0UL

      val w = uintArrayOf(w0, w1, w2, w3)
      val next =
        UIntArray(4) {
          val prod = w[it].toULong() * factor.toULong() + carry
          carry = prod shr 32
          (prod and 0xffffffffUL).toUInt()
        }
      if (carry != 0UL) {
        return null
      }
      return UInt128(
        w3 = next[3],
        w2 = next[2],
        w1 = next[1],
        w0 = next[0],
      )
    }

    private fun divSmall(
      divisor: UInt,
    ): UInt128 {
      var remainder = 0UL

      fun step(
        word: UInt,
      ): UInt {
        val current = (remainder shl 32) + word.toULong()
        val q = current / divisor.toULong()
        remainder = current % divisor.toULong()
        return q.toUInt()
      }

      val q3 = step(w3)
      val q2 = step(w2)
      val q1 = step(w1)
      val q0 = step(w0)

      return UInt128(
        w3 = q3,
        w2 = q2,
        w1 = q1,
        w0 = q0,
      )
    }

    companion object {
      fun fromULong(
        value: ULong,
      ): UInt128 =
        UInt128(
          w3 = 0u,
          w2 = 0u,
          w1 = (value shr 32).toUInt(),
          w0 = (value and 0xffffffffUL).toUInt(),
        )
    }
  }
}
