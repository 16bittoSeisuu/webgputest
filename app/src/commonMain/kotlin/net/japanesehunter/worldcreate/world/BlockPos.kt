package net.japanesehunter.worldcreate.world

import net.japanesehunter.math.ImmutableLength3
import net.japanesehunter.math.ImmutablePoint3
import net.japanesehunter.math.Length
import net.japanesehunter.math.Length3
import net.japanesehunter.math.LengthUnit
import net.japanesehunter.math.Point3
import net.japanesehunter.math.meters
import net.japanesehunter.math.unaryMinus
import net.japanesehunter.worldcreate.world.World
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Represents an integer block position measured in whole meters within the finite world.
 * Coordinates never exceed the magnitude derived from [World.Companion.MAX_SIZE] and the type remains immutable and thread-safe.
 *
 * Invariants state that each component stays within `[-maxCoordinate, maxCoordinate]`, where `maxCoordinate` comes from the
 * whole-meter value of [World.Companion.MAX_SIZE].
 */
data class BlockPos(
  /**
   * Whole-meter x coordinate of the block.
   * range: -maxCoordinate <= x <= maxCoordinate
   */
  val x: Int,
  /**
   * Whole-meter y coordinate of the block.
   * range: -maxCoordinate <= y <= maxCoordinate
   */
  val y: Int,
  /**
   * Whole-meter z coordinate of the block.
   * range: -maxCoordinate <= z <= maxCoordinate
   */
  val z: Int,
) {
  init {
    ensureWithinWorld(x)
    ensureWithinWorld(y)
    ensureWithinWorld(z)
  }

  /**
   * Computes a new position translated by the provided deltas.
   *
   * @param dx Delta along the x-axis in blocks.
   * @param dy Delta along the y-axis in blocks.
   * @param dz Delta along the z-axis in blocks.
   * @return The translated block position.
   * @throws ArithmeticException If arithmetic overflows or the result exceeds world bounds.
   */
  fun offset(
    dx: Int = 0,
    dy: Int = 0,
    dz: Int = 0,
  ): BlockPos =
    BlockPos(addExactInt(x, dx), addExactInt(y, dy), addExactInt(z, dz))

  /**
   * Computes a new position by subtracting another block position component-wise.
   *
   * @param other The position to subtract.
   * @return The displacement from [other] to this position.
   */
  operator fun minus(
    other: BlockPos,
  ): ImmutableLength3 =
    Length3(
      dx = subtractExactInt(x, other.x).meters,
      dy = subtractExactInt(y, other.y).meters,
      dz = subtractExactInt(z, other.z).meters,
    )

  /**
   * Computes the negated position.
   *
   * @return The position with each component negated.
   * @throws ArithmeticException If arithmetic overflows or the result exceeds world bounds.
   */
  operator fun unaryMinus(): BlockPos =
    BlockPos(negateExactInt(x), negateExactInt(y), negateExactInt(z))

  /**
   * Translates this position by a metric distance.
   * Rounds each component to the nearest whole meter before applying.
   *
   * @param distance The displacement to apply.
   * @return The translated block position.
   * @throws ArithmeticException If rounding overflows or the result exceeds world bounds.
   */
  operator fun plus(
    distance: Length3,
  ): BlockPos =
    translateBy(distance)

  /**
   * Translates this position by the negated metric distance.
   * Rounds each component to the nearest whole meter before applying.
   *
   * @param distance The displacement to subtract.
   * @return The translated block position.
   * @throws ArithmeticException If rounding overflows or the result exceeds world bounds.
   */
  operator fun minus(
    distance: Length3,
  ): BlockPos =
    translateBy(-distance)

  /**
   * Converts this block position to a [net.japanesehunter.math.Point3] measured in meters.

   * @return The position expressed as [net.japanesehunter.math.ImmutablePoint3].
   */
  fun toPoint3(): ImmutablePoint3 =
    Point3(
      x = x.meters,
      y = y.meters,
      z = z.meters,
    )

  private fun translateBy(
    distance: Length3,
  ): BlockPos {
    val dxMeters = roundLengthToMeters(distance.dx)
    val dyMeters = roundLengthToMeters(distance.dy)
    val dzMeters = roundLengthToMeters(distance.dz)
    return BlockPos(
      addExactInt(
        x,
        dxMeters,
      ),
      addExactInt(y, dyMeters), addExactInt(z, dzMeters),
    )
  }

  companion object {
    /**
     * The origin block position at (0, 0, 0).
     */
    val origin: BlockPos = BlockPos(0, 0, 0)

    private val maxCoordinate: Int =
      run {
        val meters = World.Companion.MAX_SIZE.inWholeMeters
        require(
          meters <=
            Int.MAX_VALUE
              .toLong(),
        ) {
          "World size exceeds integer coordinate capacity."
        }
        meters.toInt()
      }

    private fun ensureWithinWorld(
      value: Int,
    ) {
      val magnitude = abs(value.toLong())
      if (magnitude > maxCoordinate.toLong()) {
        throw ArithmeticException(
          "Block position component $value exceeds world bounds of +/-$maxCoordinate meters.",
        )
      }
    }

    private fun addExactInt(
      a: Int,
      b: Int,
    ): Int {
      val result = a.toLong() + b.toLong()
      if (result < Int.MIN_VALUE || result > Int.MAX_VALUE) {
        throw ArithmeticException("integer overflow")
      }
      return result.toInt()
    }

    private fun subtractExactInt(
      a: Int,
      b: Int,
    ): Int {
      val result = a.toLong() - b.toLong()
      if (result < Int.MIN_VALUE || result > Int.MAX_VALUE) {
        throw ArithmeticException("integer overflow")
      }
      return result.toInt()
    }

    private fun negateExactInt(
      value: Int,
    ): Int {
      if (value == Int.MIN_VALUE) {
        throw ArithmeticException("integer overflow")
      }
      return -value
    }

    private fun roundLengthToMeters(
      length: Length,
    ): Int {
      val meters = length.toDouble(LengthUnit.METER)
      if (!meters.isFinite()) {
        throw ArithmeticException(
          "Non-finite length cannot be rounded to meters",
        )
      }
      val rounded = meters.roundToLong()
      if (rounded < Int.MIN_VALUE || rounded > Int.MAX_VALUE) {
        throw ArithmeticException("Rounded length $rounded exceeds Int range")
      }
      return rounded.toInt()
    }
  }
}
