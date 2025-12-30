package net.japanesehunter.webgpu.interop

/**
 * Optional label descriptor passed when finalizing a [GPURenderBundleEncoder].
 */
fun GPURenderBundleDescriptor(
  label: String? = null,
): GPURenderBundleDescriptor =
  js("{}").unsafeCast<GPURenderBundleDescriptor>().apply {
    if (label != null) this.label = label
  }

external interface GPURenderBundleDescriptor {
  /**
   * Optional developer-assigned label surfaced by browser tooling.
   */
  var label: String?
}
