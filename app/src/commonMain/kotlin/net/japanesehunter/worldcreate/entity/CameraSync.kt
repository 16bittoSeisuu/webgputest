package net.japanesehunter.worldcreate.entity

import net.japanesehunter.math.Length
import net.japanesehunter.math.Length3
import net.japanesehunter.math.MovableCamera
import net.japanesehunter.math.setPosition
import net.japanesehunter.math.setRotation
import net.japanesehunter.traits.EntityId
import net.japanesehunter.traits.EntityRegistry
import net.japanesehunter.traits.get
import net.japanesehunter.worldcreate.trait.Position
import net.japanesehunter.worldcreate.trait.Rotation

/**
 * Synchronizes the camera position and rotation with an entity.
 *
 * Sets the camera position to the entity's position offset by the eye height, and copies the
 * entity's rotation to the camera.
 *
 * @param registry the entity registry containing the entity.
 * @param entity the entity to follow.
 * @param eyeHeight the vertical offset from entity position to camera position.
 */
fun MovableCamera.sync(
  registry: EntityRegistry,
  entity: EntityId,
  eyeHeight: Length,
) {
  val pos = registry.get<Position>(entity)?.value ?: return
  val rot = registry.get<Rotation>(entity)?.value ?: return
  setPosition(Length3(pos.x, pos.y + eyeHeight, pos.z))
  setRotation(rot)
}
