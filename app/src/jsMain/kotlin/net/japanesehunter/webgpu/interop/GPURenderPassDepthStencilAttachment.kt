package net.japanesehunter.webgpu.interop

external class GPURenderPassDepthStencilAttachment(
  val view: GPUTextureView,
  val depthLoadOp: String,
  val depthStoreOp: String,
  val depthReadOnly: Boolean = definedExternally,
  val depthClearValue: Double = definedExternally,
  val stencilLoadOp: String = definedExternally,
  val stencilStoreOp: String = definedExternally,
  val stencilReadOnly: Boolean = definedExternally,
  val stencilClearValue: Double = definedExternally,
)
