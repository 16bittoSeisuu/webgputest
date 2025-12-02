package net.japanesehunter.webgpu.interop

fun GPURenderPassColorAttachment(
  view: GPUTextureView,
  loadOp: String,
  storeOp: String,
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
  var loadOp: String
  var storeOp: String
  var depthSlice: Int?
  var resolveTarget: GPUTextureView?
  var clearValue: GPUColor?
}
