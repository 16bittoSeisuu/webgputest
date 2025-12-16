package net.japanesehunter.worldcreate

fun interface QuadSink {
  fun put(
    quad: MaterialQuad,
    cullingReq: OpaqueFaceSink.() -> Unit,
  )

  fun interface OpaqueFaceSink {
    fun requireFace(face: BlockFace)
  }
}
