package net.japanesehunter.worldcreate

interface BlockState {
  val quads: List<Quad>

  fun isOpaque(face: BlockFace): Boolean
}

open class FullBlockState(
  top: Quad,
  north: Quad,
  east: Quad,
  south: Quad,
  west: Quad,
  bottom: Quad,
) : BlockState {
  constructor(face: Quad) : this(face, face, face, face, face, face)

  constructor(top: Quad, side: Quad, bottom: Quad) : this(
    top,
    side,
    side,
    side,
    side,
    bottom,
  )

  override val quads: List<Quad> = listOf(top, north, east, south, west, bottom)

  override fun isOpaque(face: BlockFace): Boolean = true
}
