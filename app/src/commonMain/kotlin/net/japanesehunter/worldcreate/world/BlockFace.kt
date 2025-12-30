package net.japanesehunter.worldcreate.world

import net.japanesehunter.math.Direction3
import net.japanesehunter.math.down
import net.japanesehunter.math.east
import net.japanesehunter.math.north
import net.japanesehunter.math.south
import net.japanesehunter.math.up
import net.japanesehunter.math.west

enum class BlockFace(val normal: Direction3) {
  TOP(Direction3.up),
  NORTH(Direction3.north),
  EAST(Direction3.east),
  SOUTH(Direction3.south),
  WEST(Direction3.west),
  BOTTOM(Direction3.down),
}

@Suppress("NOTHING_TO_INLINE")
inline fun BlockFace.opposite(): BlockFace =
  when (this) {
    BlockFace.TOP -> BlockFace.BOTTOM
    BlockFace.NORTH -> BlockFace.SOUTH
    BlockFace.EAST -> BlockFace.WEST
    BlockFace.SOUTH -> BlockFace.NORTH
    BlockFace.WEST -> BlockFace.EAST
    BlockFace.BOTTOM -> BlockFace.TOP
  }
