package net.japanesehunter.webgpu.interop

fun GPUColorTargetState(
  format: String,
  blend: GPUBlendState? = null,
  writeMask: Int? = null,
): GPUColorTargetState =
  {}.unsafeCast<GPUColorTargetState>().apply {
    this.format = format
    if (blend != null) this.blend = blend
    if (writeMask != null) this.writeMask = writeMask
  }

external interface GPUColorTargetState {
  var format: String
  var blend: GPUBlendState?
  var writeMask: Int?
}
