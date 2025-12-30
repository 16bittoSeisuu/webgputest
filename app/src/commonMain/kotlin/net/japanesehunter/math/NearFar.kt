package net.japanesehunter.math

/**
 * Represents near/far clipping distances with the constraint `0 < near < far`.
 *
 * @author Int16
 */
data class NearFar(
  val near: Length,
  val far: Length,
) {
  init {
    require(near.isPositive) { "near must be > 0, was $near" }
    require(far.isPositive && far > near) {
      "far must be > near, was $far (near=$near)"
    }
  }

  companion object {
    /**
     * Creates a [NearFar] ensuring `0 < near < far`.
     */
    fun from(
      near: Length,
      far: Length,
    ): NearFar =
      NearFar(near, far)

    /**
     * Creates a [NearFar] from numeric values using [unit], ensuring `0 < near < far`.
     */
    fun from(
      near: Double,
      far: Double,
      unit: LengthUnit = LengthUnit.NANOMETER,
    ): NearFar =
      NearFar(Length.from(near, unit), Length.from(far, unit))
  }
}
