package net.japanesehunter.webgpu.interop

fun GPUMultisampleState(
  count: Int? = null,
  mask: Int? = null,
  alphaToCoverageEnabled: Boolean? = null,
): GPUMultisampleState =
  {}.unsafeCast<GPUMultisampleState>().apply {
    if (count != null) this.count = count
    if (mask != null) this.mask = mask
    if (alphaToCoverageEnabled != null) this.alphaToCoverageEnabled = alphaToCoverageEnabled
  }

external interface GPUMultisampleState {
  var count: Int
  var mask: Int
  var alphaToCoverageEnabled: Boolean
}
