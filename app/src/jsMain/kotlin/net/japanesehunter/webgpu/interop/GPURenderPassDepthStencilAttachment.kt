@file:Suppress("INLINE_CLASS_IN_EXTERNAL_DECLARATION_WARNING")

package net.japanesehunter.webgpu.interop

fun GPURenderPassDepthStencilAttachment(
  view: GPUTextureView,
  depthLoadOp: GPULoadOp,
  depthStoreOp: GPUStoreOp,
  depthReadOnly: Boolean? = null,
  depthClearValue: Double? = null,
  stencilLoadOp: GPULoadOp? = null,
  stencilStoreOp: GPUStoreOp? = null,
  stencilReadOnly: Boolean? = null,
  stencilClearValue: Double? = null,
): GPURenderPassDepthStencilAttachment =
  js("{}").unsafeCast<GPURenderPassDepthStencilAttachment>().apply {
    this.view = view
    this.depthLoadOp = depthLoadOp
    this.depthStoreOp = depthStoreOp
    if (depthReadOnly != null) this.depthReadOnly = depthReadOnly
    if (depthClearValue != null) this.depthClearValue = depthClearValue
    if (stencilLoadOp != null) this.stencilLoadOp = stencilLoadOp
    if (stencilStoreOp != null) this.stencilStoreOp = stencilStoreOp
    if (stencilReadOnly != null) this.stencilReadOnly = stencilReadOnly
    if (stencilClearValue != null) this.stencilClearValue = stencilClearValue
  }

external interface GPURenderPassDepthStencilAttachment {
  var view: GPUTextureView
  var depthLoadOp: GPULoadOp
  var depthStoreOp: GPUStoreOp
  var depthReadOnly: Boolean
  var depthClearValue: Double
  var stencilLoadOp: GPULoadOp
  var stencilStoreOp: GPUStoreOp
  var stencilReadOnly: Boolean
  var stencilClearValue: Double
}
