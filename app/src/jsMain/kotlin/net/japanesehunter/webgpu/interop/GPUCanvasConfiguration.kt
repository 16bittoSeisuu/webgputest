@file:Suppress("INLINE_CLASS_IN_EXTERNAL_DECLARATION_WARNING")

package net.japanesehunter.webgpu.interop

fun GPUCanvasConfiguration(
  device: GPUDevice,
  format: GPUTextureFormat,
  usage: Int? = null,
  colorSpace: PredefinedColorSpace? = null,
  alphaMode: GPUCanvasAlphaMode? = null,
  viewFormats: Array<GPUTextureFormat>? = null,
  toneMapping: GPUCanvasToneMapping? = null,
): GPUCanvasConfiguration {
  val obj = {}.unsafeCast<GPUCanvasConfiguration>()
  obj.device = device
  obj.format = format
  if (usage != null) obj.usage = usage
  if (colorSpace != null) obj.colorSpace = colorSpace
  if (alphaMode != null) obj.alphaMode = alphaMode
  if (viewFormats != null) obj.viewFormats = viewFormats
  if (toneMapping != null) obj.toneMapping = toneMapping

  return obj
}

external interface GPUCanvasConfiguration {
  var device: GPUDevice
  var format: GPUTextureFormat
  var usage: Int?
  var colorSpace: PredefinedColorSpace?
  var alphaMode: GPUCanvasAlphaMode?
  var viewFormats: Array<GPUTextureFormat>?
  var toneMapping: GPUCanvasToneMapping?
}
