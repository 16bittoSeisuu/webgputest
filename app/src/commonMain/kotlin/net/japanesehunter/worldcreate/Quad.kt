package net.japanesehunter.worldcreate

import net.japanesehunter.math.Direction3
import net.japanesehunter.math.Length
import net.japanesehunter.math.Point3
import net.japanesehunter.math.Proportion
import net.japanesehunter.math.cross

data class Quad(
  val pos: Point3,
  val normal: Direction3,
  val tangent: Direction3,
  val aoLeftBottom: Proportion,
  val aoRightBottom: Proportion,
  val aoLeftTop: Proportion,
  val aoRightTop: Proportion,
  val sizeU: Length,
  val sizeV: Length,
  val materialId: Int,
) {
  init {
    require(pos.x < World.MAX_SIZE) {
      "pos.x must be less than ${World.MAX_SIZE}, but got: $pos"
    }
    require(pos.y < World.MAX_SIZE) {
      "pos.y must be less than ${World.MAX_SIZE}, but got: $pos"
    }
    require(pos.z < World.MAX_SIZE) {
      "pos.z must be less than ${World.MAX_SIZE}, but got: $pos"
    }
    try {
      normal cross tangent
    } catch (_: Throwable) {
      error(
        "normal and tangent must not be parallel, but got: " +
          "normal=$normal, tangent=$tangent",
      )
    }
    require(sizeU in Length.ZERO..<World.MAX_SIZE) {
      "sizeU must be in [0, ${World.MAX_SIZE}), but got: $sizeU"
    }
    require(sizeV in Length.ZERO..<World.MAX_SIZE) {
      "sizeV must be in [0, ${World.MAX_SIZE}), but got: $sizeV"
    }
  }
}
