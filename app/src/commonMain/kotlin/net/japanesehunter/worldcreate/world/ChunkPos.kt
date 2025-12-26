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
 * Represents a chunk position indexing contiguous 16x16x16 block regions within the finite world.
 * Coordinates remain within [-maxChunkCoordinate, maxChunkCoordinate] derived from [World.Companion.MAX_CHUNK_COORDINATE],
 * and instances stay immutable and thread-safe.
 *
 * @param x Chunk index along the x axis.
 *   range: -maxChunkCoordinate <= x <= maxChunkCoordinate
 * @param y Chunk index along the y axis.
 *   range: -maxChunkCoordinate <= y <= maxChunkCoordinate
 * @param z Chunk index along the z axis.
 *   range: -maxChunkCoordinate <= z <= maxChunkCoordinate
 */
data class ChunkPos(
  val x: Int,
  val y: Int,
  val z: Int,
) {
  init {
    ensureWithinWorld(x)
    ensureWithinWorld(y)
    ensureWithinWorld(z)
  }

  /**
   * Computes a new position translated by the provided chunk deltas.
   *
   * @param dx Chunk delta along the x axis.
   *   range: result remains within world bounds
   * @param dy Chunk delta along the y axis.
   *   range: result remains within world bounds
   * @param dz Chunk delta along the z axis.
   *   range: result remains within world bounds
   * @return The translated chunk position.
   * @throws ArithmeticException If arithmetic overflows or the result exceeds world bounds.
   */
  fun offset(
    dx: Int = 0,
    dy: Int = 0,
    dz: Int = 0,
  ): ChunkPos = ChunkPos(addExactInt(x, dx), addExactInt(y, dy), addExactInt(z, dz))

  /**
   * Computes the metric displacement between this position and another chunk position.
   *
   * @param other The position to subtract.
   * @return The displacement from [other] to this position measured in meters.
   */
  operator fun minus(other: ChunkPos): ImmutableLength3 =
    Length3(
      dx = chunksToMeters(subtractExactInt(x, other.x)).meters,
      dy = chunksToMeters(subtractExactInt(y, other.y)).meters,
      dz = chunksToMeters(subtractExactInt(z, other.z)).meters,
    )

  /**
   * Computes the negated chunk position.
   *
   * @return The position with each component negated.
   * @throws ArithmeticException If arithmetic overflows or the result exceeds world bounds.
   */
  operator fun unaryMinus(): ChunkPos = ChunkPos(negateExactInt(x), negateExactInt(y), negateExactInt(z))

  /**
   * Translates this chunk position by a metric distance rounded to the nearest chunk boundary.
   *
   * @param distance The displacement to apply.
   * @return The translated chunk position.
   * @throws ArithmeticException If rounding overflows or the result exceeds world bounds.
   */
  operator fun plus(distance: Length3): ChunkPos = translateBy(distance)

  /**
   * Translates this chunk position by the negated metric distance rounded to the nearest chunk boundary.
   *
   * @param distance The displacement to subtract.
   * @return The translated chunk position.
   * @throws ArithmeticException If rounding overflows or the result exceeds world bounds.
   */
  operator fun minus(distance: Length3): ChunkPos = translateBy(-distance)

  /**
   * Converts this chunk position to a point measured in meters at the chunk origin.
   *
   * @return The position expressed as [net.japanesehunter.math.ImmutablePoint3].
   */
  fun toPoint3(): ImmutablePoint3 =
    Point3(
      x = chunksToMeters(x).meters,
      y = chunksToMeters(y).meters,
      z = chunksToMeters(z).meters,
    )

  private fun translateBy(distance: Length3): ChunkPos {
    val dxChunks = roundLengthToChunks(distance.dx)
    val dyChunks = roundLengthToChunks(distance.dy)
    val dzChunks = roundLengthToChunks(distance.dz)
    return ChunkPos(addExactInt(x, dxChunks), addExactInt(y, dyChunks), addExactInt(z, dzChunks))
  }

  companion object {
    /**
     * The origin chunk position at (0, 0, 0).
     */
    val origin: ChunkPos = ChunkPos(0, 0, 0)

    private val maxChunkCoordinate: Int =
      run {
        val max = World.Companion.MAX_CHUNK_COORDINATE
        require(max <= Int.MAX_VALUE) { "Chunk coordinate exceeds integer capacity." }
        max
      }

    private fun ensureWithinWorld(value: Int) {
      val magnitude = abs(value.toLong())
      if (magnitude > maxChunkCoordinate.toLong()) {
        throw ArithmeticException("Chunk position component $value exceeds world bounds of +/-$maxChunkCoordinate chunks.")
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

    private fun negateExactInt(value: Int): Int {
      if (value == Int.MIN_VALUE) {
        throw ArithmeticException("integer overflow")
      }
      return -value
    }

    private fun roundLengthToChunks(length: Length): Int {
      val meters = length.toDouble(LengthUnit.METER)
      if (!meters.isFinite()) {
        throw ArithmeticException("Non-finite length cannot be rounded to chunks")
      }
      val chunks = meters / World.Companion.CHUNK_LENGTH_BLOCKS
      val rounded = chunks.roundToLong()
      if (rounded < Int.MIN_VALUE || rounded > Int.MAX_VALUE) {
        throw ArithmeticException("Rounded chunk displacement $rounded exceeds Int range")
      }
      return rounded.toInt()
    }

    private fun chunksToMeters(chunks: Int): Int {
      val meters = chunks.toLong() * World.Companion.CHUNK_LENGTH_BLOCKS
      if (meters < Int.MIN_VALUE || meters > Int.MAX_VALUE) {
        throw ArithmeticException("Chunk conversion to meters overflowed")
      }
      return meters.toInt()
    }
  }
}
