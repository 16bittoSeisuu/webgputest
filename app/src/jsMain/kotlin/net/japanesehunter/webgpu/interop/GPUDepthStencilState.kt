@file:Suppress("INLINE_CLASS_IN_EXTERNAL_DECLARATION_WARNING")

package net.japanesehunter.webgpu.interop

external class GPUDepthStencilState(
  val format: GPUTextureFormat,
  val depthWriteEnabled: Boolean = definedExternally,
  val depthCompare: GPUCompareFunction = definedExternally,
  val stencilFront: GPUDepthStencilStateFace = definedExternally,
  val stencilBack: GPUDepthStencilStateFace = definedExternally,
  val stencilReadMask: Int = definedExternally,
  val stencilWriteMask: Int = definedExternally,
  val depthBias: Int = definedExternally,
  val depthBiasSlopeScale: Double = definedExternally,
  val depthBiasClamp: Double = definedExternally,
)
