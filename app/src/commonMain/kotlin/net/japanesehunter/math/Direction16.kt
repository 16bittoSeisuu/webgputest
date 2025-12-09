package net.japanesehunter.math

/**
 * Describes one of the 16 evenly spaced horizontal headings based on yaw.
 */
enum class Direction16(
  val yaw: Angle,
) {
  NORTH(Angle.ZERO),
  NORTH_NORTHEAST(22.5.degrees),
  NORTHEAST(45.0.degrees),
  EAST_NORTHEAST(67.5.degrees),
  EAST(Angle.HALF_PI),
  EAST_SOUTHEAST(112.5.degrees),
  SOUTHEAST(135.0.degrees),
  SOUTH_SOUTHEAST(157.5.degrees),
  SOUTH(Angle.PI),
  SOUTH_SOUTHWEST(202.5.degrees),
  SOUTHWEST(225.0.degrees),
  WEST_SOUTHWEST(247.5.degrees),
  WEST(270.0.degrees),
  WEST_NORTHWEST(292.5.degrees),
  NORTHWEST(315.0.degrees),
  NORTH_NORTHWEST(337.5.degrees),
  ;

  companion object {
    private val SEGMENT_COUNT = entries.size
    private val SECTOR_SIZE_DEGREES = Angle.TAU / SEGMENT_COUNT
    private val HALF_SECTOR_DEGREES = SECTOR_SIZE_DEGREES / 2

    /**
     * Returns the nearest heading for the supplied [yaw].
     */
    fun from(yaw: Angle): Direction16 {
      val normalizedYaw = yaw % Angle.PI + Angle.TAU
      val index = ((normalizedYaw + HALF_SECTOR_DEGREES) / SECTOR_SIZE_DEGREES).toInt() % SEGMENT_COUNT
      return entries[index]
    }
  }
}
