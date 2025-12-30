package net.japanesehunter.webgpu.interop

fun GPURenderPassDescriptor(
  colorAttachments: Array<GPURenderPassColorAttachment>,
  label: String? = null,
  depthStencilAttachment: GPURenderPassDepthStencilAttachment? = null,
  maxDrawCount: Int? = null,
  occlusionQuerySet: GPUQuerySet? = null,
  timestampWrite: Array<GPURenderPassTimestampWrite>? = null,
): GPURenderPassDescriptor =
  js("{}").unsafeCast<GPURenderPassDescriptor>().apply {
    this.colorAttachments = colorAttachments
    if (label != null) this.label = label
    if (depthStencilAttachment !=
      null
    ) {
      this.depthStencilAttachment = depthStencilAttachment
    }
    if (maxDrawCount != null) this.maxDrawCount = maxDrawCount
    if (occlusionQuerySet != null) this.occlusionQuerySet = occlusionQuerySet
    if (timestampWrite != null) this.timestampWrite = timestampWrite
  }

external interface GPURenderPassDescriptor {
  var colorAttachments: Array<GPURenderPassColorAttachment>
  var label: String?
  var depthStencilAttachment: GPURenderPassDepthStencilAttachment?
  var maxDrawCount: Int?
  var occlusionQuerySet: GPUQuerySet?
  var timestampWrite: Array<GPURenderPassTimestampWrite>?
}
