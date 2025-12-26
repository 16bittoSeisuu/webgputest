package net.japanesehunter.worldcreate.world

import net.japanesehunter.math.Direction3
import net.japanesehunter.math.Length
import net.japanesehunter.math.Length3
import net.japanesehunter.math.Point3
import net.japanesehunter.math.Proportion
import net.japanesehunter.math.cross
import net.japanesehunter.math.plus
import net.japanesehunter.math.times
import net.japanesehunter.worldcreate.MaterialKey

class MaterialQuad(
  min: Point3,
  normal: Direction3,
  tangent: Direction3,
  sizeU: Length,
  sizeV: Length,
  val aoLeftBottom: Proportion,
  val aoRightBottom: Proportion,
  val aoLeftTop: Proportion,
  val aoRightTop: Proportion,
  val material: MaterialKey,
) : Quad(min, normal, tangent, sizeU, sizeV) {
  fun toTriangles(): Pair<MaterialTriangle, MaterialTriangle> {
    val v0 = min
    val v1 = min + u
    val v2 = min + v
    val v3 = max

    return MaterialTriangle(
      v0 = v0 to aoLeftBottom,
      v1 = v1 to aoRightBottom,
      v2 = v2 to aoLeftTop,
      material = material,
    ) to
      MaterialTriangle(
        v0 = v1 to aoLeftTop,
        v1 = v3 to aoRightBottom,
        v2 = v2 to aoRightTop,
        material = material,
      )
  }
}

open class Quad(
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
