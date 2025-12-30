package net.japanesehunter.worldcreate.world

import net.japanesehunter.math.Aabb

/**
 * Provides read access to collision geometry within a region of space.
 *
 * Implementations return a list of axis-aligned bounding boxes representing solid blocks
 * that may collide with entities in the queried region.
 *
 * Implementations are required to be thread-safe for concurrent queries.
 */
interface BlockAccess {
  /**
   * Retrieves collision boxes that intersect the given bounding box.
   *
   * @param region the bounding box to query for collisions.
   * @return a list of collision boxes that intersect the region.
   */
  fun getCollisions(
    region: Aabb,
  ): List<Aabb>
}
