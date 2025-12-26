package net.japanesehunter.worldcreate.world

import net.japanesehunter.math.Direction3
import net.japanesehunter.math.Point3
import net.japanesehunter.math.Proportion
import net.japanesehunter.math.east
import net.japanesehunter.math.meters
import net.japanesehunter.math.north
import net.japanesehunter.math.south
import net.japanesehunter.math.west
import net.japanesehunter.worldcreate.MaterialKey
import net.japanesehunter.worldcreate.world.QuadSink

interface BlockState {
  fun QuadSink.emitQuads()

  fun isOpaque(face: BlockFace): Boolean

  data object Air : BlockState {
    override fun QuadSink.emitQuads() {
      // No quads for air
    }

    override fun isOpaque(face: BlockFace): Boolean = false
  }
}

open class FullBlockState(
  top: MaterialKey,
  north: MaterialKey,
  east: MaterialKey,
  south: MaterialKey,
  west: MaterialKey,
  bottom: MaterialKey,
) : BlockState {
  constructor(face: MaterialKey) : this(face, face, face, face, face, face)

  constructor(top: MaterialKey, side: MaterialKey, bottom: MaterialKey) :
    this(
      top,
      side,
      side,
      side,
      side,
      bottom,
    )

  override fun QuadSink.emitQuads() {
    quads.forEach { (quad, face) ->
      put(quad) {
        requireFace(face)
      }
    }
  }

  private val quads =
    run {
      fun quad(
        min: Point3,
        normal: BlockFace,
        tangent: Direction3,
        material: MaterialKey,
      ) = MaterialQuad(
        min = min,
        normal = normal.normal,
        tangent = tangent,
        sizeU = 1.meters,
        sizeV = 1.meters,
        aoLeftBottom = Proportion.ONE,
        aoRightBottom = Proportion.ONE,
        aoLeftTop = Proportion.ONE,
        aoRightTop = Proportion.ONE,
        material = material,
      ) to normal
      listOf(
        // UP
        quad(
          Point3(0.meters, 1.meters, 0.meters),
          BlockFace.TOP,
          Direction3.east,
          top,
        ),
        // NORTH
        quad(
          Point3(1.meters, 1.meters, 0.meters),
          BlockFace.NORTH,
          Direction3.west,
          north,
        ),
        // EAST
        quad(
          Point3(1.meters, 1.meters, 1.meters),
          BlockFace.EAST,
          Direction3.north,
          east,
        ),
        // SOUTH
        quad(
          Point3(0.meters, 1.meters, 1.meters),
          BlockFace.SOUTH,
          Direction3.east,
          south,
        ),
        // WEST
        quad(
          Point3(0.meters, 1.meters, 0.meters),
          BlockFace.WEST,
          Direction3.south,
          west,
        ),
        // BOTTOM
        quad(
          Point3(1.meters, 0.meters, 0.meters),
          BlockFace.BOTTOM,
          Direction3.west,
          bottom,
        ),
      )
    }

  override fun isOpaque(face: BlockFace): Boolean = true
}
