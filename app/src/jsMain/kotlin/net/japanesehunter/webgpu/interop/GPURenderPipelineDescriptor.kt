package net.japanesehunter.webgpu.interop

fun GPURenderPipelineDescriptor(
  layout: GPUPipelineLayout = GPUPipelineLayout.auto,
  vertex: GPUVertexState,
  fragment: GPUFragmentState? = null,
  primitive: GPUPrimitiveState? = null,
  depthStencil: GPUDepthStencilState? = null,
  multisample: GPUMultisampleState? = null,
  label: String? = null,
): GPURenderPipelineDescriptor =
  js("{}").unsafeCast<GPURenderPipelineDescriptor>().apply {
    this.layout = layout
    this.vertex = vertex
    if (fragment != null) this.fragment = fragment
    if (primitive != null) this.primitive = primitive
    if (depthStencil != null) this.depthStencil = depthStencil
    if (multisample != null) this.multisample = multisample
    if (label != null) this.label = label
  }

external interface GPURenderPipelineDescriptor {
  var layout: GPUPipelineLayout
  var vertex: GPUVertexState
  var fragment: GPUFragmentState?
  var primitive: GPUPrimitiveState?
  var depthStencil: GPUDepthStencilState?
  var multisample: GPUMultisampleState?
  var label: String?
}
