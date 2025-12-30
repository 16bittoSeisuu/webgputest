package net.japanesehunter.worldcreate.world

fun interface QuadSink {
  fun put(
    quad: MaterialQuad,
    cullingReq: OpaqueFaceSink.() -> Unit,
  )

  fun interface OpaqueFaceSink {
    fun requireFace(
      face: BlockFace,
    )
  }
}
