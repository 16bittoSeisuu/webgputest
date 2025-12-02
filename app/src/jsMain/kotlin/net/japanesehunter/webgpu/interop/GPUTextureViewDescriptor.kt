package net.japanesehunter.webgpu.interop

fun GPUTextureViewDescriptor(
  format: String? = null,
  dimension: String? = null,
  aspect: String? = null,
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
  var format: String?
  var dimension: String?
  var aspect: String?
  var baseMipLevel: Int?
  var mipLevelCount: Int?
  var baseArrayLayer: Int?
  var arrayLayerCount: Int?
  var label: String?
}
