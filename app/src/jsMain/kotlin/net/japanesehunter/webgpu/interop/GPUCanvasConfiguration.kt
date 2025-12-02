package net.japanesehunter.webgpu.interop

fun GPUCanvasConfiguration(
  device: GPUDevice,
  format: String,
  usage: Int? = null,
  colorSpace: String? = null,
  alphaMode: String? = null,
  viewFormats: Array<String>? = null,
  toneMapping: GPUCanvasToneMappingMode? = null,
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
  var format: String
  var usage: Int?
  var colorSpace: String?
  var alphaMode: String?
  var viewFormats: Array<String>?
  var toneMapping: GPUCanvasToneMappingMode?
}
