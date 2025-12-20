package net.japanesehunter.worldcreate.entity

import net.japanesehunter.math.Length
import net.japanesehunter.math.Length3
import net.japanesehunter.math.MovableCamera
import net.japanesehunter.math.setPosition
import net.japanesehunter.math.setRotation

/**
 * Synchronizes the camera position and rotation with a player.
 *
 * Sets the camera position to the player's position offset by the eye height, and copies the
 * player's rotation to the camera.
 *
 * @param player the player to follow.
 * @param eyeHeight the vertical offset from player position to camera position.
 */
fun MovableCamera.sync(
  player: Player,
  eyeHeight: Length,
) {
  val pos = player.position
  setPosition(Length3(pos.x, pos.y + eyeHeight, pos.z))
  setRotation(player.rotation)
}
