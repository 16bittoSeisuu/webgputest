package net.japanesehunter.math

/**
 * Describes one of the 16 evenly spaced horizontal headings based on yaw.
 */
enum class Direction16(val yaw: Angle) {
  North(Angle.ZERO),
  NorthNortheast(22.5.degrees),
  Northeast(45.0.degrees),
  EastNortheast(67.5.degrees),
  East(Angle.HALF_PI),
  EastSoutheast(112.5.degrees),
  Southeast(135.0.degrees),
  SouthSoutheast(157.5.degrees),
  South(Angle.PI),
  SouthSouthwest(202.5.degrees),
  Southwest(225.0.degrees),
  WestSouthwest(247.5.degrees),
  West(270.0.degrees),
  WestNorthwest(292.5.degrees),
  Northwest(315.0.degrees),
  NorthNorthwest(337.5.degrees),
  ;

  companion object {
    private val SEGMENT_COUNT = entries.size
    private val SECTOR_SIZE_DEGREES = Angle.TAU / SEGMENT_COUNT
    private val HALF_SECTOR_DEGREES = SECTOR_SIZE_DEGREES / 2

    /**
     * Returns the nearest heading for the supplied [yaw].
     */
    fun from(
      yaw: Angle,
    ): Direction16 {
      val normalizedYaw = yaw % Angle.PI + Angle.TAU
      val index =
        ((normalizedYaw + HALF_SECTOR_DEGREES) / SECTOR_SIZE_DEGREES).toInt() %
          SEGMENT_COUNT
      return entries[index]
    }
  }
}
