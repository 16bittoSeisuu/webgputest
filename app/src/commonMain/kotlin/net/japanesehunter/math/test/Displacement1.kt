package net.japanesehunter.math.test

/**
 * Represents a one dimensional displacement for a dimension [D].
 *
 * ## Description
 *
 * A displacement is an additive value that can translate a [Point1] of the same dimension.
 * This interface models a single axis displacement and supports scaling.
 *
 * @param D The dimension of the displacement.
 */
interface Displacement1<D : Dimension<D>> {
  /**
   * Returns the displacement along the axis.
   *
   * @return The displacement component.
   */
  val dx: Quantity<D>

  /**
   * Destructures this displacement into its component.
   *
   * @return The displacement along the axis.
   */
  operator fun component1(): Quantity<D> =
    dx

  /**
   * Adds another displacement to this displacement.
   *
   * @param other The displacement to add.
   * @return A displacement whose [dx] equals `this.dx + other.dx`.
   */
  operator fun plus(
    other: Displacement1<D>,
  ): Displacement1<D>

  /**
   * Subtracts another displacement from this displacement.
   *
   * @param other The displacement to subtract.
   * @return A displacement whose [dx] equals `this.dx - other.dx`.
   */
  operator fun minus(
    other: Displacement1<D>,
  ): Displacement1<D> =
    plus(-other)

  /**
   * Scales this displacement by [scalar].
   *
   * @param scalar The scalar multiplier.
   * - Must be finite
  * @return A displacement whose [dx] equals `this.dx * scalar`.
   * @throws IllegalArgumentException
   * - If [scalar] is NaN
   * - If [scalar] is positive infinity or negative infinity
   */
  operator fun times(
    scalar: Double,
  ): Displacement1<D>

  /**
   * Scales this displacement by `1.0 / scalar`.
   *
   * @param scalar The scalar divisor.
   * - Must be finite
   * - Must not be 0.0
  * @return A displacement whose [dx] equals `this.dx / scalar`.
   * @throws IllegalArgumentException
   * - If [scalar] is 0.0
   * - If [scalar] is NaN
   * - If [scalar] is positive infinity or negative infinity
   */
  operator fun div(
    scalar: Double,
  ): Displacement1<D> =
    times(1.0 / scalar)

  /**
   * Returns this displacement itself.
   *
   * @return This instance.
   */
  operator fun unaryPlus(): Displacement1<D> =
    this

  /**
   * Negates this displacement.
   *
   * @return A displacement whose [dx] equals `-this.dx`.
   */
  operator fun unaryMinus(): Displacement1<D> =
    times(-1.0)
}
