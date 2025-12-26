package net.japanesehunter.worldcreate.trait

import net.japanesehunter.math.Acceleration
import net.japanesehunter.math.Speed
import net.japanesehunter.traits.TraitKey

/**
 * Represents a read-only view of a horizontal terminal velocity model.
 * The model maps an acceleration magnitude to a terminal speed.
 *
 * Implementations are not required to be thread-safe.
 */
interface HorizontalTerminalVelocityView {
  /**
   * Calculates the horizontal terminal speed for a given acceleration magnitude.
   *
   * @param acceleration the acceleration magnitude.
   * @return the terminal speed.
   */
  fun terminalSpeedFor(acceleration: Acceleration): Speed
}

/**
 * Represents a horizontal terminal velocity model.
 * The model is expressed as a function from acceleration magnitude to terminal speed.
 *
 * @param rule the mapping from acceleration magnitude to terminal speed.
 */
class HorizontalTerminalVelocity(
  var rule: (Acceleration) -> Speed,
) : HorizontalTerminalVelocityView {
  override fun terminalSpeedFor(acceleration: Acceleration): Speed = rule(acceleration)

  companion object Key : TraitKey<HorizontalTerminalVelocityView, HorizontalTerminalVelocity> by TraitKey()
}
