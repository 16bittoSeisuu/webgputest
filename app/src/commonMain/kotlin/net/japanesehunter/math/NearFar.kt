package net.japanesehunter.math

/**
 * Represents near/far clipping distances with the constraint `0 < near < far <= +∞`.
 *
 * @author Int16
 */
data class NearFar(
  val near: Double,
  val far: Double,
) {
  init {
    require(near.isFinite() && near > 0.0) { "near must be finite and > 0.0, was $near" }
    val farValid = far == Double.POSITIVE_INFINITY || (far.isFinite() && far > near)
    require(farValid) { "far must be > near and finite or +∞, was $far (near=$near)" }
  }

  companion object {
    /**
     * Creates a [NearFar] ensuring `0 < near < far <= +∞`.
     */
    fun from(
      near: Double,
      far: Double,
    ): NearFar = NearFar(near, far)
  }
}
