package net.japanesehunter.math.test

/**
 * Represents a one dimensional point for a dimension [D].
 *
 * ## Description
 *
 * A point can be translated by adding a [Displacement1].
 * The difference between two points is a [Displacement1].
 *
 * @param D The dimension of the point.
 */
interface Point1<D : Dimension<D>> {
  /**
   * Translates this point by adding a displacement.
   *
   * @param other The displacement to add.
   * @return A point [q] such that `(q - this).dx` equals `other.dx`.
   */
  operator fun plus(
    other: Displacement1<D>,
  ): Point1<D>

  /**
   * Translates this point by subtracting a displacement.
   *
   * @param other The displacement to subtract.
   * @return A point [q] such that `(this - q).dx` equals `other.dx`.
   */
  operator fun minus(
    other: Displacement1<D>,
  ): Point1<D>

  /**
   * Computes the displacement from [other] to this point.
   *
   * @param other The origin point.
   * @return A displacement whose [Displacement1.dx] equals the component needed to move from [other] to this point.
   */
  operator fun minus(
    other: Point1<D>,
  ): Displacement1<D>

  /**
   * Computes the displacement from [other] to this point.
   *
   * @param other The origin point used to compute the relative displacement.
   * @return A displacement whose [Displacement1.dx] equals the component needed to move from [other] to this point.
   */
  infix fun relativeTo(
    other: Point1<D>,
  ): Displacement1<D> =
    this - other
}
