package net.japanesehunter.webgpu.interop

fun GPUBindGroupDescriptor(
  layout: GPUBindGroupLayout,
  entries: Array<GPUBindGroupEntry>,
  label: String? = null,
): GPUBindGroupDescriptor =
  js("{}").unsafeCast<GPUBindGroupDescriptor>().apply {
    this.layout = layout
    this.entries = entries
    if (label != null) this.label = label
  }

external interface GPUBindGroupDescriptor {
  var layout: GPUBindGroupLayout
  var entries: Array<GPUBindGroupEntry>
  var label: String?
}
