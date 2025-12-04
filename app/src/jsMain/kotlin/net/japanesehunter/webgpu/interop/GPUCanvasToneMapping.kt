@file:Suppress("INLINE_CLASS_IN_EXTERNAL_DECLARATION_WARNING")

package net.japanesehunter.webgpu.interop

fun GPUCanvasToneMapping(
  mode: GPUCanvasToneMappingMode? = null,
  exposure: Double? = null,
): GPUCanvasToneMapping =
  js("{}").unsafeCast<GPUCanvasToneMapping>().apply {
    if (mode != null) this.mode = mode
    if (exposure != null) this.exposure = exposure
  }

external interface GPUCanvasToneMapping {
  var mode: GPUCanvasToneMappingMode?
  var exposure: Double?
}
