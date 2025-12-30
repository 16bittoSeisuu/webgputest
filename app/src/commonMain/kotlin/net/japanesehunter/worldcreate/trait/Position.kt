package net.japanesehunter.worldcreate.trait

import net.japanesehunter.math.MutablePoint3
import net.japanesehunter.math.Point3
import net.japanesehunter.math.copyOf
import net.japanesehunter.math.zero
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
class Position(initialPosition: Point3 = Point3.zero) : PositionView {
  override val value: MutablePoint3 = MutablePoint3.copyOf(initialPosition)

  companion object : TraitKey<PositionView, Position> by TraitKey()
}
