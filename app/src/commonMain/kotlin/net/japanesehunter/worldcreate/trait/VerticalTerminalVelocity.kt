package net.japanesehunter.worldcreate.trait

import net.japanesehunter.math.Acceleration
import net.japanesehunter.math.Speed
import net.japanesehunter.traits.TraitKey

/**
 * Represents a read-only view of a vertical terminal velocity model.
 * The model maps an acceleration magnitude to a terminal speed.
 *
 * Implementations are not required to be thread-safe.
 */
interface VerticalTerminalVelocityView {
  /**
   * Calculates the vertical terminal speed for a given acceleration magnitude.
   *
   * @param acceleration the acceleration magnitude.
   * @return the terminal speed.
   */
  fun terminalSpeedFor(
    acceleration: Acceleration,
  ): Speed
}

/**
 * Represents a vertical terminal velocity model.
 * The model is expressed as a function from acceleration magnitude to terminal speed.
 *
 * @param rule the mapping from acceleration magnitude to terminal speed.
 */
class VerticalTerminalVelocity(var rule: (Acceleration) -> Speed) :
  VerticalTerminalVelocityView {
  override fun terminalSpeedFor(
    acceleration: Acceleration,
  ): Speed =
    rule(acceleration)

  companion object Key :
    TraitKey<VerticalTerminalVelocityView, VerticalTerminalVelocity>
    by TraitKey()
}
