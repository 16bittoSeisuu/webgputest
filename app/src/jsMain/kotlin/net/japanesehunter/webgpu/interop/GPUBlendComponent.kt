package net.japanesehunter.webgpu.interop

fun GPUBlendComponent(
  operation: String? = null,
  srcFactor: String? = null,
  dstFactor: String? = null,
): GPUBlendComponent =
  {}.unsafeCast<GPUBlendComponent>().apply {
    if (operation != null) this.operation = operation
    if (srcFactor != null) this.srcFactor = srcFactor
    if (dstFactor != null) this.dstFactor = dstFactor
  }

external interface GPUBlendComponent {
  var operation: String?
  var srcFactor: String?
  var dstFactor: String?
}
