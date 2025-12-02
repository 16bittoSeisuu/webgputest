package net.japanesehunter.webgpu.interop

fun GPUBlendState(
  color: GPUBlendComponent,
  alpha: GPUBlendComponent,
): GPUBlendState =
  {}.unsafeCast<GPUBlendState>().apply {
    this.color = color
    this.alpha = alpha
  }

external interface GPUBlendState {
  var color: GPUBlendComponent
  var alpha: GPUBlendComponent
}
