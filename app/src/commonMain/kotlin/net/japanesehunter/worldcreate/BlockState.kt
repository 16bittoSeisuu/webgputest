package net.japanesehunter.worldcreate

interface BlockState {
  val quads: List<QuadShape>

  fun isOpaque(face: BlockFace): Boolean
}

open class FullBlockState(
  top: QuadShape,
  north: QuadShape,
  east: QuadShape,
  south: QuadShape,
  west: QuadShape,
  bottom: QuadShape,
) : BlockState {
  constructor(face: QuadShape) : this(face, face, face, face, face, face)

  constructor(top: QuadShape, side: QuadShape, bottom: QuadShape) : this(
    top,
    side,
    side,
    side,
    side,
    bottom,
  )

  override val quads: List<QuadShape> = listOf(top, north, east, south, west, bottom)

  override fun isOpaque(face: BlockFace): Boolean = true
}
