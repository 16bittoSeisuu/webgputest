@file:Suppress("INLINE_CLASS_IN_EXTERNAL_DECLARATION_WARNING")

package net.japanesehunter.webgpu.interop

external class GPURenderPassDepthStencilAttachment(
  val view: GPUTextureView,
  val depthLoadOp: GPULoadOp,
  val depthStoreOp: GPUStoreOp,
  val depthReadOnly: Boolean = definedExternally,
  val depthClearValue: Double = definedExternally,
  val stencilLoadOp: GPULoadOp = definedExternally,
  val stencilStoreOp: GPUStoreOp = definedExternally,
  val stencilReadOnly: Boolean = definedExternally,
  val stencilClearValue: Double = definedExternally,
)
