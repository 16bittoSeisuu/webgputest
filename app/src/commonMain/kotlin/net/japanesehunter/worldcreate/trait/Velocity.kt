package net.japanesehunter.worldcreate.trait

import net.japanesehunter.math.MutableVelocity3
import net.japanesehunter.math.Velocity3
import net.japanesehunter.traits.TraitKey

/**
 * Represents a read-only view of entity velocity.
 */
interface VelocityView {
  /**
   * The rate of position change expressed as distance per second.
   */
  val value: Velocity3
}

/**
 * The rate of position change for an entity expressed as distance per second.
 *
 * Velocity is modified by physics simulation systems and directly affects the
 * entity's position in subsequent frames.
 */
data class Velocity(
  override val value: MutableVelocity3,
) : VelocityView {
  companion object : TraitKey<VelocityView, Velocity> by TraitKey()
}
