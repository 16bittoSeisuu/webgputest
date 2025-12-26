package net.japanesehunter.worldcreate.trait

import net.japanesehunter.math.Acceleration3
import net.japanesehunter.math.MutableAcceleration3
import net.japanesehunter.math.copyOf
import net.japanesehunter.math.zero
import net.japanesehunter.traits.TraitKey

/**
 * Represents a read-only view of gravity settings for an entity.
 * The stored value is interpreted as an acceleration vector in world space.
 *
 * Implementations are not required to be thread-safe.
 */
interface GravityAffectedView {
  /**
   * The gravitational acceleration applied to the entity per second.
   */
  val gravity: Acceleration3
}

/**
 * Represents gravity settings for an entity.
 * Entities with this trait are expected to be affected by gravity during simulation.
 *
 * @param initialGravity the initial gravitational acceleration.
 */
class GravityAffected(
  initialGravity: Acceleration3 = Acceleration3.zero,
) : GravityAffectedView {
  override val gravity: MutableAcceleration3 = MutableAcceleration3.copyOf(initialGravity)

  companion object Key : TraitKey<GravityAffectedView, GravityAffected> by TraitKey()
}
