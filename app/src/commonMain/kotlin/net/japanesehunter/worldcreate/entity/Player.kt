package net.japanesehunter.worldcreate.entity

import net.japanesehunter.math.Aabb
import net.japanesehunter.math.ImmutableLength3
import net.japanesehunter.math.Length
import net.japanesehunter.math.Length3
import net.japanesehunter.math.MutableAabb
import net.japanesehunter.math.MutablePoint3
import net.japanesehunter.math.MutableVelocity3
import net.japanesehunter.math.Point3
import net.japanesehunter.math.copyOf
import net.japanesehunter.math.meters
import net.japanesehunter.math.overlaps
import net.japanesehunter.math.times
import net.japanesehunter.math.translatedBy
import net.japanesehunter.math.zero
import net.japanesehunter.worldcreate.world.BlockAccess
import net.japanesehunter.worldcreate.world.EventSubscription
import net.japanesehunter.worldcreate.world.TickSource
import kotlin.time.Duration

/**
 * Represents an entity that exists in the world with a position and bounding box.
 *
 * Players have a position representing the center of their feet and a bounding box
 * for collision detection.
 *
 * Implementations are not required to be thread-safe.
 */
interface Player {
  /**
   * The position of the player in world space at the center bottom of the bounding box.
   */
  val position: Point3

  /**
   * The current velocity of the player expressed as distance per second.
   * Modifying this value affects the player's movement in the next physics tick.
   */
  val velocity: MutableVelocity3

  /**
   * The axis-aligned bounding box representing the player's collision shape.
   */
  val boundingBox: Aabb

  /**
   * Whether the player is currently standing on solid ground.
   * Updated by the physics system after each tick based on collision resolution.
   */
  val isGrounded: Boolean

  /**
   * Closes the player and unsubscribes from any event sources.
   */
  fun close()
}

/**
 * Creates a player with physics simulation including gravity and collision detection.
 *
 * The created player subscribes to the provided tick source for physics updates.
 * Physics simulation uses discrete collision detection with axis-by-axis resolution.
 *
 * @param tickSource the source of tick events driving physics updates.
 * @param blockAccess the provider of collision geometry.
 * @param initialPosition the starting position of the player.
 * @param size the dimensions of the player's bounding box.
 * @param gravity the gravitational acceleration applied each second.
 * @return the created player.
 */
fun Player(
  tickSource: TickSource,
  blockAccess: BlockAccess,
  initialPosition: Point3 = Point3.zero,
  size: Length3 = Length3(dx = 0.6.meters, dy = 1.8.meters, dz = 0.6.meters),
  gravity: Length = (-32).meters,
): Player = PhysicsPlayer(tickSource, blockAccess, initialPosition, Length3.copyOf(size), gravity)

