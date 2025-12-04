@file:Suppress("INLINE_CLASS_IN_EXTERNAL_DECLARATION_WARNING")

package net.japanesehunter.webgpu.interop

fun GPUColorTargetState(
  format: GPUTextureFormat,
  blend: GPUBlendState? = null,
  writeMask: Int? = null,
): GPUColorTargetState =
  js("{}").unsafeCast<GPUColorTargetState>().apply {
    this.format = format
    if (blend != null) this.blend = blend
    if (writeMask != null) this.writeMask = writeMask
  }

external interface GPUColorTargetState {
  var format: GPUTextureFormat
  var blend: GPUBlendState?
  var writeMask: Int?
}
