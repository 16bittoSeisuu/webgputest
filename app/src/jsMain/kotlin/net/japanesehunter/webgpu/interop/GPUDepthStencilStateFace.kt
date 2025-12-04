@file:Suppress("INLINE_CLASS_IN_EXTERNAL_DECLARATION_WARNING")

package net.japanesehunter.webgpu.interop

fun GPUDepthStencilStateFace(
  compare: GPUCompareFunction,
  failOp: GPUStencilOperation,
  depthFailOp: GPUStencilOperation,
  passOp: GPUStencilOperation,
): GPUDepthStencilStateFace =
  {}.unsafeCast<GPUDepthStencilStateFace>().apply {
    this.compare = compare
    this.failOp = failOp
    this.depthFailOp = depthFailOp
    this.passOp = passOp
  }

external interface GPUDepthStencilStateFace {
  var compare: GPUCompareFunction
  var failOp: GPUStencilOperation
  var depthFailOp: GPUStencilOperation
  var passOp: GPUStencilOperation
}
