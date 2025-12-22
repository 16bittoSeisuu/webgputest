package net.japanesehunter.worldcreate.simulation

import net.japanesehunter.math.Aabb
import net.japanesehunter.math.Length
import net.japanesehunter.math.Length3
import net.japanesehunter.math.MutablePoint3
import net.japanesehunter.math.Point3
import net.japanesehunter.math.Speed
import net.japanesehunter.math.overlaps
import net.japanesehunter.math.times
import net.japanesehunter.math.translatedBy
import net.japanesehunter.traits.EntityRegistry
import net.japanesehunter.traits.buildSystem
import net.japanesehunter.worldcreate.trait.BoundingBox
import net.japanesehunter.worldcreate.trait.Grounded
import net.japanesehunter.worldcreate.trait.Position
import net.japanesehunter.worldcreate.trait.Rigidbody
import net.japanesehunter.worldcreate.trait.Velocity
import net.japanesehunter.worldcreate.world.BlockAccess
import net.japanesehunter.worldcreate.world.TickSink

/**
 * Creates a rigid body physics simulation that processes entities on each tick.
 *
 * The simulation applies gravitational acceleration, air resistance, and resolves
 * collisions against the world geometry. Entities landing on solid ground receive
 * the [Grounded] trait. Entities becoming airborne have the trait removed.
 *
 * @param registry the entity registry containing entities to simulate.
 * @param blockAccess the provider of collision geometry.
 * @return the tick sink that executes the simulation logic.
 */
fun rigidbodySimulation(
  registry: EntityRegistry,
  blockAccess: BlockAccess,
): TickSink =
  buildSystem(registry) {
    val position by write(Position)
    val velocity by write(Velocity)
    val rigidbody by read(Rigidbody)
    val boundingBox by read(BoundingBox)

    forEach { delta ->
      if (!delta.isFinite() || delta.isNegative()) return@forEach

      remove(Grounded)

      velocity.value.vy += rigidbody.gravity * delta

      if (rigidbody.drag > 0.0) {
        val dt = delta.inWholeNanoseconds / 1_000_000_000.0
        val factor = kotlin.math.exp(-rigidbody.drag * dt)
        velocity.value.vx *= factor
        velocity.value.vy *= factor
        velocity.value.vz *= factor
      }

      val displacement = velocity.value * delta
      val context = MovementContext(position.value, velocity, boundingBox.boxes)

      var becameGrounded = false
      becameGrounded = moveAxis(context, Axis.Y, displacement.dy, blockAccess) || becameGrounded
      moveAxis(context, Axis.X, displacement.dx, blockAccess)
      moveAxis(context, Axis.Z, displacement.dz, blockAccess)

      if (becameGrounded) {
        add(Grounded)
      }
    }
  }

private fun moveAxis(
  context: MovementContext,
  axis: Axis,
  delta: Length,
  blockAccess: BlockAccess,
): Boolean {
  if (delta.isZero) return false

  val displacement =
    when (axis) {
      Axis.X -> Length3(dx = delta, dy = Length.ZERO, dz = Length.ZERO)
      Axis.Y -> Length3(dx = Length.ZERO, dy = delta, dz = Length.ZERO)
      Axis.Z -> Length3(dx = Length.ZERO, dy = Length.ZERO, dz = delta)
    }

  val worldBoxes =
    context.boxes.map { localBox ->
      localBox.toWorldSpace(context.position)
    }

  for (worldBox in worldBoxes) {
    val targetBox = worldBox.translatedBy(displacement)
    val collisions = blockAccess.getCollisions(targetBox)

    if (collisions.isEmpty()) continue

    for (collision in collisions) {
      if (!targetBox.overlaps(collision)) continue

      val resolved = resolveCollision(axis, delta, worldBox, collision)
      if (resolved != delta) {
        applyMovement(context, axis, resolved)
        stopVelocity(context.velocity, axis)
        return axis == Axis.Y && delta.isNegative
      }
    }
  }

  applyMovement(context, axis, delta)
  return false
}

private fun resolveCollision(
  axis: Axis,
  delta: Length,
  entityBox: Aabb,
  collision: Aabb,
): Length =
  when (axis) {
    Axis.X -> {
      if (delta.isPositive) {
        val maxMove = collision.min.x - entityBox.max.x
        if (maxMove.isNegative) Length.ZERO else minOf(delta, maxMove)
      } else {
        val maxMove = collision.max.x - entityBox.min.x
        if (maxMove.isPositive) Length.ZERO else maxOf(delta, maxMove)
      }
    }

    Axis.Y -> {
      if (delta.isPositive) {
        val maxMove = collision.min.y - entityBox.max.y
        if (maxMove.isNegative) Length.ZERO else minOf(delta, maxMove)
      } else {
        val maxMove = collision.max.y - entityBox.min.y
        if (maxMove.isPositive) Length.ZERO else maxOf(delta, maxMove)
      }
    }

    Axis.Z -> {
      if (delta.isPositive) {
        val maxMove = collision.min.z - entityBox.max.z
        if (maxMove.isNegative) Length.ZERO else minOf(delta, maxMove)
      } else {
        val maxMove = collision.max.z - entityBox.min.z
        if (maxMove.isPositive) Length.ZERO else maxOf(delta, maxMove)
      }
    }
  }

private fun applyMovement(
  context: MovementContext,
  axis: Axis,
  delta: Length,
) {
  when (axis) {
    Axis.X -> context.position.x += delta
    Axis.Y -> context.position.y += delta
    Axis.Z -> context.position.z += delta
  }
}

private fun stopVelocity(
  velocity: Velocity,
  axis: Axis,
) {
  when (axis) {
    Axis.X -> velocity.value.vx = Speed.ZERO
    Axis.Y -> velocity.value.vy = Speed.ZERO
    Axis.Z -> velocity.value.vz = Speed.ZERO
  }
}

private fun Aabb.toWorldSpace(origin: Point3): Aabb =
  Aabb(
    min =
      Point3(
        x = origin.x + min.x,
        y = origin.y + min.y,
        z = origin.z + min.z,
      ),
    max =
      Point3(
        x = origin.x + max.x,
        y = origin.y + max.y,
        z = origin.z + max.z,
      ),
  )

private class MovementContext(
  val position: MutablePoint3,
  val velocity: Velocity,
  val boxes: List<Aabb>,
)

private enum class Axis { X, Y, Z }
