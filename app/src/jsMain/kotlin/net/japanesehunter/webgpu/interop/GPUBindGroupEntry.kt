package net.japanesehunter.webgpu.interop

fun GPUBindGroupEntry(
  binding: Int,
  resource: GPUBindingResource,
): GPUBindGroupEntry =
  js("{}").unsafeCast<GPUBindGroupEntry>().apply {
    this.binding = binding
    this.resource = resource
  }

external interface GPUBindGroupEntry {
  var binding: Int
  var resource: GPUBindingResource
}
