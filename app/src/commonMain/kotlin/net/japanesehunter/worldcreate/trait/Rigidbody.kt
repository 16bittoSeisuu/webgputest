package net.japanesehunter.worldcreate.trait

import net.japanesehunter.math.Acceleration
import net.japanesehunter.traits.TraitKey

/**
 * Represents a read-only view of rigid body physics properties.
 */
interface RigidbodyView {
  /**
   * The gravitational acceleration applied to the entity per second.
   */
  val gravity: Acceleration

  /**
   * The air resistance coefficient applied to velocity per second.
   */
  val drag: Double
}

/**
 * The rigid body physics properties that determine how an entity responds to gravity and drag.
 *
 * Entities with this trait are simulated by the physics system and experience gravitational
 * acceleration and air resistance. These properties can be modified at runtime to simulate
 * environmental changes such as entering water or applying buffs.
 *
 * @param gravity the gravitational acceleration applied to the entity per second.
 * @param initialDrag the initial air resistance coefficient applied to velocity per second.
 *   Higher values cause faster deceleration.
 *   range: initialDrag >= 0.0
 */
class Rigidbody(
  override var gravity: Acceleration,
  initialDrag: Double,
) : RigidbodyView {
  /**
   * The air resistance coefficient applied to velocity per second.
   *
   * Higher values cause faster deceleration. Must be finite and non-negative.
   */
  override var drag: Double = validateDrag(initialDrag)
    set(value) {
      field = validateDrag(value)
    }

  private fun validateDrag(
    value: Double,
  ): Double {
    require(value.isFinite() && value >= 0.0) {
      "drag must be finite and non-negative: $value"
    }
    return value
  }

  companion object : TraitKey<RigidbodyView, Rigidbody> by TraitKey()
}
