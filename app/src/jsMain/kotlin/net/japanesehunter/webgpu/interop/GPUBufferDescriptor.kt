@file:Suppress("INLINE_CLASS_IN_EXTERNAL_DECLARATION_WARNING")

package net.japanesehunter.webgpu.interop

fun GPUBufferDescriptor(
  size: Int,
  usage: GPUBufferUsage,
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
  var usage: GPUBufferUsage
  var mappedAtCreation: Boolean?
  var label: String?
}
