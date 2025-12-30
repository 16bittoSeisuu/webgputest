package net.japanesehunter.worldcreate.trait

import net.japanesehunter.math.Aabb
import net.japanesehunter.math.MutableAabb
import net.japanesehunter.math.copyOf
import net.japanesehunter.traits.TraitKey

/**
 * Represents a read-only view of entity collision shapes.
 */
interface BoundingBoxView {
  /**
   * The collision boxes in local coordinates.
   */
  val boxes: List<Aabb>
}

/**
 * The collision shapes of an entity defined as axis-aligned bounding boxes.
 *
 * Each box is expressed in local coordinates where the entity's [Position] is the origin.
 * Multiple boxes allow for complex collision shapes composed of simple primitives.
 *
 * All boxes are defensively copied on construction and when added, so modifications to the
 * original instances do not affect this trait.
 *
 * An empty bounding box indicates the entity has no collision shape.
 *
 * @param boxes the initial collision boxes in local coordinates.
 */
class BoundingBox(vararg boxes: Aabb) : BoundingBoxView {
  private val _boxes: MutableList<MutableAabb> =
    boxes
      .map {
        MutableAabb.copyOf(it)
      }.toMutableList()

  /**
   * The collision boxes in local coordinates.
   *
   * This list is a read-only view. Use [add], [remove], and [clear] to modify the boxes.
   */
  override val boxes: List<MutableAabb> get() = _boxes

  /**
   * Adds a collision box to this bounding box.
   *
   * The provided box is defensively copied, so subsequent modifications to the original
   * instance do not affect this trait.
   *
   * @param box the collision box to add in local coordinates.
   */
  fun add(
    box: Aabb,
  ) {
    _boxes.add(MutableAabb.copyOf(box))
  }

  /**
   * Removes a collision box from this bounding box.
   *
   * The box is removed by reference equality with the internal copies, not the original.
   * Use the instances from [boxes] to remove specific boxes.
   *
   * @param box the collision box to remove.
   * @return true if the box was removed, false if it was not found.
   */
  fun remove(
    box: MutableAabb,
  ): Boolean =
    _boxes.remove(box)

  /**
   * Removes all collision boxes from this bounding box.
   */
  fun clear() {
    _boxes.clear()
  }

  companion object : TraitKey<BoundingBoxView, BoundingBox> by TraitKey()
}
