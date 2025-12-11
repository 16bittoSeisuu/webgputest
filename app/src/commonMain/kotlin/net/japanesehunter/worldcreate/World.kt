package net.japanesehunter.worldcreate

import net.japanesehunter.math.Length
import net.japanesehunter.math.meters

interface World {
  companion object {
    val MAX_SIZE: Length = 2_147_483_632.meters
  }
}
