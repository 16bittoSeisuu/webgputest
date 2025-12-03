@file:Suppress("INLINE_CLASS_IN_EXTERNAL_DECLARATION_WARNING")

package net.japanesehunter.webgpu.interop

fun GPURenderPassColorAttachment(
  view: GPUTextureView,
  loadOp: GPULoadOp,
  storeOp: GPUStoreOp,
  depthSlice: Int? = null,
  resolveTarget: GPUTextureView? = null,
  clearValue: GPUColor? = null,
): GPURenderPassColorAttachment =
  {}.unsafeCast<GPURenderPassColorAttachment>().apply {
    this.view = view
    this.loadOp = loadOp
    this.storeOp = storeOp
    if (depthSlice != null) this.depthSlice = depthSlice
    if (resolveTarget != null) this.resolveTarget = resolveTarget
    if (clearValue != null) this.clearValue = clearValue
  }

external interface GPURenderPassColorAttachment {
  var view: GPUTextureView
  var loadOp: GPULoadOp
  var storeOp: GPUStoreOp
  var depthSlice: Int?
  var resolveTarget: GPUTextureView?
  var clearValue: GPUColor?
}
