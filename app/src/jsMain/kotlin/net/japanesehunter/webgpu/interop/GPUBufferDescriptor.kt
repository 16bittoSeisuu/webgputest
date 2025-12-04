package net.japanesehunter.webgpu.interop

fun GPUBufferDescriptor(
  size: Int,
  usage: Int,
  mappedAtCreation: Boolean? = null,
  label: String? = null,
): GPUBufferDescriptor =
  {}.unsafeCast<GPUBufferDescriptor>().apply {
    this.size = size
    this.usage = usage
    if (mappedAtCreation != null) this.mappedAtCreation = mappedAtCreation
    if (label != null) this.label = label
  }

external interface GPUBufferDescriptor {
  var size: Int
  var usage: Int
  var mappedAtCreation: Boolean?
  var label: String?
}