private class PhysicsPlayer(
  tickSource: TickSource,
  private val blockAccess: BlockAccess,
  initialPosition: Point3,
  private val size: ImmutableLength3,
  private val gravity: Length,
) : Player {
  private val mutablePosition: MutablePoint3 = MutablePoint3.copyOf(initialPosition)
  override val velocity: MutableVelocity3 = MutableVelocity3()
  private val mutableBoundingBox: MutableAabb = computeBoundingBox(initialPosition, size)
  private var groundedState: Boolean = false

  private val subscription: EventSubscription = tickSource.subscribe { delta -> tick(delta) }

  override val position: Point3 get() = mutablePosition
  override val boundingBox: Aabb get() = mutableBoundingBox
  override val isGrounded: Boolean get() = groundedState

  override fun close() {
    subscription.close()
  }

  private fun tick(delta: Duration) {
    val dt = delta.inWholeNanoseconds / 1_000_000_000.0
    if (!dt.isFinite() || dt <= 0.0) return

    groundedState = false

    velocity.vy += gravity * dt

    val displacement = velocity * delta
    moveAxis(Axis.Y, displacement.dy)
    moveAxis(Axis.X, displacement.dx)
    moveAxis(Axis.Z, displacement.dz)
  }

  private fun moveAxis(
    axis: Axis,
    delta: Length,
  ) {
    if (delta.isZero) return

    val displacement =
      when (axis) {
        Axis.X -> Length3(dx = delta, dy = Length.ZERO, dz = Length.ZERO)
        Axis.Y -> Length3(dx = Length.ZERO, dy = delta, dz = Length.ZERO)
        Axis.Z -> Length3(dx = Length.ZERO, dy = Length.ZERO, dz = delta)
      }

    val targetBox = mutableBoundingBox.translatedBy(displacement)
    val collisions = blockAccess.getCollisions(targetBox)

    if (collisions.isEmpty()) {
      applyMovement(axis, delta)
      return
    }

    for (collision in collisions) {
      if (!targetBox.overlaps(collision)) continue

      val resolved = resolveCollision(axis, delta, collision)
      if (resolved != delta) {
        applyMovement(axis, resolved)
        stopVelocity(axis)
        if (axis == Axis.Y && delta.isNegative) {
          groundedState = true
        }
        return
      }
    }

    applyMovement(axis, delta)
  }

  private fun resolveCollision(
    axis: Axis,
    delta: Length,
    collision: Aabb,
  ): Length =
    when (axis) {
      Axis.X -> {
        if (delta.isPositive) {
          val maxMove = collision.min.x - mutableBoundingBox.max.x
          if (maxMove.isNegative) Length.ZERO else minOf(delta, maxMove)
        } else {
          val maxMove = collision.max.x - mutableBoundingBox.min.x
          if (maxMove.isPositive) Length.ZERO else maxOf(delta, maxMove)
        }
      }

      Axis.Y -> {
        if (delta.isPositive) {
          val maxMove = collision.min.y - mutableBoundingBox.max.y
          if (maxMove.isNegative) Length.ZERO else minOf(delta, maxMove)
        } else {
          val maxMove = collision.max.y - mutableBoundingBox.min.y
          if (maxMove.isPositive) Length.ZERO else maxOf(delta, maxMove)
        }
      }

      Axis.Z -> {
        if (delta.isPositive) {
          val maxMove = collision.min.z - mutableBoundingBox.max.z
          if (maxMove.isNegative) Length.ZERO else minOf(delta, maxMove)
        } else {
          val maxMove = collision.max.z - mutableBoundingBox.min.z
          if (maxMove.isPositive) Length.ZERO else maxOf(delta, maxMove)
        }
      }
    }

  private fun applyMovement(
    axis: Axis,
    delta: Length,
  ) {
    when (axis) {
      Axis.X -> {
        mutablePosition.x += delta
        mutableBoundingBox.min = Point3(mutableBoundingBox.min.x + delta, mutableBoundingBox.min.y, mutableBoundingBox.min.z)
        mutableBoundingBox.max = Point3(mutableBoundingBox.max.x + delta, mutableBoundingBox.max.y, mutableBoundingBox.max.z)
      }

      Axis.Y -> {
        mutablePosition.y += delta
        mutableBoundingBox.min = Point3(mutableBoundingBox.min.x, mutableBoundingBox.min.y + delta, mutableBoundingBox.min.z)
        mutableBoundingBox.max = Point3(mutableBoundingBox.max.x, mutableBoundingBox.max.y + delta, mutableBoundingBox.max.z)
      }

      Axis.Z -> {
        mutablePosition.z += delta
        mutableBoundingBox.min = Point3(mutableBoundingBox.min.x, mutableBoundingBox.min.y, mutableBoundingBox.min.z + delta)
        mutableBoundingBox.max = Point3(mutableBoundingBox.max.x, mutableBoundingBox.max.y, mutableBoundingBox.max.z + delta)
      }
    }
  }

  private fun stopVelocity(axis: Axis) {
    when (axis) {
      Axis.X -> velocity.vx = Length.ZERO
      Axis.Y -> velocity.vy = Length.ZERO
      Axis.Z -> velocity.vz = Length.ZERO
    }
  }

  private enum class Axis { X, Y, Z }
}

private fun computeBoundingBox(
  position: Point3,
  size: Length3,
): MutableAabb {
  val halfWidth = size.dx / 2
  val halfDepth = size.dz / 2
  return MutableAabb(
    min =
      Point3(
        x = position.x - halfWidth,
        y = position.y,
        z = position.z - halfDepth,
      ),
    max =
      Point3(
        x = position.x + halfWidth,
        y = position.y + size.dy,
        z = position.z + halfDepth,
      ),
  )
}
