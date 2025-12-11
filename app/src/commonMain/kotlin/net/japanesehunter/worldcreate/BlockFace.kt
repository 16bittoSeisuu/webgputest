package net.japanesehunter.worldcreate

import net.japanesehunter.math.Direction3
import net.japanesehunter.math.down
import net.japanesehunter.math.east
import net.japanesehunter.math.north
import net.japanesehunter.math.south
import net.japanesehunter.math.up
import net.japanesehunter.math.west

enum class BlockFace(
  normal: Direction3,
) {
  TOP(Direction3.up),
  NORTH(Direction3.north),
  EAST(Direction3.east),
  SOUTH(Direction3.south),
  WEST(Direction3.west),
  BOTTOM(Direction3.down),
}
