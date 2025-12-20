package net.japanesehunter.worldcreate.trait

import net.japanesehunter.math.MutablePoint3

/**
 * The spatial coordinates of an entity in world space.
 *
 * For entities with a bounding box, this typically represents the center-bottom
 * point of their collision shape.
 */
data class Position(
  val value: MutablePoint3,
)
