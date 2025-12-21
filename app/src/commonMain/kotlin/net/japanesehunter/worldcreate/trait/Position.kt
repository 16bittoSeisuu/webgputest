package net.japanesehunter.worldcreate.trait

import net.japanesehunter.math.MutablePoint3
import net.japanesehunter.math.Point3
import net.japanesehunter.traits.TraitKey

/**
 * Represents a read-only view of entity position in world space.
 */
interface PositionView {
  /**
   * The spatial coordinates.
   */
  val value: Point3
}

/**
 * The spatial coordinates of an entity in world space.
 *
 * For entities with a bounding box, this typically represents the center-bottom
 * point of their collision shape.
 */
data class Position(
  override val value: MutablePoint3,
) : PositionView {
  companion object : TraitKey<PositionView, Position> by TraitKey()
}
