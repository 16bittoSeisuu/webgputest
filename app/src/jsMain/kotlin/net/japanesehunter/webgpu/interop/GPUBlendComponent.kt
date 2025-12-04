@file:Suppress("INLINE_CLASS_IN_EXTERNAL_DECLARATION_WARNING")

package net.japanesehunter.webgpu.interop

fun GPUBlendComponent(
  operation: GPUBlendOperation? = null,
  srcFactor: GPUBlendFactor? = null,
  dstFactor: GPUBlendFactor? = null,
): GPUBlendComponent =
  js("{}").unsafeCast<GPUBlendComponent>().apply {
    if (operation != null) this.operation = operation
    if (srcFactor != null) this.srcFactor = srcFactor
    if (dstFactor != null) this.dstFactor = dstFactor
  }

external interface GPUBlendComponent {
  var operation: GPUBlendOperation?
  var srcFactor: GPUBlendFactor?
  var dstFactor: GPUBlendFactor?
}
