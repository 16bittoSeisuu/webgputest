package net.japanesehunter.worldcreate

import net.japanesehunter.math.Direction3
import net.japanesehunter.math.Point3
import net.japanesehunter.math.Proportion
import net.japanesehunter.math.down
import net.japanesehunter.math.east
import net.japanesehunter.math.meters
import net.japanesehunter.math.north
import net.japanesehunter.math.south
import net.japanesehunter.math.up
import net.japanesehunter.math.west

interface BlockState {
  val quads: List<MaterialQuad>

  fun isOpaque(face: BlockFace): Boolean

  data object Air : BlockState {
    override val quads: List<MaterialQuad> = emptyList()

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

  override val quads: List<MaterialQuad> =
    run {

      fun quad(
        min: Point3,
        normal: Direction3,
        tangent: Direction3,
        material: MaterialKey,
      ) = MaterialQuad(
        min = min,
        normal = normal,
        tangent = tangent,
        sizeU = 1.meters,
        sizeV = 1.meters,
        aoLeftBottom = Proportion.ONE,
        aoRightBottom = Proportion.ONE,
        aoLeftTop = Proportion.ONE,
        aoRightTop = Proportion.ONE,
        material = material,
      )
      listOf(
        // UP
        quad(
          Point3(0.meters, 1.meters, 0.meters),
          Direction3.up,
          Direction3.east,
          top,
        ),
        // NORTH
        quad(
          Point3(1.meters, 1.meters, 0.meters),
          Direction3.north,
          Direction3.west,
          north,
        ),
        // EAST
        quad(
          Point3(1.meters, 1.meters, 1.meters),
          Direction3.east,
          Direction3.north,
          east,
        ),
        // SOUTH
        quad(
          Point3(0.meters, 1.meters, 1.meters),
          Direction3.south,
          Direction3.east,
          south,
        ),
        // WEST
        quad(
          Point3(0.meters, 1.meters, 0.meters),
          Direction3.west,
          Direction3.south,
          west,
        ),
        // BOTTOM
        quad(
          Point3(1.meters, 0.meters, 0.meters),
          Direction3.down,
          Direction3.west,
          bottom,
        ),
      )
    }

  override fun isOpaque(face: BlockFace): Boolean = true
}
