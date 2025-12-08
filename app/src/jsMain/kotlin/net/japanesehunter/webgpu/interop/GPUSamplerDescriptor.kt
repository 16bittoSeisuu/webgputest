@file:Suppress("INLINE_CLASS_IN_EXTERNAL_DECLARATION_WARNING")

package net.japanesehunter.webgpu.interop

fun GPUSamplerDescriptor(
  addressModeU: GPUAddressMode? = null,
  addressModeV: GPUAddressMode? = null,
  addressModeW: GPUAddressMode? = null,
  magFilter: GPUFilterMode? = null,
  minFilter: GPUFilterMode? = null,
  mipmapFilter: GPUMipmapFilterMode? = null,
  compare: GPUCompareFunction? = null,
  lodMinClamp: Double? = null,
  lodMaxClamp: Double? = null,
  maxAnisotropy: Int? = null,
  borderColor: GPUSamplerBorderColor? = null,
  label: String? = null,
): GPUSamplerDescriptor =
  js("{}").unsafeCast<GPUSamplerDescriptor>().apply {
    if (addressModeU != null) this.addressModeU = addressModeU
    if (addressModeV != null) this.addressModeV = addressModeV
    if (addressModeW != null) this.addressModeW = addressModeW
    if (magFilter != null) this.magFilter = magFilter
    if (minFilter != null) this.minFilter = minFilter
    if (mipmapFilter != null) this.mipmapFilter = mipmapFilter
    if (compare != null) this.compare = compare
    if (lodMinClamp != null) this.lodMinClamp = lodMinClamp
    if (lodMaxClamp != null) this.lodMaxClamp = lodMaxClamp
    if (maxAnisotropy != null) this.maxAnisotropy = maxAnisotropy
    if (borderColor != null) this.borderColor = borderColor
    if (label != null) this.label = label
  }

external interface GPUSamplerDescriptor {
  var addressModeU: GPUAddressMode?
  var addressModeV: GPUAddressMode?
  var addressModeW: GPUAddressMode?
  var magFilter: GPUFilterMode?
  var minFilter: GPUFilterMode?
  var mipmapFilter: GPUMipmapFilterMode?
  var compare: GPUCompareFunction?
  var lodMinClamp: Double?
  var lodMaxClamp: Double?
  var maxAnisotropy: Int?
  var borderColor: GPUSamplerBorderColor?
  var label: String?
}
