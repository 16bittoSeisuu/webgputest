package net.japanesehunter.worldcreate.simulation

import arrow.fx.coroutines.Resource
import arrow.fx.coroutines.resource
import net.japanesehunter.math.Aabb
import net.japanesehunter.math.Length
import net.japanesehunter.math.Length3
import net.japanesehunter.math.MutableAabb
import net.japanesehunter.math.Point3
import net.japanesehunter.math.Speed
import net.japanesehunter.math.overlaps
import net.japanesehunter.math.times
import net.japanesehunter.math.translatedBy
import net.japanesehunter.traits.EntityId
import net.japanesehunter.traits.EntityRegistry
import net.japanesehunter.traits.get
import net.japanesehunter.worldcreate.trait.BoundingBox
import net.japanesehunter.worldcreate.trait.Grounded
import net.japanesehunter.worldcreate.trait.Position
import net.japanesehunter.worldcreate.trait.Rigidbody
import net.japanesehunter.worldcreate.trait.Velocity
import net.japanesehunter.worldcreate.world.BlockAccess
import net.japanesehunter.worldcreate.world.TickSource
import kotlin.time.Duration

/**
 * Creates a [RigidbodySimulation] that automatically ticks on each event from the [tickSource].
 *
 * The returned [Resource] manages the subscription lifecycle. When the resource is released,
 * the simulation unsubscribes from the tick source.
 *
 * @param tickSource the source of tick events driving physics updates.
 * @param registry the entity registry containing entities to simulate.
 * @param blockAccess the provider of collision geometry.
 * @return a resource that provides the simulation and manages its subscription.
 */
@Suppress("FunctionName")
fun RigidbodySimulation(
  tickSource: TickSource,
  registry: EntityRegistry,
  blockAccess: BlockAccess,
): Resource<RigidbodySimulation> =
  resource {
    val simulation = RigidbodySimulation(registry, blockAccess)
    val subscription = tickSource.subscribe { delta -> simulation.tick(delta) }
    install({ simulation }) { _, _ -> subscription.close() }
  }

/**
 * Simulates rigid body physics for entities with [Rigidbody], [Position], [Velocity], and [BoundingBox] traits.
 *
 * Each tick applies gravitational acceleration, air resistance, and resolves collisions against
 * the world geometry. The simulation uses discrete collision detection with axis-by-axis resolution.
 *
 * Entities that land on solid ground receive the [Grounded] trait. Entities that become airborne
 * have the [Grounded] trait removed.
 *
 * This class is not thread-safe.
 *
 * @param registry the entity registry containing entities to simulate.
 * @param blockAccess the provider of collision geometry.
 */
class RigidbodySimulation internal constructor(
  private val registry: EntityRegistry,
  private val blockAccess: BlockAccess,
) {
  /**
   * Advances the physics simulation by the given time delta.
   *
   * Processes all entities that have [Rigidbody], [Position], [Velocity], and [BoundingBox] traits.
   * Invalid delta values are ignored.
   *
   * @param delta the time elapsed since the last tick.
   *   NaN: ignored
   *   Infinity: ignored
   *   negative: ignored
   */
  fun tick(delta: Duration) {
    if (!delta.isFinite() || delta.isNegative()) return

    val entities =
      registry.query(
        Position::class,
        Velocity::class,
        Rigidbody::class,
        BoundingBox::class,
      )

    for (entity in entities) {
      tickEntity(entity, delta)
    }
  }

  private fun tickEntity(
    entity: EntityId,
    delta: Duration,
  ) {
    val position = registry.get<Position>(entity) ?: return
    val velocity = registry.get<Velocity>(entity) ?: return
    val rigidbody = registry.get<Rigidbody>(entity) ?: return
    val boundingBox = registry.get<BoundingBox>(entity) ?: return

    registry.remove(entity, Grounded::class)

    velocity.value.vy += rigidbody.gravity * delta

    if (rigidbody.drag > 0.0) {
      val dt = delta.inWholeNanoseconds / 1_000_000_000.0
      val factor = kotlin.math.exp(-rigidbody.drag * dt)
      velocity.value.vx *= factor
      velocity.value.vy *= factor
      velocity.value.vz *= factor
    }

    val displacement = velocity.value * delta
    val context = MovementContext(position, velocity, boundingBox)

    var becameGrounded = false
    becameGrounded = moveAxis(context, Axis.Y, displacement.dy) || becameGrounded
    moveAxis(context, Axis.X, displacement.dx)
    moveAxis(context, Axis.Z, displacement.dz)

    if (becameGrounded) {
      registry.add(entity, Grounded)
    }
  }

  private fun moveAxis(
    context: MovementContext,
    axis: Axis,
    delta: Length,
  ): Boolean {
    if (delta.isZero) return false

    val displacement =
      when (axis) {
        Axis.X -> Length3(dx = delta, dy = Length.ZERO, dz = Length.ZERO)
        Axis.Y -> Length3(dx = Length.ZERO, dy = delta, dz = Length.ZERO)
        Axis.Z -> Length3(dx = Length.ZERO, dy = Length.ZERO, dz = delta)
      }

    val worldBoxes =
      context.boundingBox.boxes.map { localBox ->
        localBox.toWorldSpace(context.position.value)
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
      Axis.X -> context.position.value.x += delta
      Axis.Y -> context.position.value.y += delta
      Axis.Z -> context.position.value.z += delta
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

  private fun MutableAabb.toWorldSpace(origin: Point3): Aabb =
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
    val position: Position,
    val velocity: Velocity,
    val boundingBox: BoundingBox,
  )

  private enum class Axis { X, Y, Z }
}
