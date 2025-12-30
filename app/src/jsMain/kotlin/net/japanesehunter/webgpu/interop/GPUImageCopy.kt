@file:Suppress("INLINE_CLASS_IN_EXTERNAL_DECLARATION_WARNING")

package net.japanesehunter.webgpu.interop

fun GPUImageCopyExternalImage(
  source: dynamic,
  origin: GPUOrigin3D? = null,
  flipY: Boolean? = null,
  premultipliedAlpha: Boolean? = null,
): GPUImageCopyExternalImage =
  js("{}").unsafeCast<GPUImageCopyExternalImage>().apply {
    this.source = source
    if (origin != null) this.origin = origin
    if (flipY != null) this.flipY = flipY
    if (premultipliedAlpha !=
      null
    ) {
      this.premultipliedAlpha = premultipliedAlpha
    }
  }

fun GPUImageCopyTextureTagged(
  texture: GPUTexture,
  mipLevel: Int? = null,
  origin: GPUOrigin3D? = null,
  aspect: GPUTextureAspect? = null,
  colorSpace: String? = null,
): GPUImageCopyTextureTagged =
  js("{}").unsafeCast<GPUImageCopyTextureTagged>().apply {
    this.texture = texture
    if (mipLevel != null) this.mipLevel = mipLevel
    if (origin != null) this.origin = origin
    if (aspect != null) this.aspect = aspect
    if (colorSpace != null) this.colorSpace = colorSpace
  }

external interface GPUImageCopyExternalImage {
  var source: dynamic
  var origin: GPUOrigin3D?
  var flipY: Boolean?
  var premultipliedAlpha: Boolean?
}

external interface GPUImageCopyTexture {
  var texture: GPUTexture
  var mipLevel: Int?
  var origin: GPUOrigin3D?
  var aspect: GPUTextureAspect?
}

external interface GPUImageCopyTextureTagged : GPUImageCopyTexture {
  var colorSpace: String?
}
