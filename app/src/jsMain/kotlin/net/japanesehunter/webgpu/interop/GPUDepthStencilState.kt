@file:Suppress("INLINE_CLASS_IN_EXTERNAL_DECLARATION_WARNING")

package net.japanesehunter.webgpu.interop

fun GPUDepthStencilState(
  format: GPUTextureFormat,
  depthWriteEnabled: Boolean? = null,
  depthCompare: GPUCompareFunction? = null,
  stencilFront: GPUDepthStencilStateFace? = null,
  stencilBack: GPUDepthStencilStateFace? = null,
  stencilReadMask: Int? = null,
  stencilWriteMask: Int? = null,
  depthBias: Int? = null,
  depthBiasSlopeScale: Double? = null,
  depthBiasClamp: Double? = null,
): GPUDepthStencilState =
  js("{}").unsafeCast<GPUDepthStencilState>().apply {
    this.format = format
    if (depthWriteEnabled != null) this.depthWriteEnabled = depthWriteEnabled
    if (depthCompare != null) this.depthCompare = depthCompare
    if (stencilFront != null) this.stencilFront = stencilFront
    if (stencilBack != null) this.stencilBack = stencilBack
    if (stencilReadMask != null) this.stencilReadMask = stencilReadMask
    if (stencilWriteMask != null) this.stencilWriteMask = stencilWriteMask
    if (depthBias != null) this.depthBias = depthBias
    if (depthBiasSlopeScale !=
      null
    ) {
      this.depthBiasSlopeScale = depthBiasSlopeScale
    }
    if (depthBiasClamp != null) this.depthBiasClamp = depthBiasClamp
  }

external interface GPUDepthStencilState {
  var format: GPUTextureFormat
  var depthWriteEnabled: Boolean
  var depthCompare: GPUCompareFunction
  var stencilFront: GPUDepthStencilStateFace
  var stencilBack: GPUDepthStencilStateFace
  var stencilReadMask: Int
  var stencilWriteMask: Int
  var depthBias: Int
  var depthBiasSlopeScale: Double
  var depthBiasClamp: Double
}
