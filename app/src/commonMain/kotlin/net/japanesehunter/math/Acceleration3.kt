package net.japanesehunter.math

/**
 * Represents a 3D acceleration vector with components along the x, y, and z axes.
 * Each component is expressed as an [Acceleration] value.
 * This class is immutable.
 *
 * @param ax The acceleration along the x-axis.
 * @param ay The acceleration along the y-axis.
 * @param az The acceleration along the z-axis.
 * @author Int16
 */
data class Acceleration3(
  val ax: Acceleration,
  val ay: Acceleration,
  val az: Acceleration,
) {
  /**
   * Returns the negated acceleration vector.
   *
   * @throws ArithmeticException If negation overflows [Long] for any component.
   */
  operator fun unaryMinus(): Acceleration3 = Acceleration3(-ax, -ay, -az)

  /**
   * Adds another [Acceleration3] to this vector.
   *
   * @param other The acceleration vector to add.
   * @return The resulting acceleration vector.
   * @throws ArithmeticException If the addition overflows [Long] for any component.
   */
  operator fun plus(other: Acceleration3): Acceleration3 = Acceleration3(ax + other.ax, ay + other.ay, az + other.az)

  /**
   * Subtracts another [Acceleration3] from this vector.
   *
   * @param other The acceleration vector to subtract.
   * @return The resulting acceleration vector.
   * @throws ArithmeticException If the subtraction overflows [Long] for any component.
   */
  operator fun minus(other: Acceleration3): Acceleration3 = Acceleration3(ax - other.ax, ay - other.ay, az - other.az)

  /**
   * Multiplies this acceleration vector by a scalar [Long] factor.
   *
   * @param factor The scaling factor.
   * @return The scaled acceleration vector.
   * @throws ArithmeticException If the multiplication overflows [Long] for any component.
   */
  operator fun times(factor: Long): Acceleration3 = Acceleration3(ax * factor, ay * factor, az * factor)

  /**
   * Multiplies this acceleration vector by a scalar [Double] factor.
   *
   * @param factor The scaling factor.
   * @return The scaled acceleration vector.
   * @throws ArithmeticException If the multiplication overflows [Long] for any component.
   * @throws IllegalArgumentException If [factor] is not finite.
   */
  operator fun times(factor: Double): Acceleration3 = Acceleration3(ax * factor, ay * factor, az * factor)

  /**
   * Divides this acceleration vector by a scalar [Long] divisor.
   *
   * @param divisor The divisor.
   * @return The divided acceleration vector.
   * @throws ArithmeticException If [divisor] is zero.
   */
  operator fun div(divisor: Long): Acceleration3 = Acceleration3(ax / divisor, ay / divisor, az / divisor)

  /**
   * Divides this acceleration vector by a scalar [Double] divisor.
   *
   * @param divisor The divisor.
   * @return The divided acceleration vector.
   * @throws ArithmeticException If the division overflows [Long] for any component.
   * @throws IllegalArgumentException If [divisor] is not finite or is zero.
   */
  operator fun div(divisor: Double): Acceleration3 = Acceleration3(ax / divisor, ay / divisor, az / divisor)

  override fun toString(): String = "Acceleration3(ax=$ax, ay=$ay, az=$az)"

  companion object {
    /**
     * The zero acceleration vector.
     */
    val ZERO: Acceleration3 = Acceleration3(Acceleration.ZERO, Acceleration.ZERO, Acceleration.ZERO)
  }
}
