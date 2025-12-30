package net.japanesehunter.worldcreate.world

import net.japanesehunter.math.Length
import net.japanesehunter.math.meters

interface World {
  companion object {
    const val CHUNK_LENGTH_BLOCKS: Int = 16
    val MAX_SIZE: Length = 2_147_483_632.meters
    val MAX_CHUNK_COORDINATE: Int =
      run {
        val maxChunks = MAX_SIZE.inWholeMeters / CHUNK_LENGTH_BLOCKS
        require(
          maxChunks <=
            Int.MAX_VALUE
              .toLong(),
        ) {
          "World chunk span exceeds integer coordinate capacity."
        }
        maxChunks.toInt()
      }
  }
}
