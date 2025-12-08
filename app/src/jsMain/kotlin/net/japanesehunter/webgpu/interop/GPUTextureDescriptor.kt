@file:Suppress("INLINE_CLASS_IN_EXTERNAL_DECLARATION_WARNING")

package net.japanesehunter.webgpu.interop

fun GPUTextureDescriptor(
  size: GPUExtent3D,
  mipLevelCount: Int? = null,
  sampleCount: Int? = null,
  dimension: GPUTextureDimension? = null,
  format: GPUTextureFormat? = null,
  usage: GPUTextureUsage? = null,
  viewFormats: Array<GPUTextureFormat>? = null,
  label: String? = null,
): GPUTextureDescriptor =
  js("{}").unsafeCast<GPUTextureDescriptor>().apply {
    this.size = size
    if (mipLevelCount != null) this.mipLevelCount = mipLevelCount
    if (sampleCount != null) this.sampleCount = sampleCount
    if (dimension != null) this.dimension = dimension
    if (format != null) this.format = format
    if (usage != null) this.usage = usage
    if (viewFormats != null) this.viewFormats = viewFormats
    if (label != null) this.label = label
  }

external interface GPUTextureDescriptor {
  var size: GPUExtent3D
  var mipLevelCount: Int?
  var sampleCount: Int?
  var dimension: GPUTextureDimension?
  var format: GPUTextureFormat?
  var usage: GPUTextureUsage?
  var viewFormats: Array<GPUTextureFormat>?
  var label: String?
}
