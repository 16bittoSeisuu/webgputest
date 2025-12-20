package net.japanesehunter.worldcreate.trait

import net.japanesehunter.math.MutableQuaternion

/**
 * The orientation of an entity in 3D space.
 *
 * For player entities, this typically represents the direction the entity is
 * facing. The rotation can be modified to change the entity's orientation.
 */
data class Rotation(
  val value: MutableQuaternion,
)
