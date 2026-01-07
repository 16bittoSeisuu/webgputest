package net.japanesehunter.math.test

/**
 * Represents a dimensioned quantity.
 *
 * ## Description
 *
 * A quantity can be converted into primitive numeric types in a requested [QuantityUnit].
 * Arithmetic operations are defined within the same dimension type [D].
 *
 * Narrowing conversions from [Long] to smaller integer types keep the lower bits.
 * Floating point to integer conversions truncate toward 0.0 before narrowing.
 *
 * ## Equality
 *
 * Implementations must ensure that two quantities representing the same physical amount
 * are considered equal, regardless of their internal representation derived from the
 * source numeric type.
 *
 * Specifically, `1000L.millimeters` must be equal to `1.0.meters`.
 * Also, `1L.meters` must be equal to `1.0.meters`.
 *
 * @param D The dimension of the quantity.
 */
interface Quantity<D : Dimension<D>> {
  /**
   * Converts this quantity into a [Double] value expressed in [unit].
   *
   * @param unit The unit used to express the returned numeric value.
   * @return The numeric value in [unit].
   * - On overflow, returns positive or negative infinity
   * - On underflow, returns positive or negative zero
   */
  fun toDouble(
    unit: QuantityUnit<D>,
  ): Double

  /**
   * Converts this quantity into a [Float] value expressed in [unit].
   *
   * @param unit The unit used to express the returned numeric value.
   * @return The numeric value in [unit].
   * - On overflow, returns positive or negative infinity
   * - On underflow, returns positive or negative zero
   */
  fun toFloat(
    unit: QuantityUnit<D>,
  ): Float =
    toDouble(unit).toFloat()

  /**
   * Converts this quantity into a [Long] value expressed in [unit].
   *
   * ## Description
   *
   * The conversion truncates toward 0.0 in the requested [unit].
   * If the truncated magnitude does not fit into signed 64 bits, the result keeps the lower
   * 64 bits and discards the upper bits.
   *
   * @param unit The unit used to express the integer value.
   * @return The converted integer value.
   */
  fun toLong(
    unit: QuantityUnit<D>,
  ): Long

  /**
   * Converts this quantity into a [Long] by rounding to the nearest integer in [unit].
   *
   * ## Description
   *
   * Rounding is performed to the nearest integer in the requested [unit].
   * Ties are rounded away from 0.0.
   * Values with a fractional part of 0.5 are rounded toward positive infinity.
   * Values with a fractional part of -0.5 are rounded toward negative infinity.
   *
   * If the rounded magnitude does not fit into signed 64 bits, the result keeps the lower
   * 64 bits and discards the upper bits.
   *
   * @param unit The unit used to express the integer value.
   * @return The rounded integer value.
   */
  fun roundToLong(
    unit: QuantityUnit<D>,
  ): Long

  /**
   * Converts this quantity into an [Int] value expressed in [unit].
   *
   * ## Description
   *
   * This delegates to [toLong] and returns the lower 32 bits of the result.
   * The upper bits are discarded without throwing an exception.
   *
   * @param unit The unit used to express the integer value.
   * @return The converted integer value.
   */
  fun toInt(
    unit: QuantityUnit<D>,
  ): Int =
    toLong(unit).toInt()

  /**
   * Converts this quantity into an [Int] by rounding to the nearest integer in [unit].
   *
   * ## Description
   *
   * This delegates to [roundToLong] and returns the lower 32 bits of the result.
   * The upper bits are discarded without throwing an exception.
   *
   * @param unit The unit used to express the integer value.
   * @return The rounded integer value.
   */
  fun roundToInt(
    unit: QuantityUnit<D>,
  ): Int =
    roundToLong(unit).toInt()

  /**
   * Converts this quantity into a [Short] value expressed in [unit].
   *
   * ## Description
   *
   * This delegates to [toLong] and returns the lower 16 bits of the result.
   * The upper bits are discarded without throwing an exception.
   *
   * @param unit The unit used to express the integer value.
   * @return The converted integer value.
   */
  fun toShort(
    unit: QuantityUnit<D>,
  ): Short =
    toLong(unit).toShort()

  /**
   * Converts this quantity into a [Byte] value expressed in [unit].
   *
   * ## Description
   *
   * This delegates to [toLong] and returns the lower 8 bits of the result.
   * The upper bits are discarded without throwing an exception.
   *
   * @param unit The unit used to express the integer value.
   * @return The converted integer value.
   */
  fun toByte(
    unit: QuantityUnit<D>,
  ): Byte =
    toLong(unit).toByte()

  /**
   * Adds another quantity to this quantity.
   *
   * @param other The quantity to add.
   * @return A quantity representing `this + other`.
   */
  operator fun plus(
    other: Quantity<D>,
  ): Quantity<D>

  /**
   * Subtracts another quantity from this quantity.
   *
   * @param other The quantity to subtract.
   * @return A quantity representing `this - other`.
   */
  operator fun minus(
    other: Quantity<D>,
  ): Quantity<D> =
    plus(-other)

  /**
   * Scales this quantity by [scalar].
   *
   * @param scalar The scalar multiplier.
   * - Must be finite
   * @return A quantity representing this value multiplied by [scalar].
   * @throws IllegalArgumentException
   * - If [scalar] is NaN
   * - If [scalar] is positive infinity or negative infinity
   * @throws ArithmeticException
   * - If scaling overflows internally because the scaled magnitude becomes too large
   */
  operator fun times(
    scalar: Double,
  ): Quantity<D>

  /**
   * Scales this quantity by `1.0 / scalar`.
   *
   * @param scalar The scalar divisor.
   * - Must be finite
   * - Must not be 0.0
   * @return A quantity representing this value divided by [scalar].
   * @throws IllegalArgumentException
   * - If [scalar] is 0.0
   * - If [scalar] is NaN
   * - If [scalar] is positive infinity or negative infinity
   * @throws ArithmeticException
   * - If scaling overflows internally because the scaled magnitude becomes too large (for example, when dividing by a very small nonzero scalar)
   */
  operator fun div(
    scalar: Double,
  ): Quantity<D> =
    times(1.0 / scalar)

  /**
   * Returns this quantity itself.
   *
   * @return This instance.
   */
  operator fun unaryPlus(): Quantity<D> =
    this

  /**
   * Negates this quantity.
   *
   * @return A quantity representing `-this`.
   */
  operator fun unaryMinus(): Quantity<D> =
    times(-1.0)
}
