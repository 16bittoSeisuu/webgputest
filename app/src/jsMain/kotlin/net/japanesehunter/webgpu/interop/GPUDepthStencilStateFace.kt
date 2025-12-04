@file:Suppress("INLINE_CLASS_IN_EXTERNAL_DECLARATION_WARNING")

package net.japanesehunter.webgpu.interop

external class GPUDepthStencilStateFace(
  val compare: GPUCompareFunction,
  val failOp: GPUStencilOperation,
  val depthFailOp: GPUStencilOperation,
  val passOp: GPUStencilOperation,
)
