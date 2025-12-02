package net.japanesehunter.webgpu.interop

external class GPUDepthStencilState(
  val format: String,
  val depthWriteEnabled: Boolean = definedExternally,
  val depthCompare: String = definedExternally,
  val stencilFront: GPUDepthStencilStateFace = definedExternally,
  val stencilBack: GPUDepthStencilStateFace = definedExternally,
  val stencilReadMask: Int = definedExternally,
  val stencilWriteMask: Int = definedExternally,
  val depthBias: Int = definedExternally,
  val depthBiasSlopeScale: Double = definedExternally,
  val depthBiasClamp: Double = definedExternally,
)
