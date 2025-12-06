package net.japanesehunter.math

/**
 * Field of view represented as an [Angle] with the constraint `0 < fov < π`.
 *
 * @author Int16
 */
value class Fov internal constructor(
  val angle: Angle,
) {
  init {
    require(angle > Angle.ZERO && angle < Angle.PI) { "Fov must satisfy 0 < angle < PI, was $angle" }
  }

  /**
   * Returns this FOV as an [Angle] in the requested [unit].
   */
  fun toDouble(unit: AngleUnit = AngleUnit.RADIAN): Double = angle.toDouble(unit)

  override fun toString(): String = "Fov(angle=$angle)"

  companion object {
    /**
     * Creates a [Fov] from an [Angle]. Requires `0 < angle < π`.
     */
    fun from(angle: Angle): Fov = Fov(angle)

    /**
     * Creates a [Fov] from radians. Requires `0 < radians < π`.
     */
    fun fromRadians(radians: Double): Fov = from(Angle.from(radians, AngleUnit.RADIAN))

    /**
     * Creates a [Fov] from degrees. Requires `0 < degrees < 180`.
     */
    fun fromDegrees(degrees: Double): Fov = from(Angle.from(degrees, AngleUnit.DEGREE))
  }
}

/**
 * Creates a [Fov] from this [Angle]. Requires `0 < angle < π`.
 */
val Angle.fov: Fov
  get() = Fov.from(this)
