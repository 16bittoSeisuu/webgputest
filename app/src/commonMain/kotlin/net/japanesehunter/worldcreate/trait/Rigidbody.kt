package net.japanesehunter.worldcreate.trait

import net.japanesehunter.math.Acceleration

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
  var gravity: Acceleration,
  initialDrag: Double,
) {
  /**
   * The air resistance coefficient applied to velocity per second.
   *
   * Higher values cause faster deceleration. Must be finite and non-negative.
   */
  var drag: Double = validateDrag(initialDrag)
    set(value) {
      field = validateDrag(value)
    }

  private fun validateDrag(value: Double): Double {
    require(value.isFinite() && value >= 0.0) { "drag must be finite and non-negative: $value" }
    return value
  }
}
