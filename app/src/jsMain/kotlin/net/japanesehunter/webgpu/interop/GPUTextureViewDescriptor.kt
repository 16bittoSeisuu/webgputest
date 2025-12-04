@file:Suppress("INLINE_CLASS_IN_EXTERNAL_DECLARATION_WARNING")

package net.japanesehunter.webgpu.interop

fun GPUTextureViewDescriptor(
  format: GPUTextureFormat? = null,
  dimension: GPUTextureViewDimension? = null,
  aspect: GPUTextureAspect? = null,
  baseMipLevel: Int? = null,
  mipLevelCount: Int? = null,
  baseArrayLayer: Int? = null,
  arrayLayerCount: Int? = null,
  label: String? = null,
): GPUTextureViewDescriptor =
  {}.unsafeCast<GPUTextureViewDescriptor>().apply {
    if (format != null) this.format = format
    if (dimension != null) this.dimension = dimension
    if (aspect != null) this.aspect = aspect
    if (baseMipLevel != null) this.baseMipLevel = baseMipLevel
    if (mipLevelCount != null) this.mipLevelCount = mipLevelCount
    if (baseArrayLayer != null) this.baseArrayLayer = baseArrayLayer
    if (arrayLayerCount != null) this.arrayLayerCount = arrayLayerCount
    if (label != null) this.label = label
  }

external interface GPUTextureViewDescriptor {
  var format: GPUTextureFormat?
  var dimension: GPUTextureViewDimension?
  var aspect: GPUTextureAspect?
  var baseMipLevel: Int?
  var mipLevelCount: Int?
  var baseArrayLayer: Int?
  var arrayLayerCount: Int?
  var label: String?
}
