package net.japanesehunter.worldcreate

import net.japanesehunter.math.Direction3
import net.japanesehunter.math.Point3
import net.japanesehunter.math.Proportion
import net.japanesehunter.math.cross
import net.japanesehunter.math.minus
import net.japanesehunter.math.toDirection
import net.japanesehunter.math.unaryMinus

class MaterialTriangle(
  v0: Pair<Point3, Proportion>,
  v1: Pair<Point3, Proportion>,
  v2: Pair<Point3, Proportion>,
  val material: MaterialKey,
) : Triangle(v0.first, v1.first, v2.first) {
  val aoV0: Proportion = v0.second
  val aoV1: Proportion = v1.second
  val aoV2: Proportion = v2.second
}

open class Triangle(
  val v0: Point3,
  val v1: Point3,
  val v2: Point3,
) {
  init {
    require(v0 != v1 && v1 != v2 && v2 != v0) {
      "The triangle's vertices must be distinct."
    }
    val edge1 = (v1 - v0).toDirection()
    val edge2 = (v2 - v0).toDirection()
    require(edge1 != edge2 && edge1 != -edge2) {
      "The triangle's vertices must not be collinear."
    }
  }

  fun normalCcw(): Direction3 {
    val edge1 = (v1 - v0).toDirection()
    val edge2 = (v2 - v0).toDirection()
    return edge2 cross edge1
  }
}
