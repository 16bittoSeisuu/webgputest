package net.japanesehunter.worldcreate

import net.japanesehunter.math.Direction3
import net.japanesehunter.math.Length
import net.japanesehunter.math.Length3
import net.japanesehunter.math.Point3
import net.japanesehunter.math.Proportion
import net.japanesehunter.math.cross
import net.japanesehunter.math.plus
import net.japanesehunter.math.times

class GreedyQuad(
  val shape: QuadShape,
  val aoLeftBottom: Proportion,
  val aoRightBottom: Proportion,
  val aoLeftTop: Proportion,
  val aoRightTop: Proportion,
  val repeatU: Int,
  val repeatV: Int,
  val material: MaterialKey,
) {
  fun toTriangles(): Pair<GreedyTriangle, GreedyTriangle> {
    val v0 = shape.min
    val v1 = shape.min + shape.u
    val v2 = shape.min + shape.v
    val v3 = shape.max

    return GreedyTriangle(
      shape = Triangle(v0, v1, v2),
      aoV0 = aoLeftBottom,
      aoV1 = aoRightBottom,
      aoV2 = aoLeftTop,
      repeatU = repeatU,
      repeatV = repeatV,
      material = material,
    ) to
      GreedyTriangle(
        shape = Triangle(v2, v1, v3),
        aoV0 = aoLeftTop,
        aoV1 = aoRightBottom,
        aoV2 = aoRightTop,
        repeatU = repeatU,
        repeatV = repeatV,
        material = material,
      )
  }
}

data class QuadShape(
  val min: Point3,
  val normal: Direction3,
  val tangent: Direction3,
  val sizeU: Length,
  val sizeV: Length,
) {
  val bitangent: Direction3 = tangent cross normal

  val u: Length3 = tangent * sizeU
  val v: Length3 = bitangent * sizeV

  val max: Point3
    get() = min + u + v
}
