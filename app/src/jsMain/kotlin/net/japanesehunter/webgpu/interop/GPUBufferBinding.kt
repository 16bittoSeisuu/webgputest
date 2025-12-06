package net.japanesehunter.webgpu.interop

fun GPUBufferBinding(
  buffer: GPUBuffer,
  offset: Int? = null,
  size: Int? = null,
): GPUBufferBinding =
  js("{}").unsafeCast<GPUBufferBinding>().apply {
    this.buffer = buffer
    this.offset = offset
    this.size = size
  }

external interface GPUBufferBinding : GPUBindingResource {
  var buffer: GPUBuffer
  var offset: Int?
  var size: Int?
}
